package com.riverflows.data;

import java.util.ArrayList;
import java.util.List;


public enum USState {
	AL("AL", "Alabama"),
	AK("AK", "Alaska"),
	AZ("AZ", "Arizona"),
	AR("AR", "Arkansas"),
	CA("CA", "California"),
	CO("CO", "Colorado"),
	CT("CT", "Connecticut"),
	DE("DE", "Delaware"),
	DC("DC", "District Of Columbia"),
	FL("FL", "Florida"),
	GA("GA", "Georgia"),
	HI("HI", "Hawaii"),
	ID("ID", "Idaho"),
	IL("IL", "Illinois"),
	IN("IN", "Indiana"),
	IA("IA", "Iowa"),
	KS("KS", "Kansas"),
	KY("KY", "Kentucky"),
	LA("LA", "Louisiana"),
	ME("ME", "Maine"),
	MD("MD", "Maryland"),
	MA("MA", "Massachusetts"),
	MI("MI", "Michigan"),
	MN("MN", "Minnesota"),
	MS("MS", "Mississippi"),
	MO("MO", "Missouri"),
	MT("MT", "Montana"),
	NE("NE", "Nebraska"),
	NV("NV", "Nevada"),
	NH("NH", "New Hampshire"),
	NJ("NJ", "New Jersey"),
	NM("NM", "New Mexico"),
	NY("NY", "New York"),
	NC("NC", "North Carolina"),
	ND("ND", "North Dakota"),
	OH("OH", "Ohio"),
	OK("OK", "Oklahoma"),
	OR("OR", "Oregon"),
	PA("PA", "Pennsylvania"),
	PR("PR", "Puerto Rico"),
	RI("RI", "Rhode Island"),
	SC("SC", "South Carolina"),
	SD("SD", "South Dakota"),
	TN("TN", "Tennessee"),
	TX("TX", "Texas"),
	UT("UT", "Utah"),
	VT("VT", "Vermont"),
	VA("VA", "Virginia"),
	WA("WA", "Washington"),
	WV("WV", "West Virginia"),
	WI("WI", "Wisconsin"),
	WY("WY", "Wyoming");
	
	private final String abbrev;
	private final String text;
	
	private USState(final String abbrev, final String text) {
		this.abbrev=abbrev;
		this.text=text;
	}
	
	public String getAbbrev() {
		return abbrev;
	}
	
	public String getText() {
		return text;
	}
	
	public static USState getUSStateText(final String abbrev) {
		USState[] values=USState.values();		
		for(USState usstate:values){
			if(usstate.getAbbrev().equals(abbrev)){
				return usstate;
			}
		}
		return null;
	}
	
	public static List<USState> asList() {
		USState[] values=USState.values();
		ArrayList<USState> result = new ArrayList<USState>(values.length);
		
		for(USState usstate:values){
			result.add(usstate);
		}
		return result;
	}
	
	@Override
	public String toString() {
		return text;
	}
}