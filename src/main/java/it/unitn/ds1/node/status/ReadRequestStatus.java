package it.unitn.ds1.node.status;

import akka.actor.ActorRef;
import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * This object is used to collect the responses of some read.
 * The coordinator store an instance of this object in memory until the request is served (or times-out).
 * When a quorum is reached, this object computes the right value to return to the client.
 */
public final class ReadRequestStatus {

	// internal variables
	private final int key;
	private final ActorRef sender;
	private final int quorum;

	// store the votes
	private final List<VersionedItem> replies;
	private int nullVotes;

	/**
	 * Create a new instance of a read status.
	 *
	 * @param key    Key that was requested.
	 * @param sender Actor that requested this read. Used to reply.
	 * @param quorum Quorum needed for the read operation.
	 */
	public ReadRequestStatus(int key, ActorRef sender, int quorum) {
		assert quorum > 0;

		this.key = key;
		this.replies = new LinkedList<>();
		this.sender = sender;
		this.quorum = quorum;
		this.nullVotes = 0;
	}

	/**
	 * @return Return the key that was requested.
	 */
	public int getKey() {
		return this.key;
	}

	/**
	 * @return Return an Akka reference to the Actor that requested the read.
	 * This is used to send the reply when the quorum is reached.
	 */
	public ActorRef getSender() {
		return this.sender;
	}

	/**
	 * Add a new vote, i.e. the value stored on a node responsible for the requested key.
	 * The votes are used, when the quorum is reached, to get the most recent value for the key.
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
	 * Compute the most recent value for the key. If all votes were null, return null.
	 *
	 * @return Most recent value for the key, if any. Otherwise null.
	 */
	@Nullable
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

}
