package it.unitn.ds1.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;
import akka.japi.Creator;
import it.unitn.ds1.SystemConstants;
import it.unitn.ds1.messages.*;
import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Akka actor that implements the node's behaviour.
 */
public class NodeActor extends UntypedActor {

	/**
	 * Unique identifier for this node.
	 */
	private final int id;

	/**
	 * Initial state of this node.
	 * This is used for a convenient initialization of the actor.
	 */
	private final InitialState initialState;

	/**
	 * Akka remote path to contact another node.
	 * This is used to make the node leave an existing system.
	 */
	private final String remote;

	/**
	 * Logger, used for debug proposes.
	 */
	private final DiagnosticLoggingAdapter logger;


	/**
	 * Internal variable used to keep track of the other nodes in the system.
	 * NB: this map contains also myself!
	 */
	private final Map<Integer, ActorRef> nodes;

	/**
	 * Keep the data store in memory for higher efficiency.
	 * This cache will use a write-through strategy for simplicity and reliability.
	 */
	private final Map<Integer, VersionedItem> cache;

	/**
	 * Internal variable used to store the current state of the node.
	 */
	private State state;


	// TODO: maybe initialState is not the best name

	/**
	 * Create a new node Actor.
	 *
	 * @param id           Unique identifier to assign to this node.
	 * @param initialState Initial state of the node. This determines the behaviour of the node when started.
	 * @param remote       Remote address of another actor to contact to leave the system.
	 *                     This parameter is not required for the bootstrap node.
	 */
	private NodeActor(int id, @NotNull InitialState initialState, @Nullable String remote) {
		this.id = id;
		this.initialState = initialState;
		this.remote = remote;

		// add myself to the map of nodes
		this.nodes = new HashMap<>();
		this.nodes.put(id, getSelf());

		// create empty cache
		this.cache = new HashMap<>();

		// TODO: maybe not needed
		// current state is STARTING... this will be changed in the preStart()
		this.state = State.STARTING;

		// setup logger context
		this.logger = Logging.getLogger(this);
		final Map<String, Object> mdc = new HashMap<String, Object>() {{
			put("actor", "Node [" + id + "]");
		}};
		logger.setMDC(mdc);

		// debug log
		logger.warning("Initialize node with initial state {}", initialState);
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
				return new NodeActor(id, InitialState.BOOTSTRAP, null);
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
				return new NodeActor(id, InitialState.JOIN, remote);
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
				return new NodeActor(id, InitialState.RECOVER, remote);
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
		assert n <= ids.size();
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
	public void preStart() {

		// depending on the initialization, decide what to do
		switch (initialState) {

			// nothing needed in this case
			case BOOTSTRAP:
				this.state = State.READY;
				logger.info("preStart(): do nothing, move to {}", state);
				break;

			// asks to the node provided from the command line to join the system
			case JOIN:
				this.state = State.JOINING_WAITING_NODES;
				logger.info("preStart(): move to {}, ask to join to {}", state, remote);
				getContext().actorSelection(remote).tell(new JoinRequestMessage(id), getSelf());
				break;

			// asks to the node provided from the command line the nodes in the system, needed for the recovery
			case RECOVER:
				this.state = State.RECOVERING_WAITING_NODES;
				logger.info("preStart(): move to {}, ask nodes to {}", state, remote);
				getContext().actorSelection(remote).tell(new JoinRequestMessage(id), getSelf());
				break;
		}
	}

	@Override
	public void onReceive(Object message) {
		if (message instanceof JoinRequestMessage) {
			onJoinRequest((JoinRequestMessage) message);
		} else if (message instanceof DataRequestMessage) {
			onDataRequest((DataRequestMessage) message);
		} else if (message instanceof NodesListMessage) {
			onNodesList((NodesListMessage) message);
		} else if (message instanceof LeaveRequestMessage) {
			onLeaveRequest();
		} else if (message instanceof ClientReadRequestMessage) {
			onClientReadRequest((ClientReadRequestMessage) message);
		} else if (message instanceof DataMessage) {
			onData((DataMessage) message);
		} else if (message instanceof JoinMessage) {
			onJoin((JoinMessage) message);
		} else if (message instanceof LeaveMessage) {
			onLeave((LeaveMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void onJoinRequest(@NotNull JoinRequestMessage message) {

		// TODO: check that I am ready to reply

		logger.info("Node [{}] asks to join the network", message.getId());

		// send back the list of nodes
		reply(new NodesListMessage(nodes));
	}

	@SuppressWarnings("UnusedParameters")
	private void onDataRequest(@NotNull DataRequestMessage message) {

		// TODO: check that I am ready to reply

		logger.info("Somebody asked me the data");

		// TODO: extract the data

		// send back the data
		reply(new DataMessage());
	}

	private void onLeaveRequest() {
		logger.info("Leave request");

		// TODO: do stuff, exit protocol

		// inform all nodes that I am leaving
		multicast(new LeaveMessage(id));

		// eventually, acknowledge the client
		reply(new LeaveAcknowledgmentMessage(id));

		// TODO: cancel the storage?

		// shutdown
		getContext().system().terminate();
	}

	private void onNodesList(@NotNull NodesListMessage message) {
		assert state == State.JOINING_WAITING_NODES || state == State.RECOVERING_WAITING_NODES;
		logger.info("Somebody sent the list of nodes: {}", message.getNodes());

		// update my list of nodes
		this.nodes.putAll(message.getNodes());

		switch (state) {
			case JOINING_WAITING_NODES: {

				// compute the next node in the ring
				final int next = nextInTheRing(nodes.keySet(), this.id);
				final ActorRef nextNode = nodes.get(next);

				// ask the data the node is responsible for
				nextNode.tell(new DataRequestMessage(), getSelf());
				this.state = State.JOINING_WAITING_DATA;

				break;
			}

			case RECOVERING_WAITING_NODES: {

				// TODO: load data items from file
				// remove the old ones

				// TODO: no need to announce me to the system... they should know me already

				// now I am ready
				this.state = State.READY;
				logger.info("Recovery completed, state = {}, nodes = {}", state, nodes.keySet());

				break;
			}
		}
	}

	private void onClientReadRequest(@NotNull ClientReadRequestMessage message) {

		// extract the key to search
		final int key = message.getKey();

		// get the nodes responsible for that key
		if (SystemConstants.READ_QUORUM > nodes.size()) {
			logger.warning("Client request for key {}... but there are not enough nodes in the system: quorum={}, nodes={}",
				key, SystemConstants.READ_QUORUM, nodes.size());

			// TODO...

		} else {
			final Set<Integer> responsible = responsibleForKey(nodes.keySet(), key, SystemConstants.REPLICATION);
			logger.info("Client request for key {}. Asking nodes {}", key, responsible);

			// ask everybody for the key
			// TODO...

			// TODO: fake, to remove
			reply(new ClientReadResultMessage(id, key, "TODO: this is just a fake value"));
		}
	}


	@SuppressWarnings("UnusedParameters")
	private void onData(@NotNull DataMessage message) {
		assert this.state == State.JOINING_WAITING_DATA;

		// TODO: store data

		logger.info("Somebody sent the me the data it is responsible for. Sending Join msg...");

		// announce everybody that I am part of the system
		multicast(new JoinMessage(id));

		// now I am ready to serve requests
		this.state = State.READY;

		logger.info("Now I am part of the system. My nodes are: {}", nodes.keySet());
	}

	private void onJoin(JoinMessage message) {

		// add the node to my list
		this.nodes.put(message.getId(), getSender());

		// log
		logger.info("Node [{}] is joining. Nodes = {}", message.getId(), nodes.keySet());

		// TODO: remove the keys I am not responsible for
	}

	private void onLeave(LeaveMessage message) {

		// remove it from my nodes
		this.nodes.remove(message.getId());

		// log
		logger.info("Node [{}] is leaving. Nodes = {}", message.getId(), nodes.keySet());
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

		// TODO: write to disk

		// write-though cache
		cache.put(key, item);
	}

	/**
	 * Enumeration of possible initial states for a node.
	 * This is used to execute the proper action in the #preStart() method.
	 */
	private enum InitialState {
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
		STARTING,
		JOINING_WAITING_NODES,
		JOINING_WAITING_DATA,
		RECOVERING_WAITING_NODES,
		READY
	}
}
