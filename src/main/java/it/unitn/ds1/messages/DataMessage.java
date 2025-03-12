package it.unitn.ds1.messages;

import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Message used to send all my data to some node.
 */
public class DataMessage extends BaseMessage {

	private final Map<Integer, VersionedItem> records;

	public DataMessage(int senderID, @NotNull Map<Integer, VersionedItem> records) {
		super(senderID);
		this.records = records;
	}

	public Map<Integer, VersionedItem> getRecords() {
		return records;
	}
}
