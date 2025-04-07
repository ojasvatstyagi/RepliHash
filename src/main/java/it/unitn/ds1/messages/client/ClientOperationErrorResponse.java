package it.unitn.ds1.messages.client;

import it.unitn.ds1.messages.BaseMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Message to inform the client that an operation (read or update) request didn't success.
 */
public final class ClientOperationErrorResponse extends BaseMessage {

	// message fields
	private final String message;

	public ClientOperationErrorResponse(int senderID, @NotNull String message) {
		super(senderID);
		this.message = message;
	}


	public String getMessage() {
		return message;
	}
}
