package it.unitn.ds1;

import com.typesafe.config.ConfigFactory;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import static org.junit.Assert.assertEquals;

/**
 * End-2-End test: test directly the command lines.
 */
public final class End2EndTest {

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

	// utility to start a new node
	private static void executeNode(int id, String commandLine) throws InterruptedException {
		System.setProperty("NODE_ID", String.valueOf(id));
		System.setProperty("PORT", String.valueOf(10000 + id));
		ConfigFactory.invalidateCaches();
		Node.main(commandLine.split(" "));
		Thread.sleep(200);
	}

	// utility to execute a command
	private static void executeCommand(String commandLine) throws InterruptedException {
		System.setProperty("NODE_ID", "0");
		System.setProperty("PORT", "10000");
		ConfigFactory.invalidateCaches();
		Client.main(commandLine.split(" "));
		Thread.sleep(200);
	}

	@BeforeClass
	public static void check() {
		// make sure the system constants are the right one... otherwise this test will fail
		assertEquals(3, SystemConstants.REPLICATION);
		assertEquals(2, SystemConstants.READ_QUORUM);
		assertEquals(2, SystemConstants.WRITE_QUORUM);
	}

	@Test
	public void bootstrapAndLeaveCommand() throws InterruptedException {
		exit.expectSystemExitWithStatus(0);
		executeNode(10, "bootstrap");
		executeNode(20, "join 127.0.0.1 10010");
		executeCommand("127.0.0.1 10010 leave");
	}

	@Test
	public void noReadQuorum() throws InterruptedException {
		exit.expectSystemExitWithStatus(1);
		executeNode(1000, "bootstrap");
		executeCommand("127.0.0.1 11000 read 2");
	}

	@Test
	public void noWriteQuorum() throws InterruptedException {
		exit.expectSystemExitWithStatus(1);
		executeNode(2000, "bootstrap");
		executeCommand("127.0.0.1 12000 write 2 23");
	}

	@Test
	public void commandTimeout() throws InterruptedException {
		exit.expectSystemExitWithStatus(2);
		executeCommand("127.0.0.1 50000 read 2");
	}
}
