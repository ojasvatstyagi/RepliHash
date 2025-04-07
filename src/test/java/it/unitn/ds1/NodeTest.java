package it.unitn.ds1;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

/**
 * Tests for @{@link Node}.
 *
 * @author Davide Pedranz
 */
public final class NodeTest {

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

	@Test
	public void wrongCommand() {
		exit.expectSystemExitWithStatus(2);
		Node.main(new String[]{"wrong"});
	}

	@Test
	public void joinWrongParametersNumber() {
		exit.expectSystemExitWithStatus(2);
		Node.main(new String[]{"leave", "xyz"});
	}

	@Test
	public void joinWrongParameters() {
		exit.expectSystemExitWithStatus(2);
		Node.main(new String[]{"leave", "xyz", "124"});
	}

	@Test
	public void recoverWrongParametersNumber() {
		exit.expectSystemExitWithStatus(2);
		Node.main(new String[]{"recover", "123.232.123.45"});
	}

	@Test
	public void recoverWrongParameters() {
		exit.expectSystemExitWithStatus(2);
		Node.main(new String[]{"recover", "333.333.333.333", "12312"});
	}

}
