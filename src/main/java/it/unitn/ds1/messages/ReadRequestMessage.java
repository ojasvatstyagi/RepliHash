package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Message to request the read of some key. This message is internal to the system.
 * See @{@link ClientReadRequestMessage} for the client to ask a key.
 */
public class ReadRequestMessage implements Serializable {

	// key I am requesting to read
	private final int key;

	public ReadRequestMessage(int key) {
		this.key = key;
	}

	public int getKey() {
		return key;
	}
}
