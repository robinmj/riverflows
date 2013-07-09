package com.riverflows.data;

import java.io.Serializable;

/**
 * Created by robin on 6/27/13.
 */
public class DestinationFacet implements Serializable {
	private int userId;
	private String description;
	private Destination destination;
	private Double tooLow;
	private Double low;
	private Double med;
	private Double high;
	private Double highPlus;
	private int lowDifficulty;
	private int medDifficulty;
	private int highDifficulty;
	private int lowPortDifficulty;
	private int medPortDifficulty;
	private int highPortDifficulty;
	private int qualityLow;
	private int qualityMed;
	private int qualityHigh;

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
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

	public int getLowDifficulty() {
		return lowDifficulty;
	}

	public void setLowDifficulty(int lowDifficulty) {
		this.lowDifficulty = lowDifficulty;
	}

	public int getMedDifficulty() {
		return medDifficulty;
	}

	public void setMedDifficulty(int medDifficulty) {
		this.medDifficulty = medDifficulty;
	}

	public int getHighDifficulty() {
		return highDifficulty;
	}

	public void setHighDifficulty(int highDifficulty) {
		this.highDifficulty = highDifficulty;
	}

	public int getLowPortDifficulty() {
		return lowPortDifficulty;
	}

	public void setLowPortDifficulty(int lowPortDifficulty) {
		this.lowPortDifficulty = lowPortDifficulty;
	}

	public int getMedPortDifficulty() {
		return medPortDifficulty;
	}

	public void setMedPortDifficulty(int medPortDifficulty) {
		this.medPortDifficulty = medPortDifficulty;
	}

	public int getHighPortDifficulty() {
		return highPortDifficulty;
	}

	public void setHighPortDifficulty(int highPortDifficulty) {
		this.highPortDifficulty = highPortDifficulty;
	}

	public int getQualityLow() {
		return qualityLow;
	}

	public void setQualityLow(int qualityLow) {
		this.qualityLow = qualityLow;
	}

	public int getQualityMed() {
		return qualityMed;
	}

	public void setQualityMed(int qualityMed) {
		this.qualityMed = qualityMed;
	}

	public int getQualityHigh() {
		return qualityHigh;
	}

	public void setQualityHigh(int qualityHigh) {
		this.qualityHigh = qualityHigh;
	}
}
