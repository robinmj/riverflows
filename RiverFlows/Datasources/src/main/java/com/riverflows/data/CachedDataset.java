package com.riverflows.data;

import java.util.Date;

public class CachedDataset {
	private String sourceUrl;
	private String cacheFileName;
	private Date timestamp;
	
	public CachedDataset(String sourceUrl, String cacheFileName, Date timestamp) {
		super();
		this.sourceUrl = sourceUrl;
		this.cacheFileName = cacheFileName;
		this.timestamp = timestamp;
	}
	
	public String getSourceUrl() {
		return sourceUrl;
	}
	public String getCacheFileName() {
		return cacheFileName;
	}
	public Date getTimestamp() {
		return timestamp;
	}
}
