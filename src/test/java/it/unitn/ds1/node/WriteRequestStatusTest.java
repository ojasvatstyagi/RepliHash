package it.unitn.ds1.node;

import akka.actor.ActorRef;
import it.unitn.ds1.node.status.WriteRequestStatus;
import it.unitn.ds1.storage.VersionedItem;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for @{@link WriteRequestStatus}.
 */
public final class WriteRequestStatusTest {

	@Test
	public void checkGets() {
		final WriteRequestStatus s = new WriteRequestStatus(44, "ciao", ActorRef.noSender(), 1, 1);
		s.addVote(new VersionedItem("ciao", 1));
		assertEquals(44, s.getKey());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
		assertEquals(ActorRef.noSender(), s.getSender());
	}

	@Test(expected = IllegalStateException.class)
	public void noVotes() {
		final WriteRequestStatus s = new WriteRequestStatus(44, "ciao", ActorRef.noSender(), 2, 2);
		assertFalse(s.isQuorumReached());
		s.getUpdatedRecord();
	}

	@Test
	public void quorumNotReached1() {
		final WriteRequestStatus s = new WriteRequestStatus(44, "ciao", ActorRef.noSender(), 2, 3);
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("ciao", 1));
		assertFalse(s.isQuorumReached());
	}

	@Test
	public void quorumNotReached2() {
		final WriteRequestStatus s = new WriteRequestStatus(44, "ciao", ActorRef.noSender(), 3, 2);
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("ciao", 1));
		assertFalse(s.isQuorumReached());
	}

	@Test
	public void singleVersion() {
		final WriteRequestStatus s = new WriteRequestStatus(44, "ciao", ActorRef.noSender(), 2, 2);
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("ciao", 1));
		assertTrue(s.isQuorumReached());
		assertEquals(2, s.getUpdatedRecord().getVersion());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
	}

	@Test
	public void multipleVotes() {
		final WriteRequestStatus s = new WriteRequestStatus(44, "ciao", ActorRef.noSender(), 3, 2);
		s.addVote(new VersionedItem("ciao", 2));
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("hello", 4));
		assertTrue(s.isQuorumReached());
		assertEquals(5, s.getUpdatedRecord().getVersion());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
	}

	@Test
	public void mixedVotes() {
		final WriteRequestStatus s = new WriteRequestStatus(44, "ciao", ActorRef.noSender(), 3, 2);
		s.addVote(null);
		s.addVote(new VersionedItem("hello", 2));
		s.addVote(new VersionedItem("ciao", 1));
		assertEquals(3, s.getUpdatedRecord().getVersion());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
	}

	@Test
	public void nullVotes() {
		final WriteRequestStatus s = new WriteRequestStatus(44, "ciao", ActorRef.noSender(), 3, 2);
		s.addVote(null);
		s.addVote(null);
		s.addVote(null);
		assertEquals(1, s.getUpdatedRecord().getVersion());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
	}

}
