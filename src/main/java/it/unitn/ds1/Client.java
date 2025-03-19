package it.unitn.ds1;

import it.unitn.ds1.client.LeaveCommand;
import it.unitn.ds1.client.ReadCommand;
import it.unitn.ds1.client.UpdateCommand;
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
	 * Try to parse some string to an integer.
	 * On failure, output an error and exit.
	 *
	 * @param raw String to parse.
	 * @return Integer (if found).
	 */
	private static int parseIntOrExit(String raw) {
		try {
			return Integer.valueOf(raw);
		} catch (NumberFormatException e) {
			System.err.println("Key must be an Integer.");
			printHelpAndExit();
			throw new RuntimeException("Should not be here...");
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

				// ask the node to leave
				leave(ip, port);
				break;
			}

			// read a key from the system
			case "read": {

				// validate number of arguments
				if (args.length != 4) {
					printHelpAndExit();
				}

				// extract the key
				final int key = parseIntOrExit(args[3]);

				// ask the value for the key
				read(ip, port, key);
				break;
			}

			case "write": {

				// validate number of arguments
				if (args.length != 5) {
					printHelpAndExit();
				}

				// extract the key
				final int key = parseIntOrExit(args[3]);
				final String value = args[4];

				// validate value
				if (value.contains(" ")) {
					System.err.println("Values cannot contain spaces. Please try again.");
					printHelpAndExit();
				}

				// ask record update
				update(ip, port, key, value);
				break;
			}

			// command not found
			default: {
				printHelpAndExit();
			}
		}
	}

	private static void leave(String ip, String port) {
		new LeaveCommand(ip, port).execute();
	}

	private static void read(String ip, String port, int key) {
		new ReadCommand(ip, port, key).execute();
	}

	private static void update(String ip, String port, int key, String value) {
		new UpdateCommand(ip, port, key, value).execute();
	}

}
