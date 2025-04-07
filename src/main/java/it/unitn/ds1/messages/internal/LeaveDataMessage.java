package it.unitn.ds1.messages.internal;

import it.unitn.ds1.messages.BaseMessage;
import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Message used to send all my data to some node when I have to leave the network.
 */
public final class LeaveDataMessage extends BaseMessage {

	private final Map<Integer, VersionedItem> records;

	public LeaveDataMessage(int senderID, @NotNull Map<Integer, VersionedItem> records) {
		super(senderID);
		this.records = records;
	}

	public Map<Integer, VersionedItem> getRecords() {
		return records;
	}
}
