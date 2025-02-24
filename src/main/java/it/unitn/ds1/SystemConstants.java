package it.unitn.ds1;

/**
 * Contains common settings for both the Node and the Client.
 *
 * @author Davide Pedranz
 */
class SystemConstants {

	/**
	 * Unique name for the Akka application.
	 * Used by Akka to contact the other Nodes of the system.
	 */
	static final String SYSTEM_NAME = "dsproject";

	/**
	 * Unique name for the Akka Node Actor.
	 * Used by Akka to contact the other Nodes of the system.
	 */
	static final String ACTOR_NAME = "node";
}
