package it.unitn.ds1.messages;

/**
 * Message used to communicate that a Node has successfully joined the system.
 */
public class JoinMessage extends BaseMessage {

	public JoinMessage(int senderID) {
		super(senderID);
	}

}
