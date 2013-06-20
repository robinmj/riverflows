package com.riverflows.data;

import java.util.Date;

/**
 * Created by robin on 6/2/13.
 */
public class UserAccount {

	private Date createdAt;
	private Date updatedAt;
	private String nickname;
	private String   firstName;
	private String   lastName;
	private Float    latitude;
	private Float    longitude;
	private int  facetTypes;
	private String email;

	public UserAccount(){}
	
	public UserAccount(UserAccount source) {
		this.createdAt = source.createdAt;
		this.updatedAt = source.updatedAt;
		this.nickname = source.nickname;
		this.firstName = source.firstName;
		this.lastName = source.lastName;
		this.latitude = source.latitude;
		this.longitude = source.longitude;
		this.facetTypes = source.facetTypes;
		this.email = source.email;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Float getLatitude() {
		return latitude;
	}

	public void setLatitude(Float latitude) {
		this.latitude = latitude;
	}

	public Float getLongitude() {
		return longitude;
	}

	public void setLongitude(Float longitude) {
		this.longitude = longitude;
	}

	public int getFacetTypes() {
		return facetTypes;
	}

	public void setFacetTypes(int facetTypes) {
		this.facetTypes = facetTypes;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new UserAccount(this);
	}
}
