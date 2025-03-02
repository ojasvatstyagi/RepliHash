package it.unitn.ds1.messages;

/**
 * Message used to ask all the data a Node is responsible for.
 */
public class DataRequestMessage extends BaseMessage {

	public DataRequestMessage(int senderID) {
		super(senderID);
	}

}
