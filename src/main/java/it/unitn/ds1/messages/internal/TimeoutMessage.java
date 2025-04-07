package it.unitn.ds1.messages.internal;

import it.unitn.ds1.messages.BaseMessage;

/**
 * Message used to communicate that a timeout for a certain request has been reached.
 */
public final class TimeoutMessage extends BaseMessage {

	private final int requestID;

	public TimeoutMessage(int senderID, int requestID) {
		super(senderID);
		this.requestID = requestID;
	}

	public int getRequestID() {
		return requestID;
	}
}
