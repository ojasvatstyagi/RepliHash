package it.unitn.ds1.messages.internal;

import it.unitn.ds1.messages.BaseMessage;

/**
 * Message used to communicate that a Node has successfully joined the system.
 */
public final class JoinMessage extends BaseMessage {

	public JoinMessage(int senderID) {
		super(senderID);
	}

}
