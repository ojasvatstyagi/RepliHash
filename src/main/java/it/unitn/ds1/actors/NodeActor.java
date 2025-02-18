package it.unitn.ds1.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

		logger.debug("NodeActor [" + id + "] -> constructor: id={}, init={}, remote={}", id, initialState, remote);
	}

	/**
	 * Create Props for a Node that should bootstrap the system.
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
	 * This method is called after the constructor, when the actor is ready.
	 * We use this to do the initial actions required by the actor, depending
	 * on the initial state. For instance, if the Node needs to join the
	 * system, we send a message to a remote Node already in the system.
	 */
	@Override
	public void preStart() {
		logger.debug("NodeActor [" + id + "] -> preStart()");

		// depending on the initialization, decide what to do
		switch (initialState) {

			// nothing needed in this case
			case BOOTSTRAP:
				break;

			// TODO
			case JOIN:
				break;

			// TODO
			case RECOVER:
				break;
		}
	}

	@Override
	public void onReceive(Object message) {
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
}
