package it.unitn.ds1.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;
import akka.japi.Creator;
import it.unitn.ds1.messages.LeaveAcknowledgmentMessage;
import it.unitn.ds1.messages.LeaveRequestMessage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * This actor is used only by the client.
 * At start-up, it contacts the designed remote Node that should leave the system.
 * It waits for an acknowledgment, then terminates the client.
 */
public class LeaveActor extends UntypedActor {

	/**
	 * Akka remote path to contact another Node.
	 * This is used to make the node leave an existing system.
	 */
	private final String remote;

	/**
	 * Logger, used for debug proposes.
	 */
	private final DiagnosticLoggingAdapter logger;


	private LeaveActor(@NotNull String remote) {
		this.remote = remote;

		// setup logger context
		this.logger = Logging.getLogger(this);
		final Map<String, Object> mdc = new HashMap<String, Object>() {{
			put("actor", "Client");
		}};
		logger.setMDC(mdc);
	}

	public static Props leave(String remote) {
		return Props.create(new Creator<LeaveActor>() {
			private static final long serialVersionUID = 1L;

			@Override
			public LeaveActor create() throws Exception {
				return new LeaveActor(remote);
			}
		});
	}

	@Override
	public void preStart() throws Exception {
		getContext().actorSelection(remote).tell(new LeaveRequestMessage(), getSelf());
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof LeaveAcknowledgmentMessage) {
			logger.warning("ACK received");
			getContext().system().terminate();
		}
	}
}
