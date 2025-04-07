package it.unitn.ds1.messages.internal;

import it.unitn.ds1.messages.BaseMessage;

/**
 * Message used to communicate that a Node has successfully joined the system
 * after a crash. This is used to update the Akka references.
 */
public final class ReJoinMessage extends BaseMessage {

	public ReJoinMessage(int senderID) {
		super(senderID);
	}
}
