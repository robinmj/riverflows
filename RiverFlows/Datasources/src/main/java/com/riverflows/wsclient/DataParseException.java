package com.riverflows.wsclient;

import com.riverflows.data.SiteId;

/**
 * Thrown when downloaded data doesn't adhere to the expected format.
 * @author robin
 *
 */
public class DataParseException extends RuntimeException {
	
	private static final long serialVersionUID = 7456724370934L;
	
	private SiteId siteId;

	public DataParseException() {
		super();
	}

	public DataParseException(SiteId siteId, String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		this.siteId = siteId;
	}

	public DataParseException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public DataParseException(String detailMessage) {
		super(detailMessage);
	}

	public DataParseException(SiteId siteId, String detailMessage) {
		super(detailMessage);
		this.siteId = siteId;
	}

	public DataParseException(Throwable throwable) {
		super(throwable);
	}
	
	public SiteId getSiteId() {
		return siteId;
	}

}
