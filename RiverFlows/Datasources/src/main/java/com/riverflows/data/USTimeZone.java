package com.riverflows.data;

import java.util.TimeZone;

public enum USTimeZone {
	UTC("GMT+00:00"),
	ADT("GMT-03:00"),
	AST("GMT-04:00"),
	EDT("GMT-04:00"),
	EST("GMT-05:00"),
	CDT("GMT-05:00"),
	CST("GMT-06:00"),
	MDT("GMT-06:00"),
	MST("GMT-07:00"),
	PDT("GMT-07:00"),
	PST("GMT-08:00"),
	AKDT("GMT-08:00"),
	AKST("GMT-09:00"),
	HADT("GMT-09:00"),
	HDT("GMT-09:00"),
	HAST("GMT-10:00"),
	HST("GMT-10:00");
	
	private String offsetString;
	
	private TimeZone timezone;
	
	private USTimeZone(String offsetString) {
		this.offsetString = offsetString;
		this.timezone = TimeZone.getTimeZone(this.offsetString);
	}
	
	public TimeZone getTimeZone() {
		return timezone;
	}
}
