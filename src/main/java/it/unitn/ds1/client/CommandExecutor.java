package it.unitn.ds1.client;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unitn.ds1.SystemConstants;
import it.unitn.ds1.client.commands.Command;
import it.unitn.ds1.client.commands.CommandResult;

import java.util.concurrent.TimeoutException;

public final class CommandExecutor {

	// internal hidden variables
	private final ActorSystem system;
	private final String remote;
	private final LoggingAdapter logger;

	/**
	 * Setup the environment to execute the command.
	 *
	 * @param ip   IP for the target Actor.
	 * @param port TCP port of the target Actor.
	 */
	public CommandExecutor(String ip, String port) {

		// initialize Akka
		final Config config = ConfigFactory.load();
		this.system = ActorSystem.create(SystemConstants.SYSTEM_NAME, config);

		// setup the logger
		this.logger = Logging.getLogger(system, CommandExecutor.class);

		// construct reference to the remote actor
		this.remote = String.format("akka.tcp://%s@%s:%s/user/%s", SystemConstants.SYSTEM_NAME, ip, port, SystemConstants.ACTOR_NAME);
	}

	/**
	 * Run the command implemented by this class.
	 */
	public final int execute(Command command) {

		// connect to the remote actor
		final ActorSelection targetActor = system.actorSelection(remote);

		// execute the command
		int result;
		try {
			final CommandResult commandResult = command.run(targetActor, remote, Logging.getLogger(system, Command.class));
			result = commandResult.isSuccess() ? 0 : 1;
		} catch (TimeoutException e) {
			logger.error("[CLIENT] Timeout error: the node did not reply");
			result = 55;
		} catch (Exception e) {
			logger.error(e, "[CLIENT] The command failed with the following error: \"{}\"", e.getMessage());
			result = 55;
		}

		// after the command, exit
		system.terminate();

		// exit code
		return result;
	}

}
