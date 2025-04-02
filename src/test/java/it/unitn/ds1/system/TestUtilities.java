package it.unitn.ds1.system;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;
import it.unitn.ds1.client.commands.Command;
import it.unitn.ds1.client.commands.CommandResult;
import it.unitn.ds1.messages.JoinRequestMessage;

/**
 * Some utility methods to test the whole system.
 */
final class TestUtilities {

	static void waitSomeTime(ActorSystem system) {
		new JavaTestKit(system) {{
			new ReceiveWhile<Boolean>(Boolean.class, duration("20 millis")) {
				protected Boolean match(Object in) {
					return true;
				}
			};
		}};
	}

	static void waitForActorToBootstrap(ActorSystem system, ActorRef actor) {
		new JavaTestKit(system) {{
			new AwaitCond(duration("500 millis"), duration("25 millis")) {
				@Override
				protected boolean cond() {
					actor.tell(new JoinRequestMessage(0), getRef());
					return msgAvailable();
				}
			};
			new ReceiveWhile<Boolean>(Boolean.class, duration("20 millis")) {
				protected Boolean match(Object in) {
					return true;
				}
			};
			expectNoMsg();
		}};
	}

	static CommandResult executeCommand(ActorSystem system, ActorRef actor, Command command) throws Exception {
		final String remote = actor.path().toSerializationFormat();
		final LoggingAdapter logger = Logging.getLogger(system, Command.class);
		return command.run(system.actorSelection(actor.path()), remote, logger);
	}

}
