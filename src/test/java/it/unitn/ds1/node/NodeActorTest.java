package it.unitn.ds1.node;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Tests for @{@link NodeActor}.
 *
 * @author Davide Pedranz
 */
public class NodeActorTest {

	@Test
	public void nextInRingAtStart() {
		final Set<Integer> ids = Sets.newHashSet(2, 3, 1, 4);
		final int actualNext = NodeActor.nextInTheRing(ids, 1);
		assertEquals(2, actualNext);
	}

	@Test
	public void nextInRingInTheMiddle() {
		final Set<Integer> ids = Sets.newHashSet(2, 3, 1, 4);
		final int actualNext = NodeActor.nextInTheRing(ids, 2);
		assertEquals(3, actualNext);
	}

	@Test
	public void nextInRingAtTheEnd() {
		final Set<Integer> ids = Sets.newHashSet(2, 3, 1, 4);
		final int actualNext = NodeActor.nextInTheRing(ids, 4);
		assertEquals(1, actualNext);
	}

	@Test
	public void responsibleForKeyAllBigger() {
		final Set<Integer> ids = Sets.newHashSet(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
		final Set<Integer> actual = NodeActor.responsibleForKey(ids, 0, 5);
		assertEquals(Sets.newHashSet(10, 20, 30, 40, 50), actual);
	}

	@Test
	public void responsibleForKeyAllSmaller() {
		final Set<Integer> ids = Sets.newHashSet(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
		final Set<Integer> actual = NodeActor.responsibleForKey(ids, 200, 4);
		assertEquals(Sets.newHashSet(10, 20, 30, 40), actual);
	}

	@Test
	public void responsibleForKeyGeneral1() {
		final Set<Integer> ids = Sets.newHashSet(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
		final Set<Integer> actual = NodeActor.responsibleForKey(ids, 50, 3);
		assertEquals(Sets.newHashSet(50, 60, 70), actual);
	}

	@Test
	public void responsibleForKeyGeneral2() {
		final Set<Integer> ids = Sets.newHashSet(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
		final Set<Integer> actual = NodeActor.responsibleForKey(ids, 51, 3);
		assertEquals(Sets.newHashSet(60, 70, 80), actual);
	}

	@Test
	public void responsibleForAll() {
		final Set<Integer> ids = Sets.newHashSet(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
		final Set<Integer> actual = NodeActor.responsibleForKey(ids, 50, 10);
		assertEquals(ids, actual);
	}

}
