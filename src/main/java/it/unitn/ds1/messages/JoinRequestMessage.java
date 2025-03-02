package it.unitn.ds1.messages;

/**
 * Message used to require to join the system.
 */
public class JoinRequestMessage extends BaseMessage {

	public JoinRequestMessage(int senderID) {
		super(senderID);
	}

}
