package it.unitn.ds1.messages.internal;

import it.unitn.ds1.messages.BaseMessage;

/**
 * Message used to ask all the data a Node is responsible for.
 */
public final class DataRequestMessage extends BaseMessage {

	public DataRequestMessage(int senderID) {
		super(senderID);
	}

}
