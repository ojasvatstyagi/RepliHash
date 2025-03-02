package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * This base message include the ID of the sender
 * for debugging convenience.
 */
public abstract class BaseMessage implements Serializable {

	// message fields
	private final int senderID;

	/**
	 * Create a new instance of the message.
	 *
	 * @param senderID ID of the sender node.
	 */
	public BaseMessage(int senderID) {
		this.senderID = senderID;
	}

	/**
	 * @return ID of the sender node.
	 */
	public int getSenderID() {
		return senderID;
	}
}
