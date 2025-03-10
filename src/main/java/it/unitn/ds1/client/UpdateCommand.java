package it.unitn.ds1.client;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.unitn.ds1.messages.client.ClientUpdateRequest;
import it.unitn.ds1.messages.client.ClientUpdateResponse;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

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

		// log the request
		logger.info("Request update of key {} with value \"{}\"...", key, value);

		// send the command to the actor
		final Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
		final Future<Object> future = Patterns.ask(actor, new ClientUpdateRequest(key, value), timeout);

		// wait for an acknowledgement
		final Object message = Await.result(future, timeout.duration());
		assert message instanceof ClientUpdateResponse;
		final ClientUpdateResponse result = (ClientUpdateResponse) message;

		// log the result
		logger.info("Actor [{}] replies... value of key ({}) has been updated (value: \"{}\", version: {})",
			result.getSenderID(), result.getKey(), result.getVersionedItem().getValue(), result.getVersionedItem().getVersion());

		// TODO: return some exit code???
	}

}
