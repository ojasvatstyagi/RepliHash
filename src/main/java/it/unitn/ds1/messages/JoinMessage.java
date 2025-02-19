package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Message used to require to join the system.
 *
 * @author Davide Pedranz
 */
public class JoinMessage implements Serializable {

	// message fields
	private final int id;

	/**
	 * JoinMessage Message: require to join the system.
	 *
	 * @param id ID of the Node trying to join the network.
	 */
	public JoinMessage(int id) {
		this.id = id;
	}

	/**
	 * @return The ID of the Node trying to join.
	 */
	public int getId() {
		return id;
	}
}
