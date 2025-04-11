package it.unitn.ds1.node.status;

import akka.actor.ActorRef;
import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * This object is used to collect the responses of some write request sent from a node.
 * The coordinator store an instance of this object in memory until the request is served (or times-out).
 * It makes sure that all writes are acknowledged before replying to the client.
 */
public final class UpdateResponseStatus {

	// TODO: add also nodes that were responsible for this
	// probably not...

	// internal variables
	private final int key;
	private final VersionedItem newValue;
	private final ActorRef sender;
	private final int quorum;

	// nodes that confirmed the write
	private final Set<Integer> acks;

	public UpdateResponseStatus(int key, @NotNull VersionedItem newValue, ActorRef sender, int readQuorum, int writeQuorum) {
		assert readQuorum > 0 && writeQuorum > 0;

		this.key = key;
		this.newValue = newValue;
		this.sender = sender;
		this.quorum = Math.max(readQuorum, writeQuorum);
		this.acks = new HashSet<>();
	}

	/**
	 * @return Return the key that was requested.
	 */
	public int getKey() {
		return this.key;
	}

	/**
	 * @return Return the value that is written in the system.
	 */
	public VersionedItem getVersionedItem() {
		return this.newValue;
	}

	/**
	 * @return Return an Akka reference to the Actor that requested the update.
	 * This is used to send the reply when the quorum is reached.
	 */
	public ActorRef getSender() {
		return this.sender;
	}

	/**
	 * Add an acknowledgement from some node.
	 *
	 * @param nodeID ID of the node that acknowledged the update.
	 */
	public void addAck(int nodeID) {
		this.acks.add(nodeID);
	}

	/**
	 * Check if the quorum of acknowledgements required is reached.
	 *
	 * @return True if the quorum is reached, false otherwise.
	 */
	public boolean isAckQuorumReached() {
		return this.acks.size() >= this.quorum;
	}

	/**
	 * @return The IDs of the nodes that acknowledged the update.
	 */
	public Set<Integer> getNodeThatAcknowledged() {
		return this.acks;
	}
}
