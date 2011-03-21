package com.riverflows.data;

public class Favorite {
	private Site site;
	private String variableId;
	
	public Favorite(Site site, String variableId) {
		super();
		this.site = site;
		this.variableId = variableId;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public String getVariable() {
		return variableId;
	}

	public void setVariable(String variableId) {
		this.variableId = variableId;
	}
}
