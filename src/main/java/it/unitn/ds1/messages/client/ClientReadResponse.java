package it.unitn.ds1.messages.client;

import it.unitn.ds1.messages.BaseMessage;
import org.jetbrains.annotations.Nullable;

/**
 * Message to reply to a read command.
 */
public class ClientReadResponse extends BaseMessage {

	// message fields
	private final int key;
	private final String value;

	public ClientReadResponse(int senderID, int key, @Nullable String value) {
		super(senderID);
		this.key = key;
		this.value = value;
	}

	/**
	 * @return The key to read from the system.
	 */
	public int getKey() {
		return key;
	}

	/**
	 * @return True if the key was found, false otherwise.
	 */
	public boolean keyFound() {
		return value != null;
	}

	/**
	 * @return Return the value of the key. This can be null if the key is not found.
	 */
	@Nullable
	public String getValue() {
		return value;
	}

}
