package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Message to request the read of some key. This message used by the client.
 */
public class ClientReadRequestMessage implements Serializable {

	// key I am requesting to read
	private final int key;

	public ClientReadRequestMessage(int key) {
		this.key = key;
	}

	public int getKey() {
		return key;
	}
}
