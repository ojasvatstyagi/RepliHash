package it.unitn.ds1.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;
import akka.japi.Creator;
import it.unitn.ds1.messages.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
	 * Create Props for a new node that is willing to leave the system.
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

			// send a message to the node provided from the command line
			// to ask to leave the system
			case JOIN:
				this.state = State.JOINING_WAITING_NODES;
				logger.info("preStart(): move to {}, ask to join to {}", state, remote);
				getContext().actorSelection(remote).tell(new JoinRequestMessage(id), getSelf());
				break;

			// TODO
			case RECOVER:
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
		getSender().tell(new NodesListMessage(nodes), getSelf());
	}

	@SuppressWarnings("UnusedParameters")
	private void onDataRequest(@NotNull DataRequestMessage message) {

		// TODO: check that I am ready to reply

		logger.info("Somebody asked me the data");

		// TODO: extract the data

		// send back the data
		getSender().tell(new DataMessage(), getSelf());
	}

	private void onLeaveRequest() {
		logger.info("Leave request");

		// TODO: do stuff, exit protocol

		// inform all nodes that I am leaving
		multicast(new LeaveMessage(id));

		// eventually, acknowledge the client
		getSender().tell(new LeaveAcknowledgmentMessage(), getSelf());

		// TODO: cancel the storage?

		// shutdown
		getContext().system().terminate();
	}

	private void onNodesList(@NotNull NodesListMessage message) {
		assert this.state == State.JOINING_WAITING_NODES;

		// TODO: should I remove nodes not in this list? [I guess not]

		logger.info("Somebody sent the list of nodes: {}", message.getNodes());

		// update my list of nodes
		this.nodes.putAll(message.getNodes());

		// compute the next node in the ring
		final int next = nextInTheRing(nodes.keySet(), this.id);
		final ActorRef nextNode = nodes.get(next);

		// ask the data the node is responsible for
		nextNode.tell(new DataRequestMessage(), getSelf());
		this.state = State.JOINING_WAITING_DATA;
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
	 * Enumeration of possible initial states for a node.
	 * This is used to run the proper action in the #preStart() method.
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
		READY
	}
}
