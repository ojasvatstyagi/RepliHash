package it.unitn.ds1.messages.internal;

import akka.actor.ActorRef;
import it.unitn.ds1.messages.BaseMessage;

import java.util.Collections;
import java.util.Map;

/**
 * Message used to transmit the list of Nodes in the system.
 * Used during the leave phase.
 */
public final class NodesListMessage extends BaseMessage {

	// message fields
	private final Map<Integer, ActorRef> nodes;

	/**
	 * Nodes List Message: transmit the Nodes present in the system.
	 *
	 * @param senderID ID of the sender node.
	 * @param nodes    Nodes in the system. Note that the provided map will be
	 *                 copied and made unmodifiable before sending it in the message.
	 */
	public NodesListMessage(int senderID, Map<Integer, ActorRef> nodes) {
		super(senderID);
		this.nodes = Collections.unmodifiableMap(nodes);
	}

	/**
	 * @return Nodes in the system, as a Map: id -> node reference.
	 */
	public Map<Integer, ActorRef> getNodes() {
		return nodes;
	}
}
