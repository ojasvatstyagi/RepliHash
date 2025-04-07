package it.unitn.ds1.node.status;

import akka.actor.ActorRef;
import it.unitn.ds1.storage.VersionedItem;

import java.util.HashSet;
import java.util.Set;

/**
 * This object is used to collect the responses of some write request sent from a node.
 */
public final class WriteResponseStatus {

	// TODO: add also nodes that were responsible for this

	// internal variables
	private final int key;
	private final VersionedItem versionedItem;

	// nodes who confirmed the write
	private final Set<Integer> nodesAcks;
	private final ActorRef sender;
	private final int quorum;

	public WriteResponseStatus(int key, VersionedItem newValue, ActorRef sender, int readQuorum, int writeQuorum) {
		assert readQuorum > 0 && writeQuorum > 0;
		this.key = key;
		this.versionedItem = newValue;
		this.nodesAcks = new HashSet<>();
		this.sender = sender;
		this.quorum = Math.max(readQuorum, writeQuorum);
	}

	public void addAck(int nodeId) {
		this.nodesAcks.add(nodeId);
	}

	public boolean hasAckQuorumReached() {
		return this.nodesAcks.size() >= quorum;
	}

	public int getKey() {
		return key;
	}

	public VersionedItem getVersionedItem() {
		return versionedItem;
	}

	public Set<Integer> getNodeAcksIds() {
		return nodesAcks;
	}

	public ActorRef getSender() {
		return sender;
	}
}
