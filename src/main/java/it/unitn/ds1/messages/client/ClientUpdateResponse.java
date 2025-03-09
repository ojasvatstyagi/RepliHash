package it.unitn.ds1.messages.client;

import it.unitn.ds1.messages.BaseMessage;
import it.unitn.ds1.storage.VersionedItem;

/**
 * Message to reply to a read command.
 */
public class ClientUpdateResponse extends BaseMessage {

	// message fields
	private final int key;
	private final VersionedItem versionedItem;

	public ClientUpdateResponse(int senderID, int key, VersionedItem versionedItem) {
		super(senderID);
		this.key = key;
		this.versionedItem = versionedItem;
	}

	/**
	 * @return The key to updated by the system.
	 */
	public int getKey() {
		return key;
	}

	/**
	 * @return The value and the new version number of the record updated by the system.
	 */
	public VersionedItem getVersionedItem() {
		return versionedItem;
	}
}
