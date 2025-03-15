package it.unitn.ds1.client;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.unitn.ds1.SystemConstants;
import it.unitn.ds1.messages.client.ClientLeaveRequest;
import it.unitn.ds1.messages.client.ClientLeaveResponse;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

import static it.unitn.ds1.SystemConstants.CLIENT_TIMEOUT_SECONDS;

/**
 * Command to instruct the target actor to leave the system.
 */
public final class LeaveCommand extends BaseCommand {

	public LeaveCommand(String ip, String port) {
		super(ip, port);
	}

	@Override
	protected void command(ActorSelection actor, LoggingAdapter logger) throws Exception {

		// instruct the target actor to leave the system
		final Timeout timeout = new Timeout(CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		final Future<Object> future = Patterns.ask(actor, new ClientLeaveRequest(), timeout);

		// wait for an acknowledgement
		final Object message = Await.result(future, timeout.duration());
		assert message instanceof ClientLeaveResponse;

		// log the result
		logger.info("Actor [{}] has successful left the system.", ((ClientLeaveResponse) message).getSenderID());
	}
}
