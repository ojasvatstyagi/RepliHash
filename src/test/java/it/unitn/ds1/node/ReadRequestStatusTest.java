package it.unitn.ds1.node;

import akka.actor.ActorRef;
import it.unitn.ds1.storage.VersionedItem;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for @{@link ReadRequestStatus}.
 */
public class ReadRequestStatusTest {

	@Test
	public void checkGets() {
		final ReadRequestStatus s = new ReadRequestStatus(3, ActorRef.noSender(), 1);
		assertEquals(3, s.getKey());
		assertEquals(ActorRef.noSender(), s.getSender());
	}

	@Test(expected = IllegalStateException.class)
	public void noVotes() {
		final ReadRequestStatus s = new ReadRequestStatus(3, ActorRef.noSender(), 1);
		assertNull(s.getLatestValue());
	}

	@Test
	public void nullVotes() {
		final ReadRequestStatus s = new ReadRequestStatus(3, ActorRef.noSender(), 1);
		s.addVote(null);
		assertNull(s.getLatestValue());
	}

	@Test
	public void singleVote() {
		final ReadRequestStatus s = new ReadRequestStatus(3, ActorRef.noSender(), 1);
		s.addVote(new VersionedItem("ciao", 1));
		assertTrue(s.isQuorumReached());
		assertEquals("ciao", s.getLatestValue());
	}

	@Test
	public void multipleVotes() {
		final ReadRequestStatus s = new ReadRequestStatus(3, ActorRef.noSender(), 2);
		s.addVote(new VersionedItem("hello", 2));
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("world", 4));
		assertTrue(s.isQuorumReached());
		assertEquals("world", s.getLatestValue());
	}

	@Test
	public void duplicatedVotes() {
		final ReadRequestStatus s = new ReadRequestStatus(3, ActorRef.noSender(), 3);
		s.addVote(new VersionedItem("hello", 2));
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("hello", 2));
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("ciao", 1));
		assertTrue(s.isQuorumReached());
		assertEquals("hello", s.getLatestValue());
	}

	@Test
	public void mixedVotes() {
		final ReadRequestStatus s = new ReadRequestStatus(3, ActorRef.noSender(), 3);
		s.addVote(null);
		s.addVote(new VersionedItem("hello", 2));
		s.addVote(new VersionedItem("ciao", 1));
		assertEquals("hello", s.getLatestValue());
	}

}
