package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Message used to require to join the system.
 *
 * @author Davide Pedranz
 */
public class JoinRequestMessage implements Serializable {

	// message fields
	private final int id;

	/**
	 * Join Request Message: require to join the system.
	 *
	 * @param id ID of the Node trying to join the network.
	 */
	public JoinRequestMessage(int id) {
		this.id = id;
	}

	/**
	 * @return The ID of the Node trying to join.
	 */
	public int getId() {
		return id;
	}
}
