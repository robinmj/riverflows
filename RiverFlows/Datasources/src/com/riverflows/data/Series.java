package com.riverflows.data;

import java.util.List;

public class Series {
	private Variable variable;
	private List<Reading> readings;
	private String sourceUrl;

	public List<Reading> getReadings() {
		return readings;
	}

	public void setReadings(List<Reading> readings) {
		this.readings = readings;
	}

	public Variable getVariable() {
		return variable;
	}

	public void setVariable(Variable variable) {
		this.variable = variable;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

    /**
     * @param s cannot be null
     * @return the last non-forecasted reading in a series, or null if the series has no
     *  non-forecasted readings
     */
	public Reading getLastObservation() {

        int readingIndex = readings.size() - 1;
        Reading mostRecentReading = null;
        
        do {
        	if(readingIndex < 0) {
        		break;
        	}
        	mostRecentReading = readings.get(readingIndex--);
        } while(mostRecentReading.getValue() == null || mostRecentReading instanceof Forecast);
        
        return mostRecentReading;
	}
}
