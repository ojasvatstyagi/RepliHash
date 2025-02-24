package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Message used to communicate that a Node is leaving the system.
 */
public class LeaveMessage implements Serializable {

	// message fields
	private final int id;

	/**
	 * Leave Message: the Node is leaving the system.
	 *
	 * @param id ID of the Node leaving the system.
	 */
	public LeaveMessage(int id) {
		this.id = id;
	}

	/**
	 * @return The ID of the Node leaving.
	 */
	public int getId() {
		return id;
	}
}
