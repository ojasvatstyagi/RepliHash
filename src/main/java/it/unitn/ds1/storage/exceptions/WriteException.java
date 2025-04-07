package it.unitn.ds1.storage.exceptions;

import org.jetbrains.annotations.NonNls;

/**
 * Write records exception
 */
public final class WriteException extends RuntimeException {

	public WriteException(@NonNls String message) {
		super(message);
	}

	public WriteException(Throwable cause) {
		super(cause);
	}
}
