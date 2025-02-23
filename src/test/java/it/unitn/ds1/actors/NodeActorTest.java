package it.unitn.ds1.actors;

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

}
