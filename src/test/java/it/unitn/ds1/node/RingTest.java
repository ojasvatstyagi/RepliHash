package it.unitn.ds1.node;

import akka.actor.ActorRef;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Tests for @{@link Ring}.
 */
public final class RingTest {

	private static Ring createRing(int[] ids, int replication, int myID) {
		final Ring ring = new Ring(replication, myID);
		for (int id : ids) {
			ring.addNode(id, ActorRef.noSender());
		}
		return ring;
	}

	@Test
	public void nextInRingAtStart() {
		final Ring ring = createRing(new int[]{2, 3, 1, 4}, 3, 1);
		final int actualNext = ring.nextIDInTheRing();
		assertEquals(2, actualNext);
	}

	@Test
	public void nextInRingInTheMiddle() {
		final Ring ring = createRing(new int[]{2, 3, 1, 4}, 3, 2);
		final int actualNext = ring.nextIDInTheRing();
		assertEquals(3, actualNext);
	}

	@Test
	public void nextInRingAtTheEnd() {
		final Ring ring = createRing(new int[]{2, 3, 1, 4}, 3, 4);
		final int actualNext = ring.nextIDInTheRing();
		assertEquals(1, actualNext);
	}

	@Test
	public void responsibleForKeyAllBigger() {
		final Ring ring = createRing(new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, 5, 4);
		final Set<Integer> actual = ring.responsibleForKey(0);
		assertEquals(Sets.newHashSet(10, 20, 30, 40, 50), actual);
	}

	@Test
	public void responsibleForKeyAllSmaller() {
		final Ring ring = createRing(new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, 4, 4);
		final Set<Integer> actual = ring.responsibleForKey(0);
		assertEquals(Sets.newHashSet(10, 20, 30, 40), actual);
	}

	@Test
	public void responsibleForKeyGeneral1() {
		final Ring ring = createRing(new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, 3, 4);
		final Set<Integer> actual = ring.responsibleForKey(50);
		assertEquals(Sets.newHashSet(50, 60, 70), actual);
	}

	@Test
	public void responsibleForKeyGeneral2() {
		final Ring ring = createRing(new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, 3, 4);
		final Set<Integer> actual = ring.responsibleForKey(51);
		assertEquals(Sets.newHashSet(60, 70, 80), actual);
	}

	@Test
	public void responsibleForAll() {
		final Ring ring = createRing(new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, 10, 4);
		final Set<Integer> actual = ring.responsibleForKey(50);
		assertEquals(Sets.newHashSet(10, 20, 30, 40, 50, 60, 70, 80, 90, 100), actual);
	}

	@Test
	public void responsibleForKeyIAmLeaving1() {
		final Ring ring = createRing(new int[]{10, 20, 30, 40, 50, 60}, 3, 50);
		final Set<Integer> actual = ring.nextResponsibleReplicasForLeaving(50);
		assertEquals(Sets.newHashSet(10, 20, 60), actual);
	}

	@Test
	public void responsibleForKeyIAmLeaving2() {
		final Ring ring = createRing(new int[]{10, 20, 30}, 3, 30);
		final Set<Integer> actual = ring.nextResponsibleReplicasForLeaving(40);
		assertEquals(Sets.newHashSet(10, 20), actual);
	}

	@Test
	public void responsibleForKeyIAmLeaving3() {
		final Ring ring = createRing(new int[]{10, 20}, 3, 10);
		final Set<Integer> actual = ring.nextResponsibleReplicasForLeaving(15);
		assertEquals(Sets.newHashSet(20), actual);
	}

}
