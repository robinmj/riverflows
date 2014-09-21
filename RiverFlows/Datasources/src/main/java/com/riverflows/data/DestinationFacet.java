package com.riverflows.data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by robin on 6/27/13.
 */
public class DestinationFacet implements Serializable {

	private boolean placeholderObj = false;
    private Integer id;
	private UserAccount user;
	private String description;
	private Destination destination;
    private int facetType;
    private Variable variable;
	private Double tooLow;
	private Double low;
	private Double med;
	private Double high;
	private Double highPlus;
	private Integer lowDifficulty;
	private Integer medDifficulty;
	private Integer highDifficulty;
	private Integer lowPortDifficulty;
	private Integer medPortDifficulty;
	private Integer highPortDifficulty;
	private Integer qualityLow;
	private Integer qualityMed;
	private Integer qualityHigh;
	private Date creationDate;
	private Date modificationDate;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Destination getDestination() {
		return destination;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

    public int getFacetType() {
        return facetType;
    }

    public void setFacetType(int facetType) {
        this.facetType = facetType;
    }

    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }


    public Double getTooLow() {
		return tooLow;
	}

	public void setTooLow(Double tooLow) {
		this.tooLow = tooLow;
	}

	public Double getLow() {
		return low;
	}

	public void setLow(Double low) {
		this.low = low;
	}

	public Double getMed() {
		return med;
	}

	public void setMed(Double med) {
		this.med = med;
	}

	public Double getHigh() {
		return high;
	}

	public void setHigh(Double high) {
		this.high = high;
	}

	public Double getHighPlus() {
		return highPlus;
	}

	public void setHighPlus(Double highPlus) {
		this.highPlus = highPlus;
	}

	public Integer getLowDifficulty() {
		return lowDifficulty;
	}

	public void setLowDifficulty(Integer lowDifficulty) {
		this.lowDifficulty = lowDifficulty;
	}

	public Integer getMedDifficulty() {
		return medDifficulty;
	}

	public void setMedDifficulty(Integer medDifficulty) {
		this.medDifficulty = medDifficulty;
	}

	public Integer getHighDifficulty() {
		return highDifficulty;
	}

	public void setHighDifficulty(Integer highDifficulty) {
		this.highDifficulty = highDifficulty;
	}

	public Integer getLowPortDifficulty() {
		return lowPortDifficulty;
	}

	public void setLowPortDifficulty(Integer lowPortDifficulty) {
		this.lowPortDifficulty = lowPortDifficulty;
	}

	public Integer getMedPortDifficulty() {
		return medPortDifficulty;
	}

	public void setMedPortDifficulty(Integer medPortDifficulty) {
		this.medPortDifficulty = medPortDifficulty;
	}

	public Integer getHighPortDifficulty() {
		return highPortDifficulty;
	}

	public void setHighPortDifficulty(Integer highPortDifficulty) {
		this.highPortDifficulty = highPortDifficulty;
	}

	public Integer getQualityLow() {
		return qualityLow;
	}

	public void setQualityLow(Integer qualityLow) {
		this.qualityLow = qualityLow;
	}

	public Integer getQualityMed() {
		return qualityMed;
	}

	public void setQualityMed(Integer qualityMed) {
		this.qualityMed = qualityMed;
	}

	public Integer getQualityHigh() {
		return qualityHigh;
	}

	public void setQualityHigh(Integer qualityHigh) {
		this.qualityHigh = qualityHigh;
	}

	public boolean isPlaceholderObj() {
		return placeholderObj;
	}

	public void setPlaceholderObj(boolean placeholderObj) {
		this.placeholderObj = placeholderObj;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Date modificationDate) {
		this.modificationDate = modificationDate;
	}
}
