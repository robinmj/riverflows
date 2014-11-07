package com.riverflows.data;

import java.io.Serializable;
import java.util.Date;

public class Favorite implements Serializable, RemoteObject {
	private Integer id;
	private Site site;
	private String variableId;
	private String name;
	private int order = 0;
	private Date creationDate;
	private DestinationFacet destinationFacet;
    private boolean placeholderObj = false;
	
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

    /** @return primary key of favorite in local database, or null if not yet stored */
	public Integer getId() {
		return id;
	}

    /** @param id primary key of favorite in local database */
	public void setId(Integer id) {
		this.id = id;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public DestinationFacet getDestinationFacet() {
		return destinationFacet;
	}

	public void setDestinationFacet(DestinationFacet destinationFacet) {
		this.destinationFacet = destinationFacet;
	}

    public boolean isPlaceholderObj() {
        return placeholderObj;
    }

    public void setPlaceholderObj(boolean placeholderObj) {
        this.placeholderObj = placeholderObj;
    }
}
