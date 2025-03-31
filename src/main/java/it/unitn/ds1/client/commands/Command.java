package it.unitn.ds1.client.commands;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Each command in the system should implement this interface.
 */
public interface Command {

	/**
	 * This method is executed after Akka bootstrap.
	 * Exceptions are caught and handled properly.
	 *
	 * @param actor  A reference to the actor target of the command.
	 * @param remote The URL used to contact the coordinator for this request.
	 * @param logger A ready to use logger.
	 * @return The result of the command.
	 * @throws Exception The class that implements this method should
	 *                   throw an Exception if the command fails for some reason.
	 */
	@NotNull
	CommandResult run(ActorSelection actor, String remote, LoggingAdapter logger) throws Exception;
}
