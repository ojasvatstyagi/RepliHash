package it.unitn.ds1.client;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.unitn.ds1.messages.client.ClientReadRequest;
import it.unitn.ds1.messages.client.ClientReadResponse;
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
		logger.info("[CLIENT] Read key [{}] from node [{}]...", key, getRemote());

		// send the command to the actor
		final Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
		final Future<Object> future = Patterns.ask(actor, new ClientReadRequest(key), timeout);

		// wait for an acknowledgement
		final Object message = Await.result(future, timeout.duration());
		assert message instanceof ClientReadResponse;
		final ClientReadResponse result = (ClientReadResponse) message;

		// log the result
		if (result.keyFound()) {
			logger.info("[CLIENT] Actor [{}] replies: value for key [{}] is \"{}\"",
				result.getSenderID(), result.getKey(), result.getValue());
		} else {
			logger.warning("[CLIENT] Actor [{}] replies: key [{}] was NOT FOUND",
				result.getSenderID(), result.getKey());
		}

		// TODO: return some exit code???
	}

}
