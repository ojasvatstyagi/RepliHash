package it.unitn.ds1.client;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.unitn.ds1.messages.client.ClientOperationErrorResponse;
import it.unitn.ds1.messages.client.ClientUpdateRequest;
import it.unitn.ds1.messages.client.ClientUpdateResponse;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

import static it.unitn.ds1.SystemConstants.CLIENT_TIMEOUT_SECONDS;

/**
 * Command used to update a record in the system.
 */
public final class UpdateCommand extends BaseCommand {

	// internal variables
	private final int key;
	private final String value;

	public UpdateCommand(String ip, String port, int key, String value) {
		super(ip, port);
		this.key = key;
		this.value = value;
	}

	@Override
	protected void command(ActorSelection actor, LoggingAdapter logger) throws Exception {
		logger.info("[CLIENT] Update key [{}] with value \"{}\" on node [{}]...", key, value, getRemote());

		// send the command to the actor
		final Timeout timeout = new Timeout(CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		final Future<Object> future = Patterns.ask(actor, new ClientUpdateRequest(key, value), timeout);

		// wait for an acknowledgement
		final Object message = Await.result(future, timeout.duration());
		assert message instanceof ClientUpdateResponse || message instanceof ClientOperationErrorResponse;

		if (message instanceof ClientOperationErrorResponse) { // an error has occurred

			final ClientOperationErrorResponse result = (ClientOperationErrorResponse) message;

			logger.error("Actor [{}] replies... update operation has failed. Reason: \"{}\"",
				result.getSenderID(), result.getMessage());

		} else {

			final ClientUpdateResponse result = (ClientUpdateResponse) message;

			// log the result
			logger.info("[CLIENT] Actor [{}] replies: key [{}] has been updated (value: \"{}\", version: {})",
				result.getSenderID(), result.getKey(), result.getVersionedItem().getValue(), result.getVersionedItem().getVersion());
		}

		// TODO: return some exit code???
	}

}
