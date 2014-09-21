package com.riverflows.data;

import java.util.Date;

public class Reading implements Comparable<Reading> {
	private Date date;
	private Double value;
	private String qualifiers;
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public Double getValue() {
		return value;
	}
	public void setValue(Double value) {
		this.value = value;
	}
	
	/**
	 * @return a space-separated list of opaque tokens describing this data point
	 *  (possibly explaining why value is null), or null if there are no qualifiers.
	 */
	public String getQualifiers() {
		return qualifiers;
	}
	public void setQualifiers(String qualifiers) {
		this.qualifiers = qualifiers;
	}
	
	@Override
	public int compareTo(Reading another) {
		if(another == null) {
			return 1;
		}
		return another.getDate().compareTo(date);
	}
}
