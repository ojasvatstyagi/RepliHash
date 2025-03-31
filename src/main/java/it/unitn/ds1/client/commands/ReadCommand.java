package it.unitn.ds1.client.commands;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.unitn.ds1.messages.client.ClientOperationErrorResponse;
import it.unitn.ds1.messages.client.ClientReadRequest;
import it.unitn.ds1.messages.client.ClientReadResponse;
import org.jetbrains.annotations.NotNull;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

import static it.unitn.ds1.SystemConstants.CLIENT_TIMEOUT_SECONDS;

/**
 * Command used to read the value of a key from the system.
 */
public final class ReadCommand implements Command {

	// internal variables
	private final int key;

	public ReadCommand(int key) {
		this.key = key;
	}

	@Override
	@NotNull
	public CommandResult run(ActorSelection actor, String remote, LoggingAdapter logger) throws Exception {
		logger.info("[CLIENT] Read key [{}] from node [{}]...", key, remote);

		// send the command to the actor
		final Timeout timeout = new Timeout(CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		final Future<Object> future = Patterns.ask(actor, new ClientReadRequest(key), timeout);

		// wait for an acknowledgement
		final Object message = Await.result(future, timeout.duration());
		assert message instanceof ClientReadResponse || message instanceof ClientOperationErrorResponse;

		// an error has occurred
		if (message instanceof ClientOperationErrorResponse) {

			// log the cause of the error
			final ClientOperationErrorResponse result = (ClientOperationErrorResponse) message;
			logger.error("Actor [{}] replies... read operation has failed. Reason: \"{}\"",
				result.getSenderID(), result.getMessage());

			// command failed, nothing to return
			return new CommandResult(false, null);
		}

		// success
		else {

			// log the result
			final ClientReadResponse result = (ClientReadResponse) message;
			if (result.keyFound()) {
				logger.info("[CLIENT] Actor [{}] replies: value for key [{}] is \"{}\"",
					result.getSenderID(), result.getKey(), result.getValue());
			} else {
				logger.warning("[CLIENT] Actor [{}] replies: key [{}] was NOT FOUND",
					result.getSenderID(), result.getKey());
			}

			return new CommandResult(true, result.getValue());
		}
	}

}
