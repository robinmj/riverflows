package com.riverflows.data;

import java.io.Serializable;

public class SiteId implements Serializable {
	
	private static final long serialVersionUID = -7632231479270390891L;
	
	private Integer primaryKey;
	private final String agency;
	private final String id;
	
	private final String key;
	
	public SiteId(String key) {
		int slashLoc = key.indexOf('/');
		
		try {
			this.agency = key.substring(0, slashLoc);
			this.id = key.substring(slashLoc + 1);
		} catch(IndexOutOfBoundsException ioobe) {
			throw new IllegalArgumentException("invalid site ID: " + key);
		}
		
		this.key = key;
	}
	
	public SiteId(String agency, String id) {
		this.agency = agency;
		this.id = id;
		this.key = agency + "/" + id;
	}
	
	public SiteId(String agency, String id, int primaryKey) {
		this.agency = agency;
		this.id = id;
		this.key = agency + "/" + id;
		this.primaryKey = primaryKey;
	}
	
	public String getId() {
		return id;
	}
	
	public String getAgency() {
		return agency;
	}
	
	@Override
	public String toString() {
		return key;
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) {
			return false;
		}
		try {
			String otherSiteId = ((SiteId)o).toString();
			
			return toString().equals(otherSiteId);
		} catch(ClassCastException cce) {
		}
		return false;
	}

	/**
	 * @return primary key used internally by the RiverFlows app database
	 */
	public Integer getPrimaryKey() {
		return primaryKey;
	}


	/**
	 * set the primary key used internally by the RiverFlows app database
	 */
	public void setPrimaryKey(Integer primaryKey) {
		this.primaryKey = primaryKey;
	}
}
