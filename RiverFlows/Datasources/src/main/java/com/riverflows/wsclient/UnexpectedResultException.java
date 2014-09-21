package com.riverflows.wsclient;

/**
 * Thrown when an HTTP request returns a 4xx or 5xx error status
 * @author robin
 *
 */
public class UnexpectedResultException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1239857933874285657L;
	
	private int httpStatusCode;
	
	public UnexpectedResultException(String message, int statusCode) {
		super(message);
		this.httpStatusCode = statusCode;
	}
	
	public int getStatusCode() {
		return httpStatusCode;
	}
}
