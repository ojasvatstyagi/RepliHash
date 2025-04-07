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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Integration test for the whole system.
 * See http://doc.akka.io/docs/akka/current/java/testing.html for details.
 */
public final class SystemNotReadyTest {

	// system constants
	private static final int READ_QUORUM = 3;
	private static final int WRITE_QUORUM = 2;
	private static final int REPLICATION = 3;

	// testing system
	private static ActorSystem system;

	// nodes in the test
	private ActorRef node10;
	private ActorRef node20;

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

		// add another node
		this.node20 = system.actorOf(NodeActor.join(20, storagePath, node10.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node20);
	}

	@Test
	public void read() throws Exception {
		new JavaTestKit(system) {{

			// write: 3 -> "ciao"
			final CommandResult updateResult1 = TestUtilities.executeCommand(system, node10, new ReadCommand(3));
			assertFalse(updateResult1.isSuccess());
			assertNull(updateResult1.getResult());
		}};
	}

	@Test
	public void write() throws Exception {
		new JavaTestKit(system) {{

			// write: 3 -> "ciao"
			final CommandResult updateResult1 = TestUtilities.executeCommand(system, node20, new UpdateCommand(3, "ciao"));
			assertFalse(updateResult1.isSuccess());
			assertNull(updateResult1.getResult());
		}};
	}

}
