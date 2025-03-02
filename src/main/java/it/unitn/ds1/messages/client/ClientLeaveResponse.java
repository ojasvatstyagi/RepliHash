package it.unitn.ds1.messages.client;

import it.unitn.ds1.messages.BaseMessage;

/**
 * Message used to confirm to the client that the node is leaving the system.
 */
public final class ClientLeaveResponse extends BaseMessage {

	public ClientLeaveResponse(int id) {
		super(id);
	}

}
