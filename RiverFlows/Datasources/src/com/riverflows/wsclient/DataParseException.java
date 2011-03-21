package com.riverflows.wsclient;

/**
 * Thrown when downloaded data doesn't adhere to the expected format.
 * @author robin
 *
 */
public class DataParseException extends RuntimeException {

	public DataParseException() {
		super();
	}

	public DataParseException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public DataParseException(String detailMessage) {
		super(detailMessage);
	}

	public DataParseException(Throwable throwable) {
		super(throwable);
	}

}
