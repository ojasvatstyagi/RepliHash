package it.unitn.ds1;

/**
 * Contains common settings for both the Node and the Client.
 */
public final class SystemConstants {

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


	/**
	 * Timeout that a client waits before giving up
	 * when requesting something to a node.
	 */
	public static final int CLIENT_TIMEOUT_SECONDS = 10;

	/**
	 * Timeout that internal nodes wait before replying to
	 * the client that an operation failed.
	 */
	public static final int QUORUM_TIMEOUT_SECONDS = 3;


	/**
	 * Replication factor. Each key will be replicated on N nodes.
	 */
	static final int REPLICATION = 3;

	/**
	 * Read quorum: in order to get a ket, the coordinator must
	 * contact at least this number of nodes.
	 */
	static final int READ_QUORUM = 2;

	/**
	 * Write quorum: in order to update a key, the coordinator must
	 * contact at lead this number of nodes.
	 */
	static final int WRITE_QUORUM = 2;
}
