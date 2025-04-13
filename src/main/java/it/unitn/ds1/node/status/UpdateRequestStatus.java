package it.unitn.ds1.node.status;

import akka.actor.ActorRef;
import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * This object is used to collect the responses of some update request.
 * The coordinator store an instance of this object in memory until the request is served (or times-out).
 * When a quorum is reached, this object computes the new version and value for to write
 * on all nodes responsible for the key.
 */
public final class UpdateRequestStatus {

	// internal variables
	private final int key;
	private final String newValue;
	private final ActorRef sender;
	private final int quorum;

	// replies to the "read request"...
	// used to collect records to decide new record's version before performing the write
	private final List<VersionedItem> replies;
	private int nullVotes;

	/**
	 * Create a new instance of a update request status.
	 * NB: this object is used only for the initial phase of collecting the latest version.
	 * The following write is coordinated using a @{@link UpdateResponseStatus} instance.
	 *
	 * @param key         Key to update.
	 * @param newValue    New value for the key.
	 * @param sender      Actor that requested the update.
	 * @param readQuorum  Read quorum.
	 * @param writeQuorum Write quorum.
	 */
	public UpdateRequestStatus(int key, @NotNull String newValue, ActorRef sender, int readQuorum, int writeQuorum) {
		assert readQuorum > 0 && writeQuorum > 0;

		this.key = key;
		this.newValue = newValue;
		this.sender = sender;
		this.quorum = Math.max(readQuorum, writeQuorum);

		// collect the votes
		this.replies = new LinkedList<>();
		this.nullVotes = 0;
	}

	/**
	 * @return Return the key that was requested.
	 */
	public int getKey() {
		return this.key;
	}

	/**
	 * @return Return an Akka reference to the Actor that requested the update.
	 * This is used to send the reply when the quorum is reached.
	 */
	public ActorRef getSender() {
		return this.sender;
	}

	/**
	 * Add a new vote, i.e. the value stored on a node responsible for the requested key.
	 * The votes are used, when the quorum is reached, to get the most recent value for the key,
	 * in order to get the correct new version for the update operation.
	 * NB: Some votes may be null, for example when the client request a key which is not in the system.
	 * In this case, the vote is counted but not considered to get the most recent value.
	 *
	 * @param item The value and version of the key stored on some node.
	 */
	public void addVote(VersionedItem item) {
		if (item == null) {
			this.nullVotes++;
		} else {
			this.replies.add(item);
		}
	}

	/**
	 * Check if the quorum if reached.
	 *
	 * @return True if the quorum is reached, false otherwise.
	 */
	public boolean isQuorumReached() {
		return this.replies.size() + this.nullVotes >= this.quorum;
	}

	/**
	 * Compute the value that should be used for the new key.
	 * Only not null votes are considered. If all votes are null (i.e. first write for this key),
	 * version 1 is returned.
	 *
	 * @return Most recent value for the key, on 1 if the key was never in the system.
	 */
	public VersionedItem getUpdatedRecord() {
		if (!isQuorumReached()) {
			throw new IllegalStateException("Please make sure the quorum is reached before getting the new record");
		}

		// calculate new version
		int lastVersion = 0;
		for (VersionedItem record : this.replies) {
			lastVersion = (record.getVersion() > lastVersion) ? (record.getVersion()) : (lastVersion);
		}
		lastVersion++;

		// return new version to use
		return new VersionedItem(this.newValue, lastVersion);
	}

}
