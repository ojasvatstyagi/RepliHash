package it.unitn.ds1.messages;

import it.unitn.ds1.messages.client.ClientReadRequest;

/**
 * Message to request the read of some key. This message is internal to the system.
 * See @{@link ClientReadRequest} for the client to ask a key.
 */
public class ReadRequestMessage extends BaseMessage {

	// message fields
	private final int key;

	public ReadRequestMessage(int senderID, int key) {
		super(senderID);
		this.key = key;
	}

	/**
	 * @return The key to read from the system.
	 */
	public int getKey() {
		return key;
	}
}
