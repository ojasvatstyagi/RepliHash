package it.unitn.ds1.messages;

/**
 * Message to acknowledge the write of some key. This message is internal to the system.
 */
public class WriteResponse extends BaseMessage {

	// message fields
	private final int requestID;

	public WriteResponse(int senderID, int requestID) {
		super(senderID);
		this.requestID = requestID;
	}

	/**
	 * @return The id of the request. Id is generated from node who starts the request.
	 */
	public int getRequestID() {
		return requestID;
	}
}
