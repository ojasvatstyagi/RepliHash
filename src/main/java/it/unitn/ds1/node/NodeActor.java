package it.unitn.ds1.node;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;
import akka.japi.Creator;
import it.unitn.ds1.messages.client.*;
import it.unitn.ds1.messages.internal.*;
import it.unitn.ds1.node.status.ReadRequestStatus;
import it.unitn.ds1.node.status.UpdateRequestStatus;
import it.unitn.ds1.node.status.UpdateResponseStatus;
import it.unitn.ds1.storage.FileStorageManager;
import it.unitn.ds1.storage.StorageManager;
import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import it.unitn.ds1.node.HashUtil;

import java.util.TreeSet;
import java.util.stream.Collectors;

import static it.unitn.ds1.SystemConstants.QUORUM_TIMEOUT_SECONDS;


public final class NodeActor extends UntypedActor {

	// Unique identifier for this node
	private final int id;
	private final String rawId;

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
	private final Ring ring;

	// Keep the data store in memory for higher efficiency.
	// This cache will use a write-through strategy for simplicity and reliability.
	private final Map<Integer, VersionedItem> cache;

	// Read requests the node is responsible for
	// Maps the requestID to the request status
	private final Map<Integer, ReadRequestStatus> readRequests;

	// Write progress for a future write request the node is responsible for
	// Maps the requestID to the request status
	private final Map<Integer, UpdateRequestStatus> writeRequests;

	// Write responses from nodes to which a write has been asked
	// Maps the requestID to the response status
	private final Map<Integer, UpdateResponseStatus> writeResponses;

	// Timers for read or write requests
	// Every timer is responsible for delivering a timeout message to node is responsible for the request.
	// Maps the requestID to the timer
	private final Map<Integer, Cancellable> requestsTimers;

	// configuration
	private final int readQuorum;
	private final int writeQuorum;
	private final int replication;

	// used for tests: should the node terminate Akka on leave?
	private final boolean terminateSystemOnLeave;

	// Unique incremental identifier for each client request
	// The counter is to be considered unique only inside the same node
	private int requestCount;

	// Internal variable used to store the current state of the node.
	private State state;

	private NodeActor(int id, @NotNull String rawId, @NotNull String storagePath, @NotNull StartupCommand startupCommand, @Nullable String remote,
					  int readQuorum, int writeQuorum, int replication, boolean terminateSystemOnLeave) throws IOException {

		// at start, check that the constants R, W and N are correct
		assert readQuorum > 0 : "Read Quorum must be positive";
		assert writeQuorum > 0 : "Write Quorum must be positive";
		assert replication > 0 : "Replication factor must be positive";
		assert readQuorum + writeQuorum > replication : "Condition R + W > N must hold to guarantee consistency in the system";

		this.readQuorum = readQuorum;
		this.writeQuorum = writeQuorum;
		this.replication = replication;

		// initialize values
		this.id = id;
		this.rawId = rawId;
		this.startupCommand = startupCommand;

		this.logger = Logging.getLogger(this);
    	this.logger.info("Node starting: ID={} (mode={}) storage='{}'",
        id, startupCommand, storagePath);

		this.remote = remote;
		this.terminateSystemOnLeave = terminateSystemOnLeave;

		// initialize storage manager
		this.storageManager = new FileStorageManager(storagePath, id);

		// initialize the ring
		this.ring = new Ring(replication, id);
		this.ring.addNode(id, getSelf());
		this.logger.info("Initial ring membership: {}", new TreeSet<>(ring.getNodeIDs()));

		// create empty cache
		this.cache = new HashMap<>();

		// initialize other variables
		this.readRequests = new HashMap<>();
		this.writeRequests = new HashMap<>();
		this.writeResponses = new HashMap<>();
		this.requestsTimers = new HashMap<>();
		this.requestCount = 0;

		// setup logger context
        Map<String,Object> mdc = new HashMap<>();
		mdc.put("actor", "Node[" + rawId + "#" + id + "]");
        logger.setMDC(mdc);
		logger.info("Starting node: address='{}'  hashedID={}  storage='{}'  mode={}",
            rawId, id, storagePath, startupCommand);
	}

	public static Props bootstrap(final int hashedId,
                                  @NotNull final String rawId,
                                  @NotNull final String storagePath,
                                  int readQ, int writeQ, int rep,
                                  boolean terminateOnLeave) {
        return Props.create(new Creator<NodeActor>() {
            public NodeActor create() throws Exception {
                return new NodeActor(
                    hashedId, rawId, storagePath,
                    StartupCommand.BOOTSTRAP, null,
                    readQ, writeQ, rep, terminateOnLeave
                );
            }
        });
    }

    public static Props join(final int hashedId,
                             @NotNull final String rawId,
                             @NotNull final String storagePath,
                             @NotNull final String remote,
                             int readQ, int writeQ, int rep,
                             boolean terminateOnLeave) {
        return Props.create(new Creator<NodeActor>() {
            public NodeActor create() throws Exception {
                return new NodeActor(
                    hashedId, rawId, storagePath,
                    StartupCommand.JOIN, remote,
                    readQ, writeQ, rep, terminateOnLeave
                );
            }
        });
    }

	public static Props recover(final int hashedId,
                                @NotNull final String rawId,
                                @NotNull final String storagePath,
                                @NotNull final String remote,
                                int readQ, int writeQ, int rep,
                                boolean terminateOnLeave) {
        return Props.create(new Creator<NodeActor>() {
            public NodeActor create() throws Exception {
                return new NodeActor(
                    hashedId, rawId, storagePath,
                    StartupCommand.RECOVER, remote,
                    readQ, writeQ, rep, terminateOnLeave
                );
            }
        });
    }

    public void preStart() throws IOException {
        switch (startupCommand) {
            case BOOTSTRAP:
                storageManager.clearStorage();
                this.state = State.READY;
                // elevated to INFO
                logger.info("BOOTSTRAP complete: storage cleared, state={}", state);
                break;

            case JOIN:
                storageManager.clearStorage();
                getContext().actorSelection(remote)
                            .tell(new JoinRequestMessage(id), getSelf());
                this.state = State.JOINING_WAITING_NODES;
                logger.info("JOIN requested: contacting bootstrap [{}], state={}", remote, state);
                break;

            case RECOVER:
                getContext().actorSelection(remote)
                            .tell(new JoinRequestMessage(id), getSelf());
                this.state = State.RECOVERING_WAITING_NODES;
                logger.info("RECOVER requested: contacting [{}], state={}", remote, state);
                break;
        }
        assert this.state != null;
    }

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
		} else if (message instanceof WriteResponse) {
			onWriteResponse((WriteResponse) message);
		} else if (message instanceof ReadResponse) {
			onReadResponse((ReadResponse) message);
		} else if (message instanceof JoinDataMessage) {
			onJoinData((JoinDataMessage) message);
		} else if (message instanceof JoinMessage) {
			onJoin((JoinMessage) message);
		} else if (message instanceof ReJoinMessage) {
			onReJoin((ReJoinMessage) message);
		} else if (message instanceof LeaveMessage) {
			onLeave((LeaveMessage) message);
		} else if (message instanceof LeaveDataMessage) {
			onLeaveData((LeaveDataMessage) message);
		} else if (message instanceof TimeoutMessage) {
			onRequestTimeout((TimeoutMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void onJoinRequest(@NotNull JoinRequestMessage msg) {
		int sender = msg.getSenderID();
		if (state != State.JOINING_WAITING_NODES && state != State.RECOVERING_WAITING_NODES) {
			// Informative INFO-level log
			logger.info("Received JOIN request from node {}. Current ring members: {}",
				sender, new TreeSet<>(ring.getNodeIDs()));

			// Reply with the full membership
			reply(new NodesListMessage(id, ring.getNodes()));
		} else {
			logger.warning("JOIN request from node {} ignored (state = {})", sender, state);
		}
	}

	private void onDataRequest(@NotNull DataRequestMessage msg) {
		int sender = msg.getSenderID();
		if (state == State.READY) {
			Map<Integer, VersionedItem> records = storageManager.readRecords();
			logger.info("Received DATA request from node {}. Sending {} records: {}",
				sender, records.size(), records.keySet());

			reply(new JoinDataMessage(id, records));
		} else {
			logger.warning("DATA request from node {} ignored (state = {})", sender, state);
		}
	}

	private void onLeaveRequest() {
		logger.info("Client requested LEAVE. Handing off data and exiting.");

		// Prepare data handoff
		Map<Integer, Map<Integer, VersionedItem>> handoffs = new HashMap<>();
		Map<Integer, VersionedItem> allRecords = storageManager.readRecords();

		// Partition records among next replicas
		allRecords.forEach((key, value) -> {
			Set<Integer> successors = ring.nextResponsibleReplicasForLeaving(key);
			successors.forEach(replicaId ->
				handoffs
				.computeIfAbsent(replicaId, id -> new HashMap<>())
				.put(key, value)
			);
		});

		// Send handoff data
		handoffs.forEach((replicaId, recs) -> {
			logger.info("Sending {} records to successor node {}", recs.size(), replicaId);
			ring.getNode(replicaId).tell(new LeaveDataMessage(id, recs), getSelf());
		});

		// Notify remaining nodes of departure
		multicast(new LeaveMessage(id));
		reply(new ClientLeaveResponse(id));

		// Clean up storage
		storageManager.deleteStorage();

		if (terminateSystemOnLeave) {
			logger.info("Terminating actor system on leave.");
			getContext().system().terminate();
		} else {
			logger.info("Stopping actor (no system termination).");
			getContext().stop(getSelf());
		}
	}

	private void onNodesList(@NotNull NodesListMessage msg) {
		int sender = msg.getSenderID();
		Set<Integer> newMembers = msg.getNodes().keySet();
		logger.info("Received NODES_LIST from {}: {}", sender, newMembers);

		// Update ring membership
		ring.addNodes(msg.getNodes());

		switch (state) {
			case JOINING_WAITING_NODES:
				// Fetch data from predecessor
				ActorRef pred = ring.nextNodeInTheRing();
				logger.info("Requesting join data from predecessor node {}", ring.nextIDInTheRing());
				pred.tell(new DataRequestMessage(id), getSelf());
				state = State.JOINING_WAITING_DATA;
				logger.info("State -> {}", state);
				break;

			case RECOVERING_WAITING_NODES:
				// Clean up old records, rejoin
				dropOldKeys();
				ring.addNode(id, getSelf());
				logger.info("Re‐joining after crash: re‐announcing to all nodes");
				multicast(new ReJoinMessage(id));
				state = State.READY;
				logger.info("Recovery complete. State -> {}. Members = {}", state, new TreeSet<>(ring.getNodeIDs()));
				break;

			default:
				logger.warning("Unexpected NODES_LIST in state {} – ignoring", state);
		}
	}

	public void onClientReadRequest(@NotNull ClientReadRequest message) {
		final int key = message.getKey();
		final int clusterSize = ring.size();

		// Check quorum viability
		if (readQuorum > clusterSize) {
			logger.warning(
				"Read request for key={} denied: read quorum={} exceeds cluster size={}",
				key, readQuorum, clusterSize
			);
			reply(new ClientOperationErrorResponse(
				id,
				"Read failed: not enough nodes available for quorum"
			));
			return;
		}

		// Register the pending read
		requestCount++;
		readRequests.put(requestCount, new ReadRequestStatus(key, getSender(), readQuorum));

		// Compute placement
		final int hashedKey = HashUtil.hash(key);
		final Set<Integer> responsible = ring.responsibleForKey(key);

		// Debug‐level detail
		logger.debug(
			"Key placement: rawKey={} → hashedKey={} → replicas={}",
			key, hashedKey, responsible
		);

		// Kick off the read
		responsible.forEach(nodeId ->
			ring.getNode(nodeId).tell(new ReadRequest(id, requestCount, key), getSelf())
		);
		logger.info(
			"Read request for key={} forwarded to nodes {} (out of {})",
			key, responsible, ring.getNodeIDs()
		);

		// Schedule quorum timeout
		final TimeoutMessage timeout = new TimeoutMessage(id, requestCount);
		Cancellable timer = getContext().system().scheduler().scheduleOnce(
			Duration.create(QUORUM_TIMEOUT_SECONDS, TimeUnit.SECONDS),
			getSelf(), timeout,
			getContext().system().dispatcher(), getSelf()
		);
		requestsTimers.put(requestCount, timer);
	}


	public void onClientUpdateRequest(@NotNull ClientUpdateRequest message) {
		final int key = message.getKey();
		final String value = message.getValue();
		final int clusterSize = ring.size();

		// Check replication viability
		if (replication > clusterSize) {
			logger.warning(
				"Update request for key={} denied: replication factor={} exceeds cluster size={}",
				key, replication, clusterSize
			);
			reply(new ClientOperationErrorResponse(
				id,
				"Update failed: not enough nodes available for replication"
			));
			return;
		}

		// Register the pending update (gather phase)
		requestCount++;
		writeRequests.put(
			requestCount,
			new UpdateRequestStatus(key, value, getSender(), readQuorum, writeQuorum)
		);

		// Compute placement
		final int hashedKey = HashUtil.hash(key);
		final Set<Integer> responsible = ring.responsibleForKey(key);

		// Debug‐level detail
		logger.debug(
			"Key placement for update: rawKey={} → hashedKey={} → replicas={}",
			key, hashedKey, responsible
		);

		// Start by reading current versions from replicas
		responsible.forEach(nodeId ->
			ring.getNode(nodeId).tell(new ReadRequest(id, requestCount, key), getSelf())
		);
		logger.info(
			"Update request for key={} initiated, asking current versions from nodes {}",
			key, responsible
		);

		// Schedule quorum timeout
		final TimeoutMessage timeout = new TimeoutMessage(id, requestCount);
		Cancellable timer = getContext().system().scheduler().scheduleOnce(
			Duration.create(QUORUM_TIMEOUT_SECONDS, TimeUnit.SECONDS),
			getSelf(), timeout,
			getContext().system().dispatcher(), getSelf()
		);
		requestsTimers.put(requestCount, timer);
	}


protected void onReadRequest(ReadRequest message) {
    int requestId = message.getRequestID();
    int senderId  = message.getSenderID();
    int key       = message.getKey();

    // Log that we received an internal read request
    logger.info("Received ReadRequest[{}] from node {} for key={}", requestId, senderId, key);

    // Perform the lookup
    VersionedItem item = read(key);
    String value = (item != null ? item.getValue() : "NOT_FOUND");

    // Debug‐level detail
    logger.debug("Lookup result for key {}: {}", key, value);

    // Reply with the versioned item or null
    reply(new ReadResponse(id, requestId, key, item));
}

protected void onWriteRequest(WriteRequest message) {
    int requestId = message.getRequestID();
    int senderId  = message.getSenderID();
    int key       = message.getKey();
    VersionedItem newItem = message.getVersionedItem();

    // Log receipt of an internal write request
    logger.info(
    String.format(
        "Received WriteRequest[%s] from node %s: writing key=%s → '%s' (v%s)",
        requestId, senderId, key, newItem.getValue(), newItem.getVersion()
    )
);

    // Perform the write (write‐through cache + persistent storage)
    write(key, newItem);

    // Acknowledge back to the coordinator
    ring.getNode(senderId).tell(new WriteResponse(id, requestId), getSelf());
    logger.debug("Sent WriteResponse[{}] ack to node {}", requestId, senderId);
}

protected void onWriteResponse(WriteResponse message) {
    int requestId = message.getRequestID();
    int senderId  = message.getSenderID();

    // Grab the pending status, or ignore if stale
    UpdateResponseStatus status = writeResponses.get(requestId);
    if (status == null) {
        logger.debug("Ignoring stale WriteResponse[{}] from node {}", requestId, senderId);
        return;
    }

    // Log the acknowledgement
    logger.info("Received write ack for request {} from node {}", requestId, senderId);
    status.addAck(senderId);

    // Check for quorum
    if (status.isAckQuorumReached()) {
        Set<Integer> acks = status.getNodeThatAcknowledged();
        logger.info(
            "Write quorum reached for request {} with acknowledgements {}. Replying to client.",
            requestId, acks
        );

        // Send final success to the original client
        status.getSender().tell(
            new ClientUpdateResponse(id, status.getKey(), status.getVersionedItem()),
            getSelf()
        );

        // Cleanup
        writeResponses.remove(requestId);
    } else {
        Set<Integer> acks = status.getNodeThatAcknowledged();
        logger.debug(
            "Waiting for more write acks for request {}. Current acknowledgements: {}",
            requestId, acks
        );
    }
}

protected void onReadResponse(ReadResponse message) {
    int requestId = message.getRequestID();
    int senderId  = message.getSenderID();
    VersionedItem item = message.getValue();

    // Stale or unknown request?
    boolean isReadPending   = readRequests.containsKey(requestId);
    boolean isUpdatePending = writeRequests.containsKey(requestId);
    if (!isReadPending && !isUpdatePending) {
        logger.debug("Ignoring stale response for request {} from node {}", requestId, senderId);
        return;
    }

    // If this was a client‐read operation
    if (isReadPending) {
        ReadRequestStatus status = readRequests.get(requestId);
        status.addVote(item);

        logger.debug(
            "Received read vote for request {} from node {}: value={}",
            requestId, senderId,
            (item != null ? item.getValue() : "NULL")
        );

        if (status.isQuorumReached()) {
            String latest = status.getLatestValue();
            logger.info(
                "Read quorum achieved for request {}: returning '{}'",
                requestId, (latest != null ? latest : "NOT_FOUND")
            );

            status.getSender().tell(
                new ClientReadResponse(id, status.getKey(), latest),
                getSelf()
            );

            // Cancel timer and clean up
            requestsTimers.remove(requestId).cancel();
            readRequests.remove(requestId);
        } else {
            logger.debug(
                "Read quorum not yet reached for request {}: have {}/{} votes", 
                requestId,
                status.getVotesCount(),  // you might expose this in ReadRequestStatus
                readQuorum
            );
        }
    }

    // If this was an update (first‐phase) operation
    else { // isUpdatePending
        UpdateRequestStatus status = writeRequests.get(requestId);
        status.addVote(item);

        logger.debug(
            "Received update vote for request {} from node {}: value={}",
            requestId, senderId,
            (item != null ? item.getValue() : "NULL")
        );

        if (status.isQuorumReached()) {
            VersionedItem updated = status.getUpdatedRecord();
            logger.info(
                "Update quorum achieved for request {}: new record '{}'(v{})",
                requestId, updated.getValue(), updated.getVersion()
            );

            // Move to second phase: send WriteRequests
            UpdateResponseStatus respStatus = new UpdateResponseStatus(
                status.getKey(), updated, status.getSender(),
                readQuorum, writeQuorum
            );
            writeResponses.put(requestId, respStatus);

            Set<Integer> replicas = ring.responsibleForKey(status.getKey());
            replicas.forEach(nodeId -> 
                ring.getNode(nodeId).tell(
                    new WriteRequest(id, requestId, status.getKey(), updated),
                    getSelf()
                )
            );
            logger.info("Sent WriteRequest[{}] for key={} to replicas {}", 
                requestId, status.getKey(), replicas);

            // Cancel timer and clean up first phase
            requestsTimers.remove(requestId).cancel();
            writeRequests.remove(requestId);
        } else {
            logger.debug(
                "Update quorum not yet reached for request {}: have {}/{} votes", 
                requestId,
                status.getVotesCount(),  // expose in UpdateRequestStatus
                Math.max(readQuorum, writeQuorum)
            );
        }
    }
}

protected void onRequestTimeout(@NotNull TimeoutMessage msg) {
    int requestId = msg.getRequestID();

    ReadRequestStatus  readStatus   = readRequests.get(requestId);
    UpdateRequestStatus updateStatus = writeRequests.get(requestId);

    // If neither a read nor update is pending, ignore
    if (readStatus == null && updateStatus == null) {
        logger.debug("Timeout for request {} ignored: no pending operation", requestId);
        return;
    }

    // Otherwise, we’ve timed out waiting for quorum
    logger.warning("Operation timeout: request {} did not reach quorum, cancelling", requestId);

    // Notify the original client
    ActorRef client = (readStatus != null ? readStatus.getSender() : updateStatus.getSender());
    client.tell(
        new ClientOperationErrorResponse(id, "Operation timed out before quorum was reached"),
        getSelf()
    );

    // Clean up pending state & cancel timer
    readRequests.remove(requestId);
    writeRequests.remove(requestId);
    Cancellable timer = requestsTimers.remove(requestId);
    if (timer != null) timer.cancel();
}

protected void onJoinData(@NotNull JoinDataMessage msg) {
    // Should only happen during join phase
    if (state != State.JOINING_WAITING_DATA) {
        logger.warning("Unexpected JOIN_DATA from {} in state {} – ignoring", msg.getSenderID(), state);
        return;
    }

    Map<Integer, VersionedItem> records = msg.getRecords();
    logger.info(
        "Received initial data for join: {} keys from node {}",
        records.size(), msg.getSenderID()
    );

    // Persist & cache
    storageManager.appendRecords(records);
    cache.putAll(records);

    // Announce presence
    multicast(new JoinMessage(id));

    // Transition to ready
    state = State.READY;
    logger.info("Join complete: now READY. Ring members = {}", new TreeSet<>(ring.getNodeIDs()));
}

protected void onJoin(@NotNull JoinMessage msg) {
    int joiningId = msg.getSenderID();
    ring.addNode(joiningId, getSender());
    logger.info("Node {} joined the ring. Members = {}", joiningId, new TreeSet<>(ring.getNodeIDs()));

    // Remove records no longer our responsibility
    dropOldKeys();
    logger.debug("After join, cleaned up local records. Cache now holds keys = {}", cache.keySet());
}

protected void onReJoin(@NotNull ReJoinMessage msg) {
    int rejoiningId = msg.getSenderID();
    ring.addNode(rejoiningId, getSender());
    logger.info("Node {} re-joined after crash. Members = {}", rejoiningId, new TreeSet<>(ring.getNodeIDs()));
}

protected void onLeave(@NotNull LeaveMessage msg) {
    int leavingId = msg.getSenderID();
    ring.removeNode(leavingId);
    logger.info("Node {} gracefully left. Members = {}", leavingId, new TreeSet<>(ring.getNodeIDs()));
}

protected void onLeaveData(@NotNull LeaveDataMessage msg) {
    int fromId = msg.getSenderID();
    Map<Integer, VersionedItem> legacy = msg.getRecords();
    logger.info("Received {} legacy records from departing node {}", legacy.size(), fromId);

    // Merge into storage & cache
    storageManager.appendRecords(legacy);
    cache.putAll(legacy);
    logger.debug("Post-merge cache keys = {}", cache.keySet());
}
// Multicast a message to all other nodes in the ring
private void multicast(Serializable message) {
    Set<Integer> targets = ring.getNodeIDs().stream()
        .filter(nodeId -> nodeId != id)
        .collect(Collectors.toCollection(TreeSet::new));

    logger.info("Multicasting {} to nodes {}", message.getClass().getSimpleName(), targets);
    targets.forEach(nodeId ->
        ring.getNode(nodeId).tell(message, getSelf())
    );
}

// Send a direct reply to the original sender
private void reply(Serializable response) {
    ActorRef client = getSender();
    logger.debug("Replying {} to {}", response.getClass().getSimpleName(), client);
    client.tell(response, getSelf());
}

// In-memory cache lookup (used by both client and internal reads)
@Nullable
private VersionedItem read(int key) {
    VersionedItem item = cache.get(key);
    logger.debug(
        "Cache lookup for key={} → {}",
        key,
        (item != null ? String.format("value='%s',v=%d", item.getValue(), item.getVersion()) : "NOT_FOUND")
    );
    return item;
}
// Persistent + write-through cache write
private void write(int key, VersionedItem item) {
    logger.info(
        "Persisting key={} → '{}' (version={})",
        key, item.getValue(), item.getVersion()
    );
    storageManager.appendRecord(key, item);
    cache.put(key, item);
}

// Remove keys no longer this node's responsibility (on join/recovery)
private void dropOldKeys() {
    Map<Integer, VersionedItem> all = storageManager.readRecords();
    Set<Integer> keep = all.keySet().stream()
        .filter(k -> ring.responsibleForKey(k).contains(id))
        .collect(Collectors.toCollection(TreeSet::new));

    Set<Integer> removed = new TreeSet<>(all.keySet());
    removed.removeAll(keep);

    logger.info(
        "Repartitioning storage: keeping {} keys, removing {} keys",
        keep.size(), removed.size()
    );
    logger.debug("Old keys={}, New keys={}", all.keySet(), keep);

    // Persist only the kept keys
    Map<Integer, VersionedItem> filtered = keep.stream()
        .collect(Collectors.toMap(k -> k, all::get));
    storageManager.writeRecords(filtered);

    // Refresh cache
    cache.clear();
    cache.putAll(filtered);
}

	private enum StartupCommand {
		BOOTSTRAP,
		JOIN,
		RECOVER
	}
	private enum State {
		JOINING_WAITING_NODES,
		JOINING_WAITING_DATA,
		RECOVERING_WAITING_NODES,
		READY
	}
}
