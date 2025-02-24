package it.unitn.ds1.messages;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Message used to transmit the list of Nodes in the system.
 * Used during the leave phase.
 *
 * @author Davide Pedranz
 */
public class NodesListMessage implements Serializable {

	// message fields
	private final Map<Integer, ActorRef> nodes;

	/**
	 * Nodes List Message: transmit the Nodes present in the system.
	 *
	 * @param nodes Nodes in the system. Note that the provided map will be
	 *              copied and made unmodifiable before sending it in the message.
	 */
	public NodesListMessage(final Map<Integer, ActorRef> nodes) {
		this.nodes = Collections.unmodifiableMap(nodes);
	}

	/**
	 * @return Nodes in the system, as a Map: id -> node reference.
	 */
	public Map<Integer, ActorRef> getNodes() {
		return nodes;
	}
}
