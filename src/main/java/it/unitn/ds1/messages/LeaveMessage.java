package it.unitn.ds1.messages;

/**
 * Message used to communicate that a Node is leaving the system.
 */
public class LeaveMessage extends BaseMessage {

	public LeaveMessage(int senderID) {
		super(senderID);
	}

}
