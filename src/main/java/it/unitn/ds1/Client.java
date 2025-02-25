package it.unitn.ds1;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unitn.ds1.actors.LeaveActor;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * Client.
 */
public class Client {

	/**
	 * Error message to print when the Node is invoked with the wrong parameters.
	 */
	private static final String USAGE = "\n" +
		"Usage: java Client [ip] [port] COMMAND [ARGUMENTS]\n" +
		"\n" +
		"Command line interface for the distributed system.\n" +
		"\n" +
		"Parameters:\n" +
		"   ip         IP of the Node to contact\n" +
		"   port       TCP port of the Node to contact\n" +
		"\n" +
		"Commands:\n" +
		"   read   [key]           Read the value with the given key\n" +
		"   write  [key] [value]   Update the value of the entry with the given key\n" +
		"   leave                  Instruct the Node to leave the system\n";

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
		if (args.length < 3) {
			printHelpAndExit();
		}

		// extract IP and PORT
		final String ip = args[0];
		final String port = args[1];
		if (!validateIPAndPort(ip, port)) {
			System.err.println("Invalid IP address or port");
			printHelpAndExit();
		}

		// extract the command
		final String command = args[2];
		switch (command) {

			// bootstrap a new system
			case "leave": {

				// validate number of arguments
				if (args.length != 3) {
					printHelpAndExit();
				}

				// bootstrap the system
				leave(ip, port);
				break;
			}

			// TODO
			case "read": {
				break;
			}

			// TODO
			case "write": {
				break;
			}

			// command not found
			default: {
				printHelpAndExit();
			}
		}
	}

	/**
	 * Asks to a Node to leave the system.
	 *
	 * @param ip   IP of the Node.
	 * @param port TCP port of the Node.
	 */
	private static void leave(String ip, String port) {
		System.out.println("Leave request for - " + ip + ":" + port);

		// load configuration
		final Config config = ConfigFactory.load();

		// initialize Akka
		final ActorSystem system = ActorSystem.create(SystemConstants.SYSTEM_NAME, config);

		// send the leave message to the Node
		final String remote = String.format("akka.tcp://%s@%s:%s/user/%s",
			SystemConstants.SYSTEM_NAME, ip, port, SystemConstants.ACTOR_NAME);

		system.actorOf(LeaveActor.leave(remote), SystemConstants.ACTOR_NAME);
	}

}
