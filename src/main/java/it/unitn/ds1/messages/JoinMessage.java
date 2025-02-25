package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Message used to communicate that a Node has successfully joined the system.
 */
public class JoinMessage implements Serializable {

	// message fields
	private final int id;

	/**
	 * Join Message: the Node is joining the system.
	 *
	 * @param id ID of the Node joining the system.
	 */
	public JoinMessage(int id) {
		this.id = id;
	}

	/**
	 * @return The ID of the Node joining.
	 */
	public int getId() {
		return id;
	}
}
