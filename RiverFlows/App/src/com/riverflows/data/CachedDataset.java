package com.riverflows.data;

import java.util.Date;

public class CachedDataset {
	private String dataInfo;
	private Series series;
	private Date timestamp;
	
	public CachedDataset(String dataInfo, Series series, Date timestamp) {
		super();
		this.dataInfo = dataInfo;
		this.series = series;
		this.timestamp = timestamp;
	}
	
	public String getDataInfo() {
		return dataInfo;
	}
	public Series getSeries() {
		return series;
	}
	public Date getTimestamp() {
		return timestamp;
	}
}
