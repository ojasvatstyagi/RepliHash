package it.unitn.ds1.system;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unitn.ds1.client.commands.CommandResult;
import it.unitn.ds1.client.commands.LeaveCommand;
import it.unitn.ds1.client.commands.ReadCommand;
import it.unitn.ds1.client.commands.UpdateCommand;
import it.unitn.ds1.node.NodeActor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration test for the whole system.
 * See http://doc.akka.io/docs/akka/current/java/testing.html for details.
 */
public class SystemJoinAndLeave {

	// system constants
	private static final int READ_QUORUM = 1;
	private static final int WRITE_QUORUM = 2;
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
	public void allLeaving() throws Exception {

		// get storage path
		final Config config = ConfigFactory.load();
		final String storagePath = config.getString("node.storage-path");

		// create initial node
		final ActorRef node10 = system.actorOf(NodeActor.bootstrap(10, storagePath, READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node10);

		// add other nodes
		final ActorRef node20 = system.actorOf(NodeActor.join(20, storagePath, node10.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node20);
		final ActorRef node30 = system.actorOf(NodeActor.join(30, storagePath, node10.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node30);

		// write a key -> should be stored on nodes 10 and 20
		final CommandResult write2 = TestUtilities.executeCommand(system, node30, new UpdateCommand(1, "ciao"));
		assertTrue(write2.isSuccess());

		// TODO: remove when project is finished
		TestUtilities.waitSomeTime(system);

		// TODO: check storage here!

		// make 1 node leave -> key should still be available
		final CommandResult leave1 = TestUtilities.executeCommand(system, node10, new LeaveCommand());
		assertTrue(leave1.isSuccess());

		// read key, should be still in the system
		final CommandResult read1 = TestUtilities.executeCommand(system, node30, new ReadCommand(1));
		assertTrue(read1.isSuccess());
		assertEquals("ciao", read1.getResult());

		// make another node leave -> key should still be available
		final CommandResult leave2 = TestUtilities.executeCommand(system, node20, new LeaveCommand());
		assertTrue(leave2.isSuccess());

		// read key, should be still in the system
		final CommandResult read2 = TestUtilities.executeCommand(system, node30, new ReadCommand(1));
		assertTrue(read2.isSuccess());
		assertEquals("ciao", read2.getResult());
	}

	@Test
	public void joinAndLeave() throws Exception {

		// get storage path
		final Config config = ConfigFactory.load();
		final String storagePath = config.getString("node.storage-path");

		// create initial node
		final ActorRef node10 = system.actorOf(NodeActor.bootstrap(10, storagePath, READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node10);

		// ask a key -> not found
		final CommandResult read1 = TestUtilities.executeCommand(system, node10, new ReadCommand(3));
		assertTrue(read1.isSuccess());
		assertNull(read1.getResult());

		// write a key -> fail
		final CommandResult write1 = TestUtilities.executeCommand(system, node10, new UpdateCommand(1, "ciao"));
		assertFalse(write1.isSuccess());

		// add another node
		final ActorRef node20 = system.actorOf(NodeActor.join(20, storagePath, node10.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node20);

		// write a key
		final CommandResult write2 = TestUtilities.executeCommand(system, node20, new UpdateCommand(1, "ciao"));
		assertTrue(write2.isSuccess());

		// TODO: remove when project is finished
		TestUtilities.waitSomeTime(system);

		// make node 2 leave
		final CommandResult leave1 = TestUtilities.executeCommand(system, node10, new LeaveCommand());
		assertTrue(leave1.isSuccess());

		// read key, should be still in the system
		final CommandResult read3 = TestUtilities.executeCommand(system, node20, new ReadCommand(1));
		assertTrue(read3.isSuccess());
		assertEquals("ciao", read3.getResult());

		// add other 2 nodes
		final ActorRef node30 = system.actorOf(NodeActor.join(30, storagePath, node20.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node30);
		final ActorRef node40 = system.actorOf(NodeActor.join(40, storagePath, node20.path().toSerializationFormat(),
			READ_QUORUM, WRITE_QUORUM, REPLICATION, false));
		TestUtilities.waitForActorToBootstrap(system, node40);

		// leave 20, 30
		final CommandResult leave2 = TestUtilities.executeCommand(system, node30, new LeaveCommand());
		assertTrue(leave2.isSuccess());
		final CommandResult leave3 = TestUtilities.executeCommand(system, node20, new LeaveCommand());
		assertTrue(leave3.isSuccess());

		// read key, should be still in the system
		final CommandResult read4 = TestUtilities.executeCommand(system, node40, new ReadCommand(1));
		assertTrue(read4.isSuccess());
		assertEquals("ciao", read4.getResult());
	}

}
