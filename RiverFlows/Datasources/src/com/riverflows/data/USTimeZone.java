package com.riverflows.data;

import java.util.TimeZone;

public enum USTimeZone {
	UTC("GMT+00:00"),
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
	HAST("GMT-10:00");
	
	private String offsetString;
	
	private USTimeZone(String offsetString) {
		this.offsetString = offsetString;
	}
	
	public TimeZone getTimeZone() {
		return TimeZone.getTimeZone(this.offsetString);
	}
}
