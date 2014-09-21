package com.riverflows.data;

import java.io.Serializable;

public class Site implements Serializable {
	
	private static final long serialVersionUID = 4804489663377330926L;
	
	public Site(){}
	
	public Site(SiteId key, String name, USState state, Variable[] supportedVars) {
		this(key, name, null, null, state, supportedVars);
	}
	
	public Site(SiteId siteId, String name, Double longitude, Double latitude, USState state, Variable[] supportedVars) {
		super();
		this.name = name;
		this.siteId = siteId;
		this.longitude = longitude;
		this.latitude = latitude;
		this.state = state;
		this.supportedVariables = supportedVars;
	}
	
	private String name;
	private SiteId siteId;
	private Double longitude;
	private Double latitude;
	private USState state;
	private Variable[] supportedVariables;
	
	public USState getState() {
		return state;
	}

	public void setState(USState state) {
		this.state = state;
	}

	/**
	 * 
	 * @return an identifier for this site that is unique across agencies.
	 */
	public long getKey() {
		return siteId.hashCode();
	}
	public String getAgency() {
		return siteId.getAgency();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public SiteId getSiteId() {
		return this.siteId;
	}
	
	public void setSiteId(SiteId siteId) {
		this.siteId = siteId;
	}
	
	/**
	 * @return a string identifier for this site that is specific to its agency
	 */
	public String getId() {
		return siteId.getId();
	}
	@Override
	public String toString() {
		return name;
	}

	public Variable[] getSupportedVariables() {
		return supportedVariables;
	}

	public void setSupportedVariables(Variable[] supportedVariables) {
		this.supportedVariables = supportedVariables;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
}
