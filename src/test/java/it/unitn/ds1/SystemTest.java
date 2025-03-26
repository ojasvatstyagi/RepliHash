package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unitn.ds1.client.commands.Command;
import it.unitn.ds1.client.commands.ReadCommand;
import it.unitn.ds1.client.commands.UpdateCommand;
import it.unitn.ds1.messages.JoinRequestMessage;
import it.unitn.ds1.node.NodeActor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration test for the whole system.
 * See http://doc.akka.io/docs/akka/current/java/testing.html for details.
 */
public class SystemTest {

	private static ActorSystem system;

	private ActorRef node10;
	private ActorRef node20;
	private ActorRef node30;
	private ActorRef node40;

	@BeforeClass
	public static void setup() {
		system = ActorSystem.create();
	}

	@AfterClass
	public static void teardown() {
		JavaTestKit.shutdownActorSystem(system);
		system = null;
	}

	private static void waitSomeTime() {
		new JavaTestKit(system) {{
			new ReceiveWhile<Boolean>(Boolean.class, duration("20 millis")) {
				protected Boolean match(Object in) {
					return true;
				}
			};
		}};
	}

	private static void waitForActorToBootstrap(ActorSystem system, ActorRef actor) {
		new JavaTestKit(system) {{
			new AwaitCond(duration("200 millis"), duration("20 millis")) {
				@Override
				protected boolean cond() {
					actor.tell(new JoinRequestMessage(0), getRef());
					return msgAvailable();
				}
			};
			new ReceiveWhile<Boolean>(Boolean.class, duration("10 millis")) {
				protected Boolean match(Object in) {
					return true;
				}
			};
			expectNoMsg();
		}};
	}

	private static Object executeCommand(ActorSystem system, ActorRef actor, Command command) throws Exception {
		final String remote = actor.path().toSerializationFormat();
		final LoggingAdapter logger = Logging.getLogger(system, Command.class);
		return command.run(system.actorSelection(actor.path()), remote, logger);
	}

	@Before
	public void bootstrap() {

		// get storage path
		final Config config = ConfigFactory.load();
		final String storagePath = config.getString("node.storage-path");

		// create initial node
		this.node10 = system.actorOf(NodeActor.bootstrap(10, storagePath));
		waitForActorToBootstrap(system, node10);

		// add a couple of nodes
		this.node20 = system.actorOf(NodeActor.join(20, storagePath, node10.path().toSerializationFormat()));
		waitForActorToBootstrap(system, node20);
		this.node30 = system.actorOf(NodeActor.join(30, storagePath, node20.path().toSerializationFormat()));
		waitForActorToBootstrap(system, node30);
		this.node40 = system.actorOf(NodeActor.join(40, storagePath, node30.path().toSerializationFormat()));
		waitForActorToBootstrap(system, node40);
	}

	@Test
	public void readNotFound() throws Exception {
		new JavaTestKit(system) {{

			// read 2: not existing
			final String value = (String) executeCommand(system, node10, new ReadCommand(2));
			assertNull(value);
		}};
	}

	@Test
	public void existingKey() throws Exception {
		new JavaTestKit(system) {{

			// write: 3 -> "ciao"
			final Object updateResult = executeCommand(system, node30, new UpdateCommand(3, "ciao"));
			assertNotNull(updateResult);

			// TODO: remove when project is finished
			waitSomeTime();

			// read 3: must be "ciao"
			final String readResult = (String) executeCommand(system, node10, new ReadCommand(3));
			assertEquals("ciao", readResult);
		}};
	}

	@Test
	public void multipleWrites() throws Exception {
		new JavaTestKit(system) {{

			// write: 3 -> "ciao"
			final Object updateResult1 = executeCommand(system, node20, new UpdateCommand(3, "ciao"));
			assertNotNull(updateResult1);

			// TODO: remove when project is finished
			waitSomeTime();

			// write: 3 -> "hello"
			final Object updateResult2 = executeCommand(system, node40, new UpdateCommand(3, "hello"));
			assertNotNull(updateResult2);

			// TODO: remove when project is finished
			waitSomeTime();

			// read 3: must be "hello"
			final String readResult = (String) executeCommand(system, node30, new ReadCommand(3));
			assertEquals("hello", readResult);
		}};
	}

	@Test
	public void multipleKeys() throws Exception {
		new JavaTestKit(system) {{

			// write: 3 -> "ciao"
			final Object updateResult1 = executeCommand(system, node10, new UpdateCommand(3, "ciao"));
			assertNotNull(updateResult1);

			// write: 55 -> "pippo"
			final Object updateResult2 = executeCommand(system, node10, new UpdateCommand(55, "pippo"));
			assertNotNull(updateResult2);

			// write: 22 -> "pluto"
			final Object updateResult3 = executeCommand(system, node10, new UpdateCommand(22, "pluto"));
			assertNotNull(updateResult3);

			// TODO: remove when project is finished
			waitSomeTime();

			// override 55 -> "topolino"
			final Object updateResult4 = executeCommand(system, node20, new UpdateCommand(55, "topolino"));
			assertNotNull(updateResult4);

			// TODO: remove when project is finished
			waitSomeTime();

			// read 3: must be "ciao"
			final String readResult1 = (String) executeCommand(system, node30, new ReadCommand(3));
			assertEquals("ciao", readResult1);

			// read 55: must be "topolino"
			final String readResult2 = (String) executeCommand(system, node30, new ReadCommand(55));
			assertEquals("topolino", readResult2);

			// read 22: must be "pluto"
			final String readResult3 = (String) executeCommand(system, node30, new ReadCommand(22));
			assertEquals("pluto", readResult3);
		}};
	}
}
