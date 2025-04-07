package it.unitn.ds1.messages.internal;

import it.unitn.ds1.messages.BaseMessage;
import it.unitn.ds1.messages.client.ClientReadRequest;
import it.unitn.ds1.storage.VersionedItem;
import org.jetbrains.annotations.Nullable;

/**
 * Message to request the read of some key. This message is internal to the system.
 * See @{@link ClientReadRequest} for the client to ask a key.
 */
public final class ReadResponse extends BaseMessage {

	// TODO: request ID will get out of range at some point

	// message fields
	private final int requestID;
	private final int key;
	private final VersionedItem item;

	public ReadResponse(int senderID, int requestID, int key, @Nullable VersionedItem item) {
		super(senderID);
		this.requestID = requestID;
		this.key = key;
		this.item = item;
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

	/**
	 * @return value and version associated with the key contained in this response.
	 */
	@Nullable
	public VersionedItem getValue() {
		return item;
	}
}
