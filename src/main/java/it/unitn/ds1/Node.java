package it.unitn.ds1;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unitn.ds1.node.NodeActor;
import it.unitn.ds1.node.HashUtil;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * Entry point to launch a new node.
 */
public final class Node {

	/**
	 * Key used in the configuration file to pass the ID for the Node to launch.
	 */
	private static final String CONFIG_NODE_ID = "node.id";

	/**
	 * Key used in the configuration file to indicate where the node data store should be created.
	 */
	private static final String CONFIG_STORAGE_PATH = "node.storage-path";

	/**
	 * Error message to print when the Node is invoked with the wrong parameters.
	 */
	private static final String USAGE = "\n" +
		"Usage: java Node COMMAND [ip] [port]\n" +
		"\n" +
		"Launch a new Node in the system.\n" +
		"\n" +
		"Commands:\n" +
		"   bootstrap  Instruct the Node to bootstrap a new system (does NOT require ip and port)\n" +
		"   join       Instruct the Node to join the system (for the first time)\n" +
		"   recover    Instruct the Node to do recovery and join again the system after a crash\n" +
		"\n" +
		"Arguments:\n" +
		"   ip         The IP of a remote Node already in the system\n" +
		"   port       The TCP port to use to which the remote Node is listening\n";

	/**
	 * Print an help message and exit.
	 */
	private static void printHelpAndExit() {
		System.err.println(USAGE);
		System.exit(2);
	}


	/**
	 * Validate IP and port.
	 *
	 * @param ip   IP to validate.
	 * @param port Port to validate.
	 * @return True if both IP and port are valid, false otherwise.
	 */
	private static boolean validateIPAndPort(String ip, String port) {
		try {
			final int portAsInteger = Integer.parseInt(port);
			return InetAddressValidator.getInstance().isValid(ip) &&
				portAsInteger >= 1 && portAsInteger <= 65535;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Entry point.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {

		// check the command line arguments
		if (args.length < 1) {
			printHelpAndExit();
		}

		// extract the command
		final String command = args[0];
		switch (command) {

			// bootstrap a new system
			case "bootstrap": {

				// validate number of arguments
				if (args.length != 1) {
					printHelpAndExit();
				}

				// bootstrap the system
				bootstrap();
				break;
			}

			// launch this node and ask it to join an existing system
			case "join": {

				// validate number of arguments
				if (args.length != 3) {
					printHelpAndExit();
				}

				// extract ip and port of the node to contact to join the system
				final String ip = args[1];
				final String port = args[2];
				if (!validateIPAndPort(ip, port)) {
					System.err.println("Invalid IP address or port");
					printHelpAndExit();
				}

				// launch the new Node
				join(ip, port);
				break;
			}

			// launch this node and ask it to perform recovery and join again the system
			case "recover": {

				// validate number of arguments
				if (args.length != 3) {
					printHelpAndExit();
				}

				// extract ip and port of the node to contact to join the system
				final String ip = args[1];
				final String port = args[2];
				if (!validateIPAndPort(ip, port)) {
					System.err.println("Invalid IP address or port");
					printHelpAndExit();
				}

				// launch the new Node
				recover(ip, port);
				break;
			}

			// command not found
			default: {
				printHelpAndExit();
			}
		}
	}

	/**
	 * Launch a new Node which will bootstrap the system.
	 */
	private static void bootstrap() {

		// load configuration
		final Config config = ConfigFactory.load();

		// initialize Akka
		final ActorSystem system = ActorSystem.create(SystemConstants.SYSTEM_NAME, config);

		// create a NodeActor of type "bootstrap" and add it to the system
		final String storagePath = config.getString(CONFIG_STORAGE_PATH);
       // We’ll treat this node’s “raw address” as stored in config or default to localhost:0
       final String rawAddress = config.hasPath("node.address")
           ? config.getString("node.address")
           : "127.0.0.1:" + config.getInt("akka.remote.netty.tcp.port");
       final int hashedId = HashUtil.hash(rawAddress);
       system.actorOf(NodeActor.bootstrap(
           hashedId,            // hashed node ID
           rawAddress,          // the string “ip:port”
           storagePath,
           SystemConstants.READ_QUORUM,
           SystemConstants.WRITE_QUORUM,
           SystemConstants.REPLICATION,
           true
       ), SystemConstants.ACTOR_NAME);
	}

	/**
	 * Launch a new Node to join an existing system for the first time.
	 *
	 * @param ip   IP of a remote Node in the system.
	 * @param port Port of a remote Node in the system.
	 */
	private static void join(String ip, String port) {
    // load config
    final Config config = ConfigFactory.load();

	String selfHost = config.getString("akka.remote.netty.tcp.hostname");
	int    selfPort = config.getInt("akka.remote.netty.tcp.port");
	String storagePath = config.getString(CONFIG_STORAGE_PATH);

	String rawAddress = selfHost + ":" + selfPort;
	int    hashedId   = HashUtil.hash(rawAddress);

    // initialize Akka
    final ActorSystem system = ActorSystem.create(SystemConstants.SYSTEM_NAME, config);

    // THIS is the corrected line:
    String remote = String.format(
		"akka.tcp://%s@%s:%s/user/%s",
		SystemConstants.SYSTEM_NAME,  // dsproject
		ip,                           // bootstrap IP (method arg)
		port,                         // bootstrap port (method arg)
		SystemConstants.ACTOR_NAME    // node
	);

    // spawn the NodeActor
    system.actorOf(NodeActor.join(
        hashedId,
        rawAddress,
        storagePath,
        remote,
        SystemConstants.READ_QUORUM,
        SystemConstants.WRITE_QUORUM,
        SystemConstants.REPLICATION,
        true
    ), SystemConstants.ACTOR_NAME);
}

private static void recover(String ip, String port) {
    // load configuration
    final Config config = ConfigFactory.load();

	String selfHost = config.getString("akka.remote.netty.tcp.hostname");
	int    selfPort = config.getInt("akka.remote.netty.tcp.port");
	String storagePath = config.getString(CONFIG_STORAGE_PATH);

	String rawAddress = selfHost + ":" + selfPort;
	int    hashedId   = HashUtil.hash(rawAddress);

    // initialize Akka
    final ActorSystem system = ActorSystem.create(SystemConstants.SYSTEM_NAME, config);

    // build the Akka remote path of an existing node
    String remote = String.format(
		"akka.tcp://%s@%s:%s/user/%s",
		SystemConstants.SYSTEM_NAME,  // dsproject
		ip,                           // bootstrap IP (method arg)
		port,                         // bootstrap port (method arg)
		SystemConstants.ACTOR_NAME    // node
	);

	system.actorOf(NodeActor.join(
		hashedId,      // now unique per node
		rawAddress,    // e.g. "127.0.0.1:20020"
		storagePath,
		remote,
		SystemConstants.READ_QUORUM,
		SystemConstants.WRITE_QUORUM,
		SystemConstants.REPLICATION,
		true
	), SystemConstants.ACTOR_NAME);
}
}