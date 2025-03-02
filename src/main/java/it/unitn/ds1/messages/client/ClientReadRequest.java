package it.unitn.ds1.messages.client;

import java.io.Serializable;

/**
 * Message to request the read of some key. This message used by the client.
 */
public class ClientReadRequest implements Serializable {

	// message fields
	private final int key;

	public ClientReadRequest(int key) {
		this.key = key;
	}

	/**
	 * @return The key to read from the system.
	 */
	public int getKey() {
		return key;
	}
}
