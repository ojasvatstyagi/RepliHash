package it.unitn.ds1.client;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import it.unitn.ds1.SystemConstants;

import java.util.concurrent.TimeoutException;

/**
 * Base class to implement the Clients commands.
 * Bootstrap the Akka system in order to execute just one command.
 * Commands that extend this class can be used inside a CLI.
 */
@SuppressWarnings("WeakerAccess")
abstract class BaseCommand {

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
	protected BaseCommand(String ip, String port) {

		// initialize Akka
		final Config config = ConfigFactory.load();
		this.system = ActorSystem.create(SystemConstants.SYSTEM_NAME, config);

		// setup the logger
		this.logger = Logging.getLogger(system, BaseCommand.class);

		// construct reference to the remote actor
		this.remote = String.format("akka.tcp://%s@%s:%s/user/%s", SystemConstants.SYSTEM_NAME, ip, port, SystemConstants.ACTOR_NAME);
	}

	/**
	 * @return Return the URL used to contact the coordinator for this request.
	 */
	protected final String getRemote() {
		return remote;
	}

	/**
	 * Run the command implemented by this class.
	 */
	public final void execute() {

		// connect to the remote actor
		final ActorSelection targetActor = system.actorSelection(remote);

		// execute the command
		try {
			command(targetActor, logger);
		} catch (TimeoutException e) {
			logger.error("[CLIENT] Timeout error: the node did not reply");
		} catch (Exception e) {
			logger.error(e, "[CLIENT] The command failed with the following error: \"{}\"", e.getMessage());
		}

		// after the command, exit
		system.terminate();
	}

	/**
	 * This method is executed after Akka bootstrap.
	 * Exceptions are caught and handled properly.
	 *
	 * @param targetActor A reference to the actor target of the command.
	 * @param logger      A ready to use logger.
	 * @throws Exception The class that implements this method should
	 *                   throw an Exception if the command fails for some reason.
	 */
	protected abstract void command(ActorSelection targetActor, LoggingAdapter logger) throws Exception;
}
