package it.unitn.ds1.messages;

import it.unitn.ds1.messages.client.ClientUpdateRequest;
import it.unitn.ds1.storage.VersionedItem;

/**
 * Message to request the write of some record. This message is internal to the system.
 * See @{@link ClientUpdateRequest} for the client to update a record.
 */
public class WriteRequest extends BaseMessage {

	// message fields
	private final int requestID;
	private final int key;
	private final VersionedItem versionedItem;

	public WriteRequest(int senderID, int requestID, int key, VersionedItem versionedItem) {
		super(senderID);
		this.requestID = requestID;
		this.key = key;
		this.versionedItem = versionedItem;
	}

	// TODO: doc
	public int getRequestID() {
		return requestID;
	}

	/**
	 * @return The key to write from the system.
	 */
	public int getKey() {
		return key;
	}

	/**
	 * @return The record to write in the system.
	 */
	public VersionedItem getVersionedItem() {
		return versionedItem;
	}
}
