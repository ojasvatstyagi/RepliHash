package it.unitn.ds1.node;

import akka.actor.ActorRef;
import org.jetbrains.annotations.NotNull;
import it.unitn.ds1.node.HashUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;

/**
 * Represent the topology of the system.
 * Contains utilities to individuate the nodes to interrogate for a given function.
 * Eg. next node in the ring
 */
final class Ring {

	// configuration
	private final int replication;

	// identifier of the node is using this object
	private final int myID;

	// Internal variable used to keep track of the other nodes in the system.
	// NB: this map contains also the node that instantiated this object!
	private final Map<Integer, ActorRef> nodes;

	/**
	 * Create a new empty ring for a node.
	 *
	 * @param replication Replication factor.
	 * @param myID        ID of the node.
	 */
	Ring(int replication, int myID) {
		this.replication = replication;
		this.myID = myID;
		this.nodes = new HashMap<>();
	}

	@NotNull
	private static Set<Integer> computeResponsibleForKey(int hashedKey, Set<Integer> nodes, int replication) {
		return nodes.stream()
				.sorted((o1, o2) -> {
					if (o1 >= hashedKey && o2 >= hashedKey) return o1 - o2;
					if (o1 >= hashedKey && o2 < hashedKey) return -1;
					if (o1 < hashedKey && o2 >= hashedKey) return +1;
					else return o1 - o2;
				})
				.limit(replication)
				.collect(Collectors.toCollection(LinkedHashSet::new)); // preserve order
	}

	/**
	 * Return the IDs responsible for the given key.
	 *
	 * @param key Key.
	 * @return Set of responsible IDs.
	 */
	@NotNull
	Set<Integer> responsibleForKey(int key) {
		int hashedKey = HashUtil.hash(key); // <-- hash the key before lookup
		return computeResponsibleForKey(hashedKey, this.nodes.keySet(), this.replication);
	}

	/**
	 * Return the IDs responsible for a given key after the current node leaves.
	 *
	 * @param key Key.
	 * @return Set of new responsible IDs.
	 */
	@NotNull
	Set<Integer> nextResponsibleReplicasForLeaving(int key) {
		int hashedKey = HashUtil.hash(key); // <-- hash the key before lookup
		final Set<Integer> nodesExceptMe = this.nodes.keySet().stream()
				.filter(id -> id != this.myID)
				.collect(Collectors.toSet());
		return computeResponsibleForKey(hashedKey, nodesExceptMe, this.replication);
	}

	/**
	 * @return The ID of the next node in the ring.
	 */
	@NotNull
	Integer nextIDInTheRing() {
		return this.nodes.keySet().stream()
			.filter(key -> key > myID)
			.findFirst()
			.orElse(Collections.min(nodes.keySet()));
	}

	/**
	 * @return The next node in the ring.
	 */
	@NotNull
	ActorRef nextNodeInTheRing() {
		return this.nodes.get(this.nextIDInTheRing());
	}

	/**
	 * Return the node with the given ID.
	 *
	 * @param id ID of the node.
	 * @return Actor reference to the node.
	 */
	@NotNull
	ActorRef getNode(int id) {
		return this.nodes.get(id);
	}

	/**
	 * Return all the nodes in the system.
	 *
	 * @return Map of ID -> Actor reference.
	 */
	@NotNull
	Map<Integer, ActorRef> getNodes() {
		return this.nodes;
	}

	/**
	 * Return the set of IDs.
	 *
	 * @return IDs of all nodes.
	 */
	@NotNull
	Set<Integer> getNodeIDs() {
		return this.nodes.keySet();
	}

	/**
	 * @return The size of the ring.
	 */
	int size() {
		return this.nodes.size();
	}

	/**
	 * Add a new node to the system.
	 *
	 * @param id       ID of the node.
	 * @param actorRef Actor reference to the node.
	 */
	void addNode(int id, ActorRef actorRef) {
		this.nodes.put(id, actorRef);
	}

	/**
	 * Add a set of nodes to the ring.
	 *
	 * @param nodes Set of nodes.
	 */
	void addNodes(Map<Integer, ActorRef> nodes) {
		this.nodes.putAll(nodes);
	}

	/**
	 * Remove a node from the system.
	 *
	 * @param nodeID ID of the nodo to remove.
	 */
	void removeNode(int nodeID) {
		this.nodes.remove(nodeID);
	}
}
