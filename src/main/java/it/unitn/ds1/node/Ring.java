package it.unitn.ds1.node;

import akka.actor.ActorRef;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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

	/**
	 * Return the IDs responsible for the given key.
	 *
	 * @param key Key.
	 * @return Set of responsible IDs.
	 */
	Set<Integer> responsibleForKey(int key) {
		return this.nodes.keySet().stream().sorted((o1, o2) -> {
			if (o1 >= key && o2 >= key) return o1 - o2;
			if (o1 >= key && o2 < key) return -1;
			if (o1 < key && o2 >= key) return +1;
			else return o1 - o2;
		}).limit(replication).collect(Collectors.toSet());
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
	 * Return the Ids of the replicas that would be responsible for current node's keys
	 * in the case that this node would leave the network.
	 *
	 * @return The ID of the replica.
	 */
	@NotNull
	Set<Integer> nextResponsibleReplicasForLeaving() {
		final Set<Integer> nextReplicas = new HashSet<>();
		final List<Integer> idsList = this.nodes.keySet().stream().sorted().collect(Collectors.toList());

		// get the next ReplicationNumber-th replicas after the current node
		int currentNodeIndex = idsList.indexOf(myID);
		for (int i = 1; i <= replication; i++) {

			int nextReplicaIndex = ((currentNodeIndex + i) % idsList.size());
			int nextReplica = idsList.get(nextReplicaIndex);

			// avoid to add current node if it is leaving
			if (nextReplica != myID) {
				nextReplicas.add(nextReplica);
			}
		}

		return nextReplicas;
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
		// TODO: immutable
		return this.nodes;
	}

	/**
	 * Return the set of IDs.
	 *
	 * @return IDs of all nodes.
	 */
	@NotNull
	Set<Integer> getNodeIDs() {
		// TODO: immutable
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
