package it.unitn.ds1.messages;

import it.unitn.ds1.messages.client.ClientReadRequest;

/**
 * Message to request the read of some key. This message is internal to the system.
 * See @{@link ClientReadRequest} for the client to ask a key.
 */
public class ReadRequest extends BaseMessage {

	// message fields
	private final int requestID;
	private final int key;

	public ReadRequest(int senderID, int requestID, int key) {
		super(senderID);
		this.requestID = requestID;
		this.key = key;
	}

	/**
	 * @return The id of the request. Id is generated from node who starts the request.
	 */
	public int getRequestID() {
		return requestID;
	}

	/**
	 * @return The key to read from the system.
	 */
	public int getKey() {
		return key;
	}
}
