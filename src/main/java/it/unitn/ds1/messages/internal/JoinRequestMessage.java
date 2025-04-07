package it.unitn.ds1.messages.internal;

import it.unitn.ds1.messages.BaseMessage;

/**
 * Message used to require to join the system.
 */
public final class JoinRequestMessage extends BaseMessage {

	public JoinRequestMessage(int senderID) {
		super(senderID);
	}

}
