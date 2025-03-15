package it.unitn.ds1;

// TODO: make an Interface?

/**
 * Contains common settings for both the Node and the Client.
 */
public class SystemConstants {

	public static final int REPLICATION = 3;
	public static final int READ_QUORUM = 2;
	public static final int WRITE_QUORUM = 2;
	public static final int QUORUM_TIMEOUT_SECONDS = 3;

	public static final int CLIENT_TIMEOUT_SECONDS = 5;

	/**
	 * Unique name for the Akka application.
	 * Used by Akka to contact the other Nodes of the system.
	 */
	public static final String SYSTEM_NAME = "dsproject";

	/**
	 * Unique name for the Akka Node Actor.
	 * Used by Akka to contact the other Nodes of the system.
	 */
	public static final String ACTOR_NAME = "node";
}
