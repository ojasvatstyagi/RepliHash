package it.unitn.ds1.client.commands;

import org.jetbrains.annotations.Nullable;

/**
 * Represent the result of a command. Each command can succeed of fail.
 * In case of success, a result can be returned. In case of failure, nothing is returned.
 */
public final class CommandResult {

	// internal variables
	private final boolean success;
	private final Object result;

	/**
	 * Create a new result of some command.
	 *
	 * @param success True if the command was successful, false otherwise.
	 * @param result  Result object, if any.
	 */
	public CommandResult(boolean success, @Nullable Object result) {
		this.success = success;
		this.result = result;
	}

	/**
	 * @return True if the command was successful, false otherwise.
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @return Result object, if any.
	 */
	@Nullable
	public Object getResult() {
		return result;
	}
}
