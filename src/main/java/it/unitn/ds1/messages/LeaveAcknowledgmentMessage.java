package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Message used to confirm to the client that the Node is leaving the system.
 */
public class LeaveAcknowledgmentMessage implements Serializable {

	// message fields
	private final int id;

	public LeaveAcknowledgmentMessage(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}


}
