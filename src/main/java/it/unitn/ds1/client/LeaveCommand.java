package it.unitn.ds1.client;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.unitn.ds1.messages.LeaveAcknowledgmentMessage;
import it.unitn.ds1.messages.LeaveRequestMessage;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

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
		final Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
		final Future<Object> future = Patterns.ask(actor, new LeaveRequestMessage(), timeout);

		// wait for an acknowledgement
		final Object message = Await.result(future, timeout.duration());
		assert message instanceof LeaveAcknowledgmentMessage;

		// log the result
		logger.info("Actor [{}] has successful left the system.", ((LeaveAcknowledgmentMessage) message).getId());
	}
}
