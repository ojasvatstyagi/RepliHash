package it.unitn.ds1.node;

import akka.actor.ActorRef;
import it.unitn.ds1.storage.VersionedItem;

import java.util.LinkedList;
import java.util.List;

/**
 * This object is used to collect the responses of some read.
 */
@SuppressWarnings("WeakerAccess")
public final class ReadRequestStatus {

	// internal variables
	private final int key;
	private final List<VersionedItem> replies;
	private final ActorRef sender;
	private final int quorum;
	private int nullVotes;

	public ReadRequestStatus(int key, ActorRef sender, int quorum) {
		assert quorum > 0;

		this.key = key;
		this.replies = new LinkedList<>();
		this.sender = sender;
		this.quorum = quorum;
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

	public String getLatestValue() {
		if (!isQuorumReached()) {
			throw new IllegalStateException("Please make sure the quorum is reached before getting the value");
		}
		return this.replies.stream()
			.sorted((o1, o2) -> o2.getVersion() - o1.getVersion())
			.findFirst()
			.orElse(new VersionedItem(null, Integer.MAX_VALUE))
			.getValue();
	}

	public ActorRef getSender() {
		return sender;
	}
}
