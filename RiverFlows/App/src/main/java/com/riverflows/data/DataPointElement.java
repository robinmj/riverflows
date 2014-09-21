package com.riverflows.data;

public class DataPointElement {
	public DataPointElement(){}
	public DataPointElement(float y) {
		this.y = y;
	}
	private float y;
	public float getY() {
		return y;
	}
	public void setY(float y) {
		this.y = y;
	}
}
