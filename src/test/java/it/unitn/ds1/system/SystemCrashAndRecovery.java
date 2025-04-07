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
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Integration test for the whole system.
 * See http://doc.akka.io/docs/akka/current/java/testing.html for details.
 */
public final class SystemCrashAndRecovery {

	// system constants
	private static final int READ_QUORUM = 2;
	private static final int WRITE_QUORUM = 1;
	private static final int REPLICATION = 2;

	// testing system
	private static ActorSystem system;

	@BeforeClass
	public static void setup() {
		system = ActorSystem.create();
	}

	@AfterClass
	public static void teardown() {
		JavaTestKit.shutdownActorSystem(system);
		system = null;
	}

	@Test
	public void crashAndRecover() throws Exception {

		// get storage path
		final Config config = ConfigFactory.load();
		final String storagePath = config.getString("node.storage-path");

		// create initial node
		final ActorRef node10 = system.actorOf(NodeActor.bootstrap(10, storagePath, READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node10);

		// add another node
		ActorRef node20 = system.actorOf(NodeActor.join(20, storagePath, node10.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node20);

		// write some key
		final CommandResult write = TestUtilities.executeCommand(system, node20, new UpdateCommand(1, "pluto"));
		assertTrue(write.isSuccess());

		// crash one node... break the quorums
		system.stop(node20);

		// read -> timeout
		try {
			TestUtilities.executeCommand(system, node20, new ReadCommand(44));
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof TimeoutException);
		}

		// read -> no quorum
		final CommandResult readNoQuorum = TestUtilities.executeCommand(system, node10, new ReadCommand(1));
		assertFalse(readNoQuorum.isSuccess());

		// add new node, still no quorum (node20 is responsible for the key)
		final ActorRef node30 = system.actorOf(NodeActor.join(30, storagePath, node10.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node30);

		// read -> no quorum2
		final CommandResult readNoQuorum2 = TestUtilities.executeCommand(system, node30, new ReadCommand(1));
		assertFalse(readNoQuorum2.isSuccess());

		// recover node20
		node20 = system.actorOf(NodeActor.recover(20, storagePath, node10.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node20);

		// read -> ok
		final CommandResult readOk = TestUtilities.executeCommand(system, node20, new ReadCommand(1));
		assertTrue(readOk.isSuccess());
		assertEquals("pluto", readOk.getResult());
	}

}
