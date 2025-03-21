package it.unitn.ds1.node;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;
import akka.japi.Creator;
import it.unitn.ds1.SystemConstants;
import it.unitn.ds1.messages.*;
import it.unitn.ds1.messages.client.*;
import it.unitn.ds1.storage.FileStorageManager;
import it.unitn.ds1.storage.StorageManager;
import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Akka Actor that implements the node's behaviour.
 */
public class NodeActor extends UntypedActor {

	// Unique identifier for this node
	private final int id;

	// Storage Manager that helps read and write records into persistent storage.
	private final StorageManager storageManager;

	// Command used to launch the node.
	// This is used for a convenient initialization of the actor.
	private final StartupCommand startupCommand;

	// Akka remote path to contact another node.
	// This is used to make the node leave an existing system.
	private final String remote;

	// Logger, used for debug proposes.
	private final DiagnosticLoggingAdapter logger;

	// Internal variable used to keep track of the other nodes in the system.
	// NB: this map contains also myself!
	private final Map<Integer, ActorRef> nodes;

	// Keep the data store in memory for higher efficiency.
	// This cache will use a write-through strategy for simplicity and reliability.
	private final Map<Integer, VersionedItem> cache;

	// Read requests the node is responsible for
	// Maps the requestID to the request status
	private final Map<Integer, ReadRequestStatus> readRequests;

	// Write progress for a future write request the node is responsible for
	// Maps the requestID to the request status
	private final Map<Integer, WriteRequestStatus> writeRequests;

	// Unique incremental identifier for each client request
	// The counter is to be considered unique only inside the same node
	private int requestCount;

	// Internal variable used to store the current state of the node.
	private State state;

	/**
	 * Create a new node Actor.
	 *
	 * @param id             Unique identifier to assign to this node.
	 * @param startupCommand Initial state of the node. This determines the behaviour of the node when started.
	 * @param remote         Remote address of another actor to contact to leave the system.
	 *                       This parameter is not required for the bootstrap node.
	 */
	@SuppressWarnings("ConstantConditions")
	private NodeActor(int id, @NotNull StartupCommand startupCommand, @Nullable String remote) throws IOException {

		// at start, check that the constants R, W and N are correct
		assert SystemConstants.READ_QUORUM > 0 : "Read Quorum must be positive";
		assert SystemConstants.WRITE_QUORUM > 0 : "Write Quorum must be positive";
		assert SystemConstants.REPLICATION > 0 : "Replication factor must be positive";
		assert SystemConstants.READ_QUORUM + SystemConstants.WRITE_QUORUM > SystemConstants.REPLICATION :
			"Condition R + W > N must hold to guarantee consistency in the system";

		// initialize values
		this.id = id;
		this.startupCommand = startupCommand;
		this.remote = remote;

		// initialize storage manager
		this.storageManager = FileStorageManager.getInstance(id);

		// add myself to the map of nodes
		this.nodes = new HashMap<>();
		this.nodes.put(id, getSelf());

		// create empty cache
		this.cache = new HashMap<>();

		// initialize other variables
		this.readRequests = new HashMap<>();
		this.writeRequests = new HashMap<>();
		this.requestCount = 0;

		// setup logger context
		this.logger = Logging.getLogger(this);
		final Map<String, Object> mdc = new HashMap<String, Object>() {{
			put("actor", "Node [" + id + "]:");
		}};
		logger.setMDC(mdc);
		logger.info("Initialize node with initial state {}", startupCommand);
	}

	/**
	 * Create Props for a node that should bootstrap the system.
	 * See: http://doc.akka.io/docs/akka/current/java/untyped-actors.html#Recommended_Practices
	 */
	public static Props bootstrap(final int id) {
		return Props.create(new Creator<NodeActor>() {
			private static final long serialVersionUID = 1L;

			@Override
			public NodeActor create() throws Exception {
				return new NodeActor(id, StartupCommand.BOOTSTRAP, null);
			}
		});
	}

	/**
	 * Create Props for a new node that is willing to join the system.
	 * See: http://doc.akka.io/docs/akka/current/java/untyped-actors.html#Recommended_Practices
	 */
	public static Props join(final int id, String remote) {
		return Props.create(new Creator<NodeActor>() {
			private static final long serialVersionUID = 1L;

			@Override
			public NodeActor create() throws Exception {
				return new NodeActor(id, StartupCommand.JOIN, remote);
			}
		});
	}

	/**
	 * Create Props for a new node that is willing to join back the system after a crash.
	 * See: http://doc.akka.io/docs/akka/current/java/untyped-actors.html#Recommended_Practices
	 */
	public static Props recover(final int id, String remote) {
		return Props.create(new Creator<NodeActor>() {
			private static final long serialVersionUID = 1L;

			@Override
			public NodeActor create() throws Exception {
				return new NodeActor(id, StartupCommand.RECOVER, remote);
			}
		});
	}

	/**
	 * Return the ID of the next node in the ring.
	 *
	 * @param ids  Set of all IDs in the system.
	 * @param myID My ID.
	 * @return The ID of the next node in the ring.
	 */
	@NotNull
	static Integer nextInTheRing(@NotNull Set<Integer> ids, int myID) {
		return ids.stream()
			.filter(key -> key > myID)
			.findFirst()
			.orElse(Collections.min(ids));
	}

	/**
	 * Return the IDs responsible for the given key.
	 *
	 * @param ids All IDs.
	 * @param key Key.
	 * @param n   Replication factor.
	 * @return Set of responsible IDs.
	 */
	static Set<Integer> responsibleForKey(@NotNull Set<Integer> ids, int key, int n) {
		return ids.stream().sorted((o1, o2) -> {
			if (o1 >= key && o2 >= key) return o1 - o2;
			if (o1 >= key && o2 < key) return -1;
			if (o1 < key && o2 >= key) return +1;
			else return o1 - o2;
		}).limit(n).collect(Collectors.toSet());
	}

	/**
	 * This method is called after the constructor, when the actor is ready.
	 * We use this to do the initial actions required by the actor, depending
	 * on the initial state. For instance, if the node needs to leave the
	 * system, we send a message to a remote node already in the system.
	 */
	@Override
	public void preStart() throws IOException {
		switch (startupCommand) {

			// this is the first node - just initialize the storage
			case BOOTSTRAP: {

				// initialize storage
				storageManager.clearStorage();

				// ready
				this.state = State.READY;
				logger.debug("PreStart(): storage initialized, move to {}", state);
				break;
			}

			// asks to the node provided from the command line to join the system
			case JOIN: {

				// initialize storage
				storageManager.clearStorage();

				// ask the list of nodes
				getContext().actorSelection(remote).tell(new JoinRequestMessage(id), getSelf());
				this.state = State.JOINING_WAITING_NODES;
				logger.debug("PreStart(): storage initialized, ask to join to [{}], move to {}", remote, state);
				break;
			}

			// asks to the node provided from the command line the nodes in the system, needed for the recovery
			case RECOVER: {

				// ask nodes, in order to complete the recovery
				getContext().actorSelection(remote).tell(new JoinRequestMessage(id), getSelf());
				this.state = State.RECOVERING_WAITING_NODES;
				logger.debug("PreStart(): ask nodes to [{}], move to {}", remote, state);
				break;
			}
		}

		// the method must initialize the state
		assert this.state != null;
	}

	/**
	 * For each type of message, call the relative callback
	 * to keep this method short and clean.
	 *
	 * @param message Incoming message.
	 */
	@Override
	public void onReceive(Object message) {
		if (message instanceof JoinRequestMessage) {
			onJoinRequest((JoinRequestMessage) message);
		} else if (message instanceof DataRequestMessage) {
			onDataRequest((DataRequestMessage) message);
		} else if (message instanceof NodesListMessage) {
			onNodesList((NodesListMessage) message);
		} else if (message instanceof ClientLeaveRequest) {
			onLeaveRequest();
		} else if (message instanceof ClientReadRequest) {
			onClientReadRequest((ClientReadRequest) message);
		} else if (message instanceof ClientUpdateRequest) {
			onClientUpdateRequest((ClientUpdateRequest) message);
		} else if (message instanceof ReadRequest) {
			onReadRequest((ReadRequest) message);
		} else if (message instanceof WriteRequest) {
			onWriteRequest((WriteRequest) message);
		} else if (message instanceof ReadResponse) {
			onReadResponse((ReadResponse) message);
		} else if (message instanceof DataMessage) {
			onData((DataMessage) message);
		} else if (message instanceof JoinMessage) {
			onJoin((JoinMessage) message);
		} else if (message instanceof ReJoinMessage) {
			onReJoin((ReJoinMessage) message);
		} else if (message instanceof LeaveMessage) {
			onLeave((LeaveMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void onJoinRequest(@NotNull JoinRequestMessage message) {

		// I already have the nodes
		if (state != State.JOINING_WAITING_NODES && state != State.RECOVERING_WAITING_NODES) {
			logger.debug("Node [{}] asks to join the network... sending my nodes: {}", message.getSenderID(), nodes.keySet());

			// send back the list of nodes
			reply(new NodesListMessage(id, nodes));
		}

		// if I am not ready, ignore the message
		else {
			logger.warning("Node [{}] asks to join the network, but I am NOT ready to reply ({}); ignore the request",
				message.getSenderID(), state);
		}
	}

	private void onDataRequest(@NotNull DataRequestMessage message) {

		// I am ready to reply
		if (state == State.READY) {

			// extract the data
			final Map<Integer, VersionedItem> records = storageManager.readRecords();
			logger.debug("Node [{}] asks my data. Sending keys: {}", message.getSenderID(), records.keySet());

			// send back the data
			reply(new DataMessage(id, records));
		}

		// I am waiting for the data, cannot reply
		else {
			logger.warning("Node [{}] asks my data, but I am not ready ({}); ignore the request",
				message.getSenderID(), state);
		}
	}

	private void onLeaveRequest() {
		logger.warning("[LEAVE] A Client asks me to leave... sending goodbye message");

		// TODO: do stuff, exit protocol

		// inform all nodes that I am leaving
		multicast(new LeaveMessage(id));

		// eventually, acknowledge the client
		reply(new ClientLeaveResponse(id));

		// TODO: cancel the storage?
		// storageManager.deleteStorage();

		// shutdown
		getContext().system().terminate();
	}

	private void onNodesList(@NotNull NodesListMessage message) {
		assert state == State.JOINING_WAITING_NODES || state == State.RECOVERING_WAITING_NODES;
		logger.debug("Node [{}] sends the list of nodes: {}", message.getSenderID(), message.getNodes().keySet());

		// update my list of nodes
		this.nodes.putAll(message.getNodes());

		switch (state) {

			// I trying to join
			case JOINING_WAITING_NODES: {

				// compute the next node in the ring
				final int next = nextInTheRing(nodes.keySet(), id);
				final ActorRef nextNode = nodes.get(next);

				// ask the data the node is responsible for
				nextNode.tell(new DataRequestMessage(id), getSelf());
				this.state = State.JOINING_WAITING_DATA;

				break;
			}

			// I was in the system, but I am recovering after a crash
			case RECOVERING_WAITING_NODES: {
				assert cache.isEmpty();

				// clean old keys
				this.dropOldKeys();

				// I probably got an old reference for myself too... correct it!
				nodes.put(id, getSelf());

				// I need to announce me to the system... because nodes have outdated Akka references
				// this is not really part of the protocol, it is just an Akka implementation detail
				multicast(new ReJoinMessage(id));

				// now I am ready
				this.state = State.READY;
				logger.info("[RECOVERY] Recovery completed, state = {}, nodes = {}", state, nodes.keySet());
				break;
			}
		}
	}

	private void onClientReadRequest(@NotNull ClientReadRequest message) {

		// extract the key to search
		final int key = message.getKey();

		// get the nodes responsible for that key
		if (SystemConstants.READ_QUORUM > nodes.size()) {
			logger.warning("[READ] A client requests key [{}]... but there are not enough nodes in the system (quorum={}, nodes={})",
				key, SystemConstants.READ_QUORUM, nodes.size());

			// TODO...

		} else {

			// store the read request to be able to process the responses
			requestCount++;
			readRequests.put(requestCount, new ReadRequestStatus(key, getSender(), SystemConstants.READ_QUORUM));

			// ask the responsible nodes for the key
			final Set<Integer> responsible = responsibleForKey(nodes.keySet(), key, SystemConstants.REPLICATION);
			responsible.forEach(node -> nodes.get(node).tell(new ReadRequest(id, requestCount, key), getSelf()));
			logger.info("[READ] A client requests key [{}]... asking nodes {} of {}", key, responsible, nodes.keySet());

			// TODO: set timeout to cleanup old requests
		}
	}

	private void onClientUpdateRequest(@NotNull ClientUpdateRequest message) {

		// extract the key to update
		final int key = message.getKey();

		// TODO: this should be NOT replication, but READ / WRITE max ???

		// get the nodes responsible for that key
		if (SystemConstants.REPLICATION > nodes.size()) {
			logger.warning("[UPDATE] A client requests to update key [{}]... but there are not enough nodes in the system: " +
				"(replication={}, nodes={})", key, SystemConstants.REPLICATION, nodes.size());

			// TODO...

		} else {

			// store the update request to be able to process the responses
			requestCount++;
			writeRequests.put(requestCount, new WriteRequestStatus(key, message.getValue(), getSender(),
				SystemConstants.READ_QUORUM, SystemConstants.WRITE_QUORUM));

			// before update key, ask the responsible nodes for the key
			final Set<Integer> responsible = responsibleForKey(nodes.keySet(), key, SystemConstants.REPLICATION);
			responsible.forEach(node -> nodes.get(node).tell(new ReadRequest(id, requestCount, key), getSelf()));

			logger.info("[UPDATE] A client requests to update key [{}]... asking nodes {} of {}", key, responsible, nodes.keySet());

			// TODO: set timeout to cleanup old requests
		}
	}

	private void onReadRequest(ReadRequest message) {

		// extract the key to search
		final int key = message.getKey();

		// TODO: assertion that I am responsible?

		// read the value from the data-store and send it back
		final VersionedItem item = read(key);
		reply(new ReadResponse(id, message.getRequestID(), key, item));

		// log
		final String value = item != null ? "\"" + item.getValue() + "\"" : "NOT FOUND";
		logger.debug("[READ] Read request from node [{}] for key [{}]: reply value {}",
			message.getSenderID(), message.getKey(), value);
	}

	private void onWriteRequest(WriteRequest message) {

		// extract the key to search
		final int key = message.getKey();

		// TODO: assertion that I am responsible?

		// write the new record in the data-store
		write(key, message.getVersionedItem());

		// log
		logger.info("[UPDATE]: Node [{}] sends update key [{}] -> \"{}\", version {}",
			message.getSenderID(), key, message.getVersionedItem().getValue(), message.getVersionedItem().getVersion());

		// TODO: answer to sender? Project guidelines don't talk about it
	}

	private void onReadResponse(ReadResponse message) {

		// TODO: assert something here?

		// check that the request is still valid
		final int requestID = message.getRequestID();
		final boolean valid = readRequests.containsKey(message.getRequestID()) || writeRequests.containsKey(message.getRequestID());

		// not valid -> ignore the message
		if (!valid) {
			logger.debug("[READ] Old vote for read request [{}] from node [{}]... ignore it", requestID, message.getSenderID());
		}

		// valid -> add it to the voting
		else {
			logger.debug("[READ] Valid vote for read request [{}] from node [{}]", requestID, message.getSenderID());
			final ReadRequestStatus readStatus = readRequests.get(requestID);

			// request is related to a ClientRead request
			if (readStatus != null) {
				readStatus.addVote(message.getValue());

				// check quorum
				if (readStatus.isQuorumReached()) {
					final String value = readStatus.getLatestValue() != null ? "\"" + readStatus.getLatestValue() + "\"" : "NOT FOUND";
					logger.info("[READ] Quorum reached for read request [{}] - result is " + "{}", requestID, value);
					readStatus.getSender().tell(new ClientReadResponse(id, readStatus.getKey(), readStatus.getLatestValue()), getSelf());

					// cleanup memory
					this.readRequests.remove(requestID);

				} else {
					logger.debug("[READ] Quorum NOT reached yet for read request [{}] - waiting...", requestID);
				}
			}

			// request is related to a ClientUpdate request
			else {
				final WriteRequestStatus writeStatus = writeRequests.get(requestID);
				writeStatus.addVote(message.getValue());

				// check quorum
				if (writeStatus.isQuorumReached()) {
					logger.info("[UPDATE] Quorum reached for write request [{}] - updated record to \"{}\"", requestID, writeStatus.getUpdatedRecord());

					// calculate new version
					final VersionedItem updatedRecord = writeStatus.getUpdatedRecord();

					// send successful response to client with updated record
					// TODO check if this is the right way to send an object
					writeStatus.getSender().tell(new ClientUpdateResponse(id, writeStatus.getKey(), updatedRecord), getSelf());

					// send write request to interested nodes (nodes responsible for that key)
					final Set<Integer> responsible = responsibleForKey(nodes.keySet(), writeStatus.getKey(), SystemConstants.REPLICATION);
					// TODO check if this is the right way to send an object
					responsible.forEach(node -> nodes.get(node).tell(new WriteRequest(id, requestCount, writeStatus.getKey(), updatedRecord), getSelf()));

					// cleanup memory
					this.writeRequests.remove(requestID);

				} else {
					logger.debug("[UPDATE] Quorum NOT reached yet for write request [{}] - waiting...", requestID);
				}
			}
		}
	}

	private void onData(@NotNull DataMessage message) {
		assert this.state == State.JOINING_WAITING_DATA;
		logger.debug("Node [{}] sends the data it is responsible for: {}", message.getSenderID(), message.getRecords().keySet());

		// TODO: store data --> all data?
		storageManager.appendRecords(message.getRecords());
		cache.putAll(message.getRecords());

		// announce everybody that I am part of the system
		multicast(new JoinMessage(id));

		// now I am ready to serve requests
		this.state = State.READY;
		logger.info("Sending Join msg... now I am part of the system. My nodes are: {}", nodes.keySet());
	}

	private void onJoin(JoinMessage message) {

		// add the node to my list
		this.nodes.put(message.getSenderID(), getSender());
		logger.info("Node [{}] is joining... nodes = {}", message.getSenderID(), nodes.keySet());

		// clean keys
		this.dropOldKeys();
	}

	private void onReJoin(ReJoinMessage message) {

		// update the reference for the crashed node
		// this is needed because Akka gives to the node a different reference if started again after a crash
		this.nodes.put(message.getSenderID(), getSender());
		logger.warning("Node [{}] is re-joining after a crash... nodes = {}", message.getSenderID(), nodes.keySet());
	}

	private void onLeave(LeaveMessage message) {

		// remove it from my nodes
		this.nodes.remove(message.getSenderID());
		logger.info("Node [{}] is leaving... nodes = {}", message.getSenderID(), nodes.keySet());
	}

	/**
	 * Send the given message to all the other nodes.
	 *
	 * @param message Message to send in multicast.
	 */
	private void multicast(Serializable message) {
		this.nodes.entrySet()
			.stream()
			.filter(entry -> entry.getKey() != id)
			.forEach(entry -> entry.getValue().tell(message, getSelf()));
	}

	/**
	 * Reply to the actor that sent the last message.
	 *
	 * @param reply Message to sent back.
	 */
	private void reply(Serializable reply) {
		getSender().tell(reply, getSelf());
	}

	/**
	 * Extract the item with the requested key from the data-store.
	 * We use the in-memory cache for simplicity.
	 *
	 * @param key Key of the data item.
	 */
	@Nullable
	private VersionedItem read(int key) {
		return cache.get(key);
	}

	/**
	 * Write a new data item to the storage.
	 * Also, update the in-memory cache.
	 *
	 * @param key  Key of the data item.
	 * @param item Value and version of the data item.
	 */
	private void write(int key, VersionedItem item) {

		// store in the file
		storageManager.appendRecord(key, item);

		// write-though cache
		cache.put(key, item);
	}

	private void dropOldKeys() {

		// TODO: check if this works
		// seems like it is working

		// load records from storage
		final Map<Integer, VersionedItem> oldRecords = storageManager.readRecords();

		// remove unwanted records
		final Map<Integer, VersionedItem> records = oldRecords.entrySet().stream()
			.filter(entry -> responsibleForKey(nodes.keySet(), entry.getKey(), SystemConstants.REPLICATION).contains(id))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// log
		logger.debug("Cleaning storage... nodes = {}, old keys = {}, new keys = {}", nodes.keySet(), oldRecords.keySet(), records.keySet());

		// update storage
		storageManager.writeRecords(records);

		// update cache
		cache.clear();
		cache.putAll(records);
	}

	/**
	 * Enumeration of possible initial states for a node.
	 * This is used to execute the proper action in the #preStart() method.
	 */
	private enum StartupCommand {
		BOOTSTRAP,
		JOIN,
		RECOVER
	}

	/**
	 * Enumeration of all possible states the node is in.
	 * For example, the node is joining the network and is waiting
	 * for some reply to get operational.
	 */
	private enum State {
		JOINING_WAITING_NODES,
		JOINING_WAITING_DATA,
		RECOVERING_WAITING_NODES,
		READY
	}
}
