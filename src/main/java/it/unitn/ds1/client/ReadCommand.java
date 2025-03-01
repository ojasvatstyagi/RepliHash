package it.unitn.ds1.client;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.unitn.ds1.messages.ClientReadRequestMessage;
import it.unitn.ds1.messages.ClientReadResultMessage;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

/**
 * Command used to read the value of a key from the system.
 */
public final class ReadCommand extends BaseCommand {

	// internal variables
	private final int key;

	public ReadCommand(String ip, String port, int key) {
		super(ip, port);
		this.key = key;
	}

	@Override
	protected void command(ActorSelection actor, LoggingAdapter logger) throws Exception {

		// log the request
		logger.info("Requesting key {}...", key);

		// send the command to the actor
		final Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
		final Future<Object> future = Patterns.ask(actor, new ClientReadRequestMessage(key), timeout);

		// wait for an acknowledgement
		final Object message = Await.result(future, timeout.duration());
		assert message instanceof ClientReadResultMessage;
		final ClientReadResultMessage result = (ClientReadResultMessage) message;

		// log the result
		if (result.keyFound()) {
			logger.info("Actor [{}] replies... value of key ({}) is \"{}\"",
				result.getId(), result.getKey(), result.getValue());
		} else {
			logger.warning("Actor [{}] replies... key ({}) was NOT found on the system\"\"",
				result.getId(), result.getKey(), result.getValue());
		}
	}

}
