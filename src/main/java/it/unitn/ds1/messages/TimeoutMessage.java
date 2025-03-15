package it.unitn.ds1.messages;

/**
 * Message used to communicate that a timeout for a certain request has been reached.
 */
public class TimeoutMessage extends BaseMessage {

	private final int requestID;

	public TimeoutMessage(int senderID, int requestID) {
		super(senderID);
		this.requestID = requestID;
	}

	public int getRequestID() {
		return requestID;
	}
}
