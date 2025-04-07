package it.unitn.ds1.system;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unitn.ds1.client.commands.CommandResult;
import it.unitn.ds1.client.commands.ReadCommand;
import it.unitn.ds1.client.commands.UpdateCommand;
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
public final class SystemReadyTest {

	// system constants
	private static final int READ_QUORUM = 2;
	private static final int WRITE_QUORUM = 2;
	private static final int REPLICATION = 3;

	// testing system
	private static ActorSystem system;

	// nodes in the test
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

	@Before
	public void bootstrap() {

		// get storage path
		final Config config = ConfigFactory.load();
		final String storagePath = config.getString("node.storage-path");

		// create initial node
		this.node10 = system.actorOf(NodeActor.bootstrap(10, storagePath, READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node10);

		// add a couple of nodes
		this.node20 = system.actorOf(NodeActor.join(20, storagePath, node10.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node20);

		this.node30 = system.actorOf(NodeActor.join(30, storagePath, node20.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node30);

		this.node40 = system.actorOf(NodeActor.join(40, storagePath, node30.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node40);
	}

	@Test
	public void readNotFound() throws Exception {
		new JavaTestKit(system) {{

			// read 2: not existing
			final CommandResult readResult = TestUtilities.executeCommand(system, node10, new ReadCommand(2));
			assertTrue(readResult.isSuccess());
			assertNull(readResult.getResult());
		}};
	}

	@Test
	public void existingKey() throws Exception {
		new JavaTestKit(system) {{

			// write: 3 -> "ciao"
			final Object updateResult = TestUtilities.executeCommand(system, node30, new UpdateCommand(3, "ciao"));
			assertNotNull(updateResult);

			// read 3: must be "ciao"
			final CommandResult readResult = TestUtilities.executeCommand(system, node10, new ReadCommand(3));
			assertTrue(readResult.isSuccess());
			assertEquals("ciao", readResult.getResult());
		}};
	}

	@Test
	public void multipleWrites() throws Exception {
		new JavaTestKit(system) {{

			// write: 3 -> "ciao"
			final Object updateResult1 = TestUtilities.executeCommand(system, node20, new UpdateCommand(3, "ciao"));
			assertNotNull(updateResult1);

			// write: 3 -> "hello"
			final Object updateResult2 = TestUtilities.executeCommand(system, node40, new UpdateCommand(3, "hello"));
			assertNotNull(updateResult2);

			// read 3: must be "hello"
			final CommandResult readResult = TestUtilities.executeCommand(system, node30, new ReadCommand(3));
			assertTrue(readResult.isSuccess());
			assertEquals("hello", readResult.getResult());
		}};
	}

	@Test
	public void multipleKeys() throws Exception {
		new JavaTestKit(system) {{

			// write: 3 -> "ciao"
			final Object updateResult1 = TestUtilities.executeCommand(system, node10, new UpdateCommand(3, "ciao"));
			assertNotNull(updateResult1);

			// write: 55 -> "pippo"
			final Object updateResult2 = TestUtilities.executeCommand(system, node10, new UpdateCommand(55, "pippo"));
			assertNotNull(updateResult2);

			// write: 22 -> "pluto"
			final Object updateResult3 = TestUtilities.executeCommand(system, node10, new UpdateCommand(22, "pluto"));
			assertNotNull(updateResult3);

			// override 55 -> "topolino"
			final Object updateResult4 = TestUtilities.executeCommand(system, node20, new UpdateCommand(55, "topolino"));
			assertNotNull(updateResult4);

			// read 3: must be "ciao"
			final CommandResult readResult1 = TestUtilities.executeCommand(system, node30, new ReadCommand(3));
			assertTrue(readResult1.isSuccess());
			assertEquals("ciao", readResult1.getResult());

			// read 55: must be "topolino"
			final CommandResult readResult2 = TestUtilities.executeCommand(system, node30, new ReadCommand(55));
			assertTrue(readResult2.isSuccess());
			assertEquals("topolino", readResult2.getResult());

			// read 22: must be "pluto"
			final CommandResult readResult3 = TestUtilities.executeCommand(system, node30, new ReadCommand(22));
			assertTrue(readResult3.isSuccess());
			assertEquals("pluto", readResult3.getResult());
		}};
	}
}
