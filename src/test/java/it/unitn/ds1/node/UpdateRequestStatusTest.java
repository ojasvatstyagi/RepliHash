package it.unitn.ds1.node;

import akka.actor.ActorRef;
import it.unitn.ds1.node.status.UpdateRequestStatus;
import it.unitn.ds1.storage.VersionedItem;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for @{@link UpdateRequestStatus}.
 */
public final class UpdateRequestStatusTest {

	@Test
	public void checkGets() {
		final UpdateRequestStatus s = new UpdateRequestStatus(44, "ciao", ActorRef.noSender(), 1, 1);
		s.addVote(new VersionedItem("ciao", 1));
		assertEquals(44, s.getKey());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
		assertEquals(ActorRef.noSender(), s.getSender());
	}

	@Test(expected = IllegalStateException.class)
	public void noVotes() {
		final UpdateRequestStatus s = new UpdateRequestStatus(44, "ciao", ActorRef.noSender(), 2, 2);
		assertFalse(s.isQuorumReached());
		s.getUpdatedRecord();
	}

	@Test
	public void quorumNotReached1() {
		final UpdateRequestStatus s = new UpdateRequestStatus(44, "ciao", ActorRef.noSender(), 2, 3);
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("ciao", 1));
		assertFalse(s.isQuorumReached());
	}

	@Test
	public void quorumNotReached2() {
		final UpdateRequestStatus s = new UpdateRequestStatus(44, "ciao", ActorRef.noSender(), 3, 2);
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("ciao", 1));
		assertFalse(s.isQuorumReached());
	}

	@Test
	public void singleVersion() {
		final UpdateRequestStatus s = new UpdateRequestStatus(44, "ciao", ActorRef.noSender(), 2, 2);
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("ciao", 1));
		assertTrue(s.isQuorumReached());
		assertEquals(2, s.getUpdatedRecord().getVersion());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
	}

	@Test
	public void multipleVotes() {
		final UpdateRequestStatus s = new UpdateRequestStatus(44, "ciao", ActorRef.noSender(), 3, 2);
		s.addVote(new VersionedItem("ciao", 2));
		s.addVote(new VersionedItem("ciao", 1));
		s.addVote(new VersionedItem("hello", 4));
		assertTrue(s.isQuorumReached());
		assertEquals(5, s.getUpdatedRecord().getVersion());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
	}

	@Test
	public void mixedVotes() {
		final UpdateRequestStatus s = new UpdateRequestStatus(44, "ciao", ActorRef.noSender(), 3, 2);
		s.addVote(null);
		s.addVote(new VersionedItem("hello", 2));
		s.addVote(new VersionedItem("ciao", 1));
		assertEquals(3, s.getUpdatedRecord().getVersion());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
	}

	@Test
	public void nullVotes() {
		final UpdateRequestStatus s = new UpdateRequestStatus(44, "ciao", ActorRef.noSender(), 3, 2);
		s.addVote(null);
		s.addVote(null);
		s.addVote(null);
		assertEquals(1, s.getUpdatedRecord().getVersion());
		assertEquals("ciao", s.getUpdatedRecord().getValue());
	}

}
