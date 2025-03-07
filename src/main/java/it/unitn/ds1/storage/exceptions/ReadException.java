package it.unitn.ds1.storage.exceptions;

import org.jetbrains.annotations.NonNls;

/**
 * Exception thrown when a write fails
 */
public class ReadException extends RuntimeException {

	public ReadException(@NonNls String message) {
		super(message);
	}

	public ReadException(Throwable cause) {
		super(cause);
	}
}
