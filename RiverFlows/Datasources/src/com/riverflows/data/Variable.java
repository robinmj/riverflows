package com.riverflows.data;

import java.io.Serializable;

/**
 * Maps to WaterML variable element
 * @author robin
 */
public class Variable implements Serializable {
	private static final long serialVersionUID = 8337284001324981157L;
	
	/**
	 * Correlates variables across data sources
	 * @author robin
	 *
	 */
	public enum CommonVariable {
		
		STREAMFLOW_CFS("Streamflow", "cfs"),
		GAUGE_HEIGHT_FT("Gauge Height", "ft"),
		GAUGE_HEIGHT_ABOVE_DATUM_M("Gauge Height Above Datum", "m"),
		GAUGE_DEPTH_TO_WATER_LEVEL_FT("Depth to Water Level", "ft"),
		OBSERVED_DEPTH_FT("Observed Depth", "ft"),//Distance, observation point to stream bottom, feet
		STREAM_VELOCITY_FPS("Stream Velocity", "ft/s"),
		WATERTEMP_C("Water Temperature", "°C", false),
		WATERTEMP_F("Water Temperature", "°F", false),
		INTERGRAVEL_WATERTEMP_C("Intergravel Water Temperature", "°C", false),//Temperature, intragravel water, degrees Celsius 
		RES_STORAGE_ACRE_FT("Reservoir Storage", "acre-ft", false),
		RES_ELEVATION_FT("Reservoir Surface Elevation", "ft", false),//Elevation above NGVD 1929, ft
		TIDE_ELEVATION_FT("Tide elevation above NAVD 1988","ft"),
		AIRTEMP_F("Air Temperature", "°F", false),
		AIRTEMP_C("Air Temperature", "°C", false),//Temperature, air, &#176;C
		WIND_SPEED_MPH("Wind Speed", "mph"),
		WIND_SPEED_M_S("Wind Speed", "m/s"),
		WIND_DIRECTION_DEGREES("Wind Direction", "°"),//degrees clockwise from N
		WIND_GUST_SPEED_MPH("Wind Gust Speed", "mph"),//Wind gust speed, air, mph 
		PRECIPITATION_TOTAL_IN("Precipitation, total","in"),
		SPECIFIC_CONDUCTANCE_MICROSIEMEN_AT_25C("Specific Conductance","µS/cm at 25°C", false),
		DISSOLVED_O2_MG_L("Dissolved Oxygen","mg/L", false),//Dissolved oxygen, water, unfiltered, mg/L
		DISSOLVED_O2_PCT("Dissolved Oxygen","%", false),//Dissolved oxygen, water, unfiltered, %saturation
		WATER_PH("pH",""),//pH, water, unfiltered, field, standard units
		TURBIDITY_FNU("Turbidity","FNU"),//, water, unfiltered, monochrome near infra-red LED light, 780-900 nm, detection angle 90 +/-2.5 degrees
		RELATIVE_HUMIDITY_PCT("Relative Humidity", "%", false),
		ATM_PRESSURE_MM_HG("Barometric Pressure", "mm Hg", false),
		ATM_PRESSURE_MB("Barometric Pressure", "mb", false),
		DCP_BATTERY_VOLTAGE("DCP Battery Voltage","V", false),
		DCP_SIGNAL_MODULATION_INDEX_DB("DCP signal modulation index","dB", false),
		ACOUSTIC_DOPPLER_VELOCITY_METER_SIGNAL_NOISE_RATIO("Acoustic Doppler Velocity Meter signal to noise ratio","", false),
		SAMPLE_ELEVATION_FT("Elevation of Sample","ft"),
		SAMPLE_COUNT("Count of samples collected by autosampler",""),//Count of samples collected by autosampler, number
		NUMBER_OF_SAMPLING_POINTS("Number of sampling points","");
		
		private final String name;
		private final String unit;
		private boolean graphAgainstZeroMinimum = true;
		
		public String getName() {
			return name;
		}

		public String getUnit() {
			return unit;
		}
		
		public boolean isGraphAgainstZeroMinimum() {
			return this.graphAgainstZeroMinimum;
		}
		
		private CommonVariable(String name, String unit) {
			this(name, unit, true);
		}
		
		private CommonVariable(String name, String unit, boolean graphAgainstZeroMinimum) {
			this.name = name;
			this.unit = unit;
			this.graphAgainstZeroMinimum = graphAgainstZeroMinimum;
		}
	}
	
	private CommonVariable commonVar;
	
	public Variable() {}
	
	public Variable(CommonVariable commonVar, String id, Double magicNullValue) {
		super();
		this.id = id;
		this.commonVar = commonVar;
		this.magicNullValue = magicNullValue;
	}
	private String id;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUnit() {
		return commonVar.getUnit();
	}
	private Double magicNullValue;
	
	public String getName() {
		return commonVar.getName();
	}
	public CommonVariable getCommonVariable() {
		return commonVar;
	}
	/**
	 * @return number used to represent a bad data point, or null if none specified.
	 */
	public Double getMagicNullValue() {
		return magicNullValue;
	}
	public void setMagicNullValue(Double magicNullValue) {
		this.magicNullValue = magicNullValue;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof Variable)) {
			return false;
		}
		Variable otherVar = (Variable)o;
		if(commonVar != otherVar.commonVar) {
			return false;
		}
		if(!id.equals(otherVar.id)) {
			return false;
		}
		return true;
	}
}
