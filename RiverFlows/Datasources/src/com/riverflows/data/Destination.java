package com.riverflows.data;

import java.io.Serializable;

/**
 * Created by robin on 6/27/13.
 */
public class Destination implements Serializable{

	private boolean placeholderObj = false;
    private Integer id;
	private UserAccount user;
	private String name;
	private Site site;
	private String description;
	private boolean shared;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UserAccount getUser() {
		return user;
	}

	public void setUser(UserAccount user) {
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public boolean isPlaceholderObj() {
		return placeholderObj;
	}

	public void setPlaceholderObj(boolean placeholderObj) {
		this.placeholderObj = placeholderObj;
	}
}
