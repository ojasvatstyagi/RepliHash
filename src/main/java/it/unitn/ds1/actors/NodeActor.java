package it.unitn.ds1.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import it.unitn.ds1.messages.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Akka actor that implements the Node's behaviour.
 *
 * @author Davide Pedranz
 */
public class NodeActor extends UntypedActor {

	/**
	 * Unique identifier for this Node.
	 */
	private final int id;

	/**
	 * Initial state of this Node.
	 * This is used for a convenient initialization of the actor.
	 */
	private final InitialState initialState;

	/**
	 * Akka remote path to contact another Node.
	 * This is used to make the node join an existing system.
	 */
	private final String remote;

	/**
	 * Logger, used for debug proposes.
	 */
	private final LoggingAdapter logger;


	/**
	 * Internal variable used to keep track of the other nodes in the system.
	 * NB: this map contains also myself!
	 */
	private final Map<Integer, ActorRef> nodes;

	/**
	 * Internal variable used to store the current state of the Node.
	 */
	private State state;


	// TODO: maybe initialState is not the best name

	/**
	 * Create a new Node Actor.
	 *
	 * @param id           Unique identifier to assign to this node.
	 * @param initialState Initial state of the Node. This determines the behaviour of the Node when started.
	 * @param remote       Remote address of another actor to contact to join the system.
	 *                     This parameter is not required for the bootstrap node.
	 */
	private NodeActor(int id, @NotNull InitialState initialState, @Nullable String remote) {
		this.id = id;
		this.initialState = initialState;
		this.remote = remote;
		this.logger = Logging.getLogger(getContext().system(), this);
		this.nodes = new HashMap<>();

		// add myself to the map of nodes
		this.nodes.put(id, getSelf());

		// TODO: maybe not needed
		// current state is STARTING... this will be changed in the preStart()
		this.state = State.STARTING;

		// debug log
		logger.info("Node [" + id + "] -> constructor: id={}, init={}, remote={}", id, initialState, remote);
	}

	/**
	 * Create Props for a Node that should bootstrap the system.
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
	 * Create Props for a new Node that is willing to join the system.
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
	 * on the initial state. For instance, if the Node needs to join the
	 * system, we send a message to a remote Node already in the system.
	 */
	@Override
	public void preStart() {

		// depending on the initialization, decide what to do
		switch (initialState) {

			// nothing needed in this case
			case BOOTSTRAP:
				this.state = State.READY;
				logger.info("Node [" + id + "] -> preStart(), do nothing, state={}", this.state);
				break;

			// send a message to the node provided from the command line
			// to ask to join the system
			case JOIN:
				this.state = State.JOINING_WAITING_NODES;
				logger.info("Node [" + id + "] -> preStart(), asking to join to {}, state={}", remote, this.state);
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
		} else if (message instanceof DataMessage) {
			onData((DataMessage) message);
		} else if (message instanceof JoinMessage) {
			onJoin((JoinMessage) message);
		} else {
			unhandled(message);
		}
	}

	/**
	 * A new Node is requiring to join the system.
	 *
	 * @param message Join message.
	 */
	private void onJoinRequest(@NotNull JoinRequestMessage message) {

		// TODO: check that I am ready to reply

		logger.info("Node [{}] asks to join the network", message.getId());

		// send back the list of nodes
		getSender().tell(new NodesListMessage(nodes), getSelf());
	}

	/**
	 * A Node is requiring the data I am responsible for.
	 *
	 * @param message Data Request message.
	 */
	@SuppressWarnings("UnusedParameters")
	private void onDataRequest(@NotNull DataRequestMessage message) {

		// TODO: check that I am ready to reply

		logger.info("Somebody asked me the data");

		// TODO: extract the data

		// send back the data
		getSender().tell(new DataMessage(), getSelf());
	}

	/**
	 * Somebody sent me the list of Nodes in the system.
	 *
	 * @param message List of Nodes Message,
	 */
	private void onNodesList(@NotNull NodesListMessage message) {
		assert this.state == State.JOINING_WAITING_NODES;

		// TODO: should I remove nodes not in this list? [I guess not]

		logger.info("Somebody sent the list of nodes: {}", message.getNodes());

		// update my list of nodes
		this.nodes.putAll(message.getNodes());

		// compute the next node in the ring
		final int next = nextInTheRing(nodes.keySet(), this.id);
		final ActorRef nextNode = nodes.get(next);

		// ask the data the Node is responsible for
		nextNode.tell(new DataRequestMessage(), getSelf());
		this.state = State.JOINING_WAITING_DATA;
	}

	/**
	 * Somebody sent me the list of data it is responsible for.
	 *
	 * @param message Data Message.
	 */
	@SuppressWarnings("UnusedParameters")
	private void onData(@NotNull DataMessage message) {
		assert this.state == State.JOINING_WAITING_DATA;

		// TODO: store data

		logger.info("Somebody sent the me the data it is responsible for. Sending Join msg...");

		// announce everybody that I am part of the system
		this.nodes.entrySet()
			.stream()
			.filter(entry -> entry.getKey() != id)
			.forEach(entry -> entry.getValue().tell(new JoinMessage(id), getSelf()));

		// now I am ready to serve requests
		this.state = State.READY;

		logger.info("Now I am part of the system. My nodes are: {}", nodes.keySet());
	}

	/**
	 * A new Node has joined the system.
	 *
	 * @param message Join Message.
	 */
	private void onJoin(JoinMessage message) {
		logger.info("Node [{}] is joining", message.getId());

		// add the Node to my list
		this.nodes.put(message.getId(), getSender());

		// TODO: remove
		logger.info("Update my set of nodes: {}", nodes.keySet());

		// TODO: remove the keys I am not responsible for
	}

	/**
	 * Enumeration of possible initial states for a Node.
	 * This is used to run the proper action in the #preStart() method.
	 */
	private enum InitialState {
		BOOTSTRAP,
		JOIN,
		RECOVER
	}

	/**
	 * Enumeration of all possible states the Node is in.
	 * For example, the Node is joining the network and is waiting
	 * for some reply to get operational.
	 */
	private enum State {
		STARTING,
		JOINING_WAITING_NODES,
		JOINING_WAITING_DATA,
		READY
	}
}
