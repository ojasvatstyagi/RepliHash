package it.unitn.ds1.messages.client;

import java.io.Serializable;

/**
 * Message to request the update of some item. This message is used by the client.
 */
public final class ClientUpdateRequest implements Serializable {

	// message fields
	private final int key;
	private final String value;

	public ClientUpdateRequest(int key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * @return The key to update in the system.
	 */
	public int getKey() {
		return key;
	}

	/**
	 * @return The value to update in the system.
	 */
	public String getValue() {
		return value;
	}
}
