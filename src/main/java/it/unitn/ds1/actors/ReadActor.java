package it.unitn.ds1.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;
import akka.japi.Creator;
import it.unitn.ds1.messages.ClientReadRequestMessage;
import it.unitn.ds1.messages.ClientReadResultMessage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Davide Pedranz (pedranz@fbk.eu)
 */
public class ReadActor extends UntypedActor {

	/**
	 * Akka remote path to contact another Node.
	 * This is used to make the node leave an existing system.
	 */
	private final String remote;

	/**
	 * Key to read.
	 */
	private final int key;

	/**
	 * Logger, used for debug proposes.
	 */
	private final DiagnosticLoggingAdapter logger;


	private ReadActor(@NotNull String remote, int key) {
		this.remote = remote;
		this.key = key;

		// setup logger context
		this.logger = Logging.getLogger(this);
		final Map<String, Object> mdc = new HashMap<String, Object>() {{
			put("actor", "Client");
		}};
		logger.setMDC(mdc);
	}

	public static Props read(String remote, int key) {
		return Props.create(new Creator<ReadActor>() {
			private static final long serialVersionUID = 1L;

			@Override
			public ReadActor create() throws Exception {
				return new ReadActor(remote, key);
			}
		});
	}

	@Override
	public void preStart() throws Exception {
		logger.info("Requesting key {} to {}", key, remote);
		getContext().actorSelection(remote).tell(new ClientReadRequestMessage(key), getSelf());

		// TODO: handle timeouts
	}

	@Override
	public void onReceive(Object message) throws Throwable {
		assert message instanceof ClientReadResultMessage;
		onResult((ClientReadResultMessage) message);
	}

	private void onResult(ClientReadResultMessage message) {
		logger.info("Read: found={}, key={}, value={}", message.keyFound(), message.getKey(), message.getValue());
		getContext().system().terminate();
	}
}
