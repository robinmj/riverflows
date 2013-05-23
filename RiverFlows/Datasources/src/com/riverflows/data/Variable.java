package com.riverflows.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
		RES_ELEVATION_FT("Surface Elevation", "ft", false),//Elevation above NGVD 1929, ft
		ROLLER_LEVEL_FT("Roller Level", "ft", false),
		TIDE_ELEVATION_FT("Tide elevation above NAVD 1988","ft"),
		AIRTEMP_F("Air Temperature", "°F", false),
		AIRTEMP_C("Air Temperature", "°C", false),//Temperature, air, &#176;C
		WIND_SPEED_MPH("Wind Speed", "mph"),
		WIND_SPEED_M_S("Wind Speed", "m/s"),
		WIND_DIRECTION_DEGREES("Wind Direction", "°"),//degrees clockwise from N
		WIND_GUST_SPEED_MPH("Wind Gust Speed", "mph"),//Wind gust speed, air, mph 
		WIND_GUST_DIRECTION_DEGREES("Wind Gust Direction", "°"),//WIND, DIRECTION OF PEAK GUST, deg
		PRECIPITATION_TOTAL_IN("Precipitation, total","in"),
		SPECIFIC_CONDUCTANCE_MICROSIEMEN_AT_25C("Specific Conductance","µS/cm at 25°C", false),
		DISSOLVED_O2_MG_L("Dissolved Oxygen","mg/L", false),//Dissolved oxygen, water, unfiltered, mg/L
		DISSOLVED_O2_PCT("Dissolved Oxygen","%", false),//Dissolved oxygen, water, unfiltered, %saturation
		WATER_PH("pH",""),//pH, water, unfiltered, field, standard units //WATER, PH VALUE, ph
		TURBIDITY_FNU("Turbidity","FNU"),//, water, unfiltered, monochrome near infra-red LED light, 780-900 nm, detection angle 90 +/-2.5 degrees
		RELATIVE_HUMIDITY_PCT("Relative Humidity", "%", false),
		ATM_PRESSURE_MM_HG("Barometric Pressure", "mm Hg", false),
		ATM_PRESSURE_MB("Barometric Pressure", "mb", false),
		DCP_BATTERY_VOLTAGE("DCP Battery Voltage","V", false),
		DCP_SIGNAL_MODULATION_INDEX_DB("DCP signal modulation index","dB", false),
		ACOUSTIC_DOPPLER_VELOCITY_METER_SIGNAL_NOISE_RATIO("Acoustic Doppler Velocity Meter signal to noise ratio","", false),
		SAMPLE_ELEVATION_FT("Elevation of Sample","ft"),
		SAMPLE_COUNT("Count of samples collected by autosampler",""),//Count of samples collected by autosampler, number
		NUMBER_OF_SAMPLING_POINTS("Number of sampling points",""),
		FUEL_MOISTURE_WOOD_PCT("Fuel moisture, wood","%", false), //FUEL MOISTURE, WOOD, %
		SOLAR_RADIATION_W_SQ_M("Solar Radiation", "w/m^2", false), //SOLAR RADIATION, w/m^2
		ATM_PRESSURE_IN("Atmospheric Pressure", "in", false), //ATMOSPHERIC PRESSURE, inches
		CREEK_BED_ELEV_FT("Creek Bed Elevation", "ft", false), //CREEK BED ELEV FROM MEAN SEA L, feet
		SNOW_DEPTH_IN("Snow Depth", "in"), //SNOW DEPTH, inches, 
		SNOW_WATER_CONTENT_IN("Snow Water Content", "in"), //SNOW, WATER CONTENT, inches, 
		PRECIP_RAINTIP_IN("Precipitation, Tipping Bucket", "in"), //PRECIPITATION, TIPPING BUCKET, inches,
		ELECTRICAL_CONDUCTIVITY_MS_CM("Electrical Conductivity", "mS/cm", false), //ELECTRICAL CONDUCTIVTY MILLI S, ms/cm
		RES_OUTFLOW_CFS("Reservoir Outflow","cfs", false), //RESERVOIR OUTFLOW, cfs
		RES_INFLOW_CFS("Reservoir Inflow","cfs", false), //RESERVOIR INFLOW, cfs
		RIVER_STAGE_ELEV_FT("River Stage above NAVD88", "ft", false), //RIVER STAGE NAVD88, feet
		CONTROL_REGULATING_DISCHARGE("Discharge, Control Regulating", "cfs", false),  //DISCHARGE,CONTROL REGULATING, cfs
		BOTTOM_ELECTRICAL_CONDUCTIVITY_MICROS_CM("Electrical Conductivity on Bottom","µS/cm", false); //ELECTRICAL COND BOTTOM MICRO S, us/cm
		
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
		
		public static CommonVariable getByNameAndUnit(String name, String unit) {
			CommonVariable[] all = values();
			
			for(CommonVariable current:all) {
				if(!current.name.equals(name)) {
					continue;
				}
				
				if(!current.unit.equals(unit)) {
					continue;
				}
				
				return current;
			}
			
			return null;
		}

		
		public static Map<CommonVariable,CommonVariable> temperatureConversionMap(String preferredTempUnit) {
			
			Map<CommonVariable,CommonVariable> conversionMap = new HashMap<CommonVariable,CommonVariable>();
			
			if(preferredTempUnit == null) {
				return conversionMap;
			}
			
			String otherUnit = null;
			if(preferredTempUnit.equals("°C")) {
				otherUnit = "°F";
			} else {
				otherUnit = "°C";
			}			
			
			
			CommonVariable[] all = values();
			
			for(CommonVariable fromVar:all) {
				
				if(!fromVar.unit.equals(otherUnit)) {
					continue;
				}
		    	
		    	CommonVariable toVar = CommonVariable.getByNameAndUnit(fromVar.name, preferredTempUnit);
		    	
		    	if(toVar != null) {
		    		conversionMap.put(fromVar, toVar);
		    	}
			}
			
			return conversionMap;
		}
	}
	
	private CommonVariable commonVar;
	
	private String exactName;
	
	public Variable() {}
	
	public Variable(CommonVariable commonVar, String id, Double magicNullValue) {
		super();
		this.id = id;
		this.commonVar = commonVar;
		this.magicNullValue = magicNullValue;
	}
	
	public Variable(CommonVariable commonVar, String id, Double magicNullValue, String exactName) {
		super();
		this.id = id;
		this.commonVar = commonVar;
		this.magicNullValue = magicNullValue;
		this.exactName = exactName;
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
	
	/**
	 * 
	 * @return the name of the CommonVariable
	 */
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
	
	@Override
	public String toString() {
		return "" + id + " " + commonVar;
	}

	/**
	 * @return the actual name of the variable used by the agency, rather than the common variable name
	 */
	public String getExactName() {
		return exactName;
	}

	public void setExactName(String exactName) {
		this.exactName = exactName;
	}
}
