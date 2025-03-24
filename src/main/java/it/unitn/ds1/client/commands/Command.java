package it.unitn.ds1.client.commands;

import akka.actor.ActorSelection;
import akka.event.LoggingAdapter;

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
	 * @return True if the command was successful, false otherwise.
	 * @throws Exception The class that implements this method should
	 *                   throw an Exception if the command fails for some reason.
	 */
	boolean run(ActorSelection actor, String remote, LoggingAdapter logger) throws Exception;
}
