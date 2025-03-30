package it.unitn.ds1.node;

import akka.actor.ActorRef;
import it.unitn.ds1.storage.VersionedItem;

import java.util.HashSet;
import java.util.Set;

/**
 * This object is used to collect the responses of some write request sent from a node.
 */
@SuppressWarnings("WeakerAccess")
public final class WriteResponseStatus {

	// TODO: add also nodes that were responsible for this

	// internal variables
	private final int key;
	private final VersionedItem versionedItem;
	// nodes who confirmed the write
	private final Set<Integer> nodesAcks;
	private final ActorRef sender;
	private final int replicationFactor;

	public WriteResponseStatus(int key, VersionedItem newValue, ActorRef sender, int replicationFactor) {
		assert replicationFactor > 0;
		this.key = key;
		this.versionedItem = newValue;
		this.nodesAcks = new HashSet<>();
		this.sender = sender;
		this.replicationFactor = replicationFactor;
	}

	public void addAck(int nodeId) {
		this.nodesAcks.add(nodeId);
	}

	public boolean hasEveryoneAck() {
		return nodesAcks.size() >= replicationFactor;
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
