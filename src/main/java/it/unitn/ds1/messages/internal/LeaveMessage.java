package it.unitn.ds1.messages.internal;

import it.unitn.ds1.messages.BaseMessage;

/**
 * Message used to communicate that a Node is leaving the system.
 */
public final class LeaveMessage extends BaseMessage {

	public LeaveMessage(int senderID) {
		super(senderID);
	}

}
