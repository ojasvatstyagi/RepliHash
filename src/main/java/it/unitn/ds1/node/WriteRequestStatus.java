package it.unitn.ds1.node;

import akka.actor.ActorRef;
import it.unitn.ds1.storage.VersionedItem;

import java.util.LinkedList;
import java.util.List;

/**
 * This object is used to collect the responses of some write request.
 */
@SuppressWarnings("WeakerAccess")
public final class WriteRequestStatus {

	// TODO: add also nodes that were responsible for this

	// internal variables
	private final int key;
	private final String newValue;
	// replies to "read requests"
	// used to collect records to decide new record's version before performing the write
	private final List<VersionedItem> replies;
	private final ActorRef sender;
	private final int quorum;
	private int nullVotes;

	public WriteRequestStatus(int key, String newValue, ActorRef sender, int readQuorum, int writeQuorum) {
		assert readQuorum > 0 && writeQuorum > 0;
		this.key = key;
		this.newValue = newValue;
		this.replies = new LinkedList<>();
		this.sender = sender;
		this.quorum = Math.max(readQuorum, writeQuorum);
		this.nullVotes = 0;
	}

	public void addVote(VersionedItem item) {
		if (item == null) {
			this.nullVotes++;
		} else {
			this.replies.add(item);
		}
	}

	public boolean isQuorumReached() {
		return this.replies.size() + this.nullVotes >= quorum;
	}

	public int getKey() {
		return key;
	}

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

		return new VersionedItem(this.newValue, lastVersion);
	}

	public ActorRef getSender() {
		return sender;
	}
}
