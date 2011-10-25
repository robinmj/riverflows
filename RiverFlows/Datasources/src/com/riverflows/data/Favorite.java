package com.riverflows.data;

import java.util.Date;

public class Favorite {
	private Integer id;
	private Site site;
	private String variableId;
	private String name;
	private int order = 0;
	private Date creationDate;
	
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
}
