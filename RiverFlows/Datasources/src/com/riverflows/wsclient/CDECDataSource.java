package com.riverflows.wsclient;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USTimeZone;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

public class CDECDataSource implements RESTDataSource {
	
	private static final Log LOG = LogFactory.getLog(CDECDataSource.class);
	
	public static final String AGENCY = "CDEC";

	public static final String AGENCY_URL = "http://cdec.water.ca.gov";
	public static final String SITE_DATA_URL = AGENCY_URL + "/cgi-progs/queryF";
	
	public static final Variable VTYPE_BAT_VOL = new Variable(CommonVariable.DCP_BATTERY_VOLTAGE,"BAT%20VOL", -99999d); //BATTERY VOLTAGE,volts
	public static final Variable VTYPE_FUEL_MS = new Variable(CommonVariable.FUEL_MOISTURE_WOOD_PCT,"FUEL%20MS", -99999d); //FUEL MOISTURE, WOOD, %
	public static final Variable VTYPE_RAIN = new Variable(CommonVariable.PRECIPITATION_TOTAL_IN,"RAIN", -99999d); //PRECIPITATION, ACCUMULATED, inches, 
	public static final Variable VTYPE_REL_HUM = new Variable(CommonVariable.RELATIVE_HUMIDITY_PCT,"REL%20HUM", -99999d); //RELATIVE HUMIDITY, %, 
	public static final Variable VTYPE_WIND_DR = new Variable(CommonVariable.WIND_DIRECTION_DEGREES,"WIND%20DR", -99999d); //WIND, DIRECTION, deg, 
	public static final Variable VTYPE_PEAK_WD = new Variable(CommonVariable.WIND_GUST_DIRECTION_DEGREES,"PEAK%20WD", -99999d); //WIND, DIRECTION OF PEAK GUST, deg
	public static final Variable VTYPE_PEAK_WS = new Variable(CommonVariable.WIND_GUST_SPEED_MPH,"PEAK%20WS", -99999d); //WIND, PEAK GUST, mph, 
	public static final Variable VTYPE_WIND_SP = new Variable(CommonVariable.WIND_SPEED_MPH,"WIND%20SP", -99999d); //WIND, SPEED, mph, 
	public static final Variable VTYPE_SOLAR_R = new Variable(CommonVariable.SOLAR_RADIATION_W_SQ_M,"SOLAR%20R", -99999d); //SOLAR RADIATION, w/m^2, 
	public static final Variable VTYPE_BAR_PRE = new Variable(CommonVariable.ATM_PRESSURE_IN,"BAR%20PRE", -99999d); //ATMOSPHERIC PRESSURE, inches
	public static final Variable VTYPE_CB_ELEV = new Variable(CommonVariable.CREEK_BED_ELEV_FT,"CB%20ELEV", -99999d); //CREEK BED ELEV FROM MEAN SEA L, feet, 
	public static final Variable VTYPE_SNOW_DP = new Variable(CommonVariable.SNOW_DEPTH_IN,"SNOW%20DP", -99999d); //SNOW DEPTH, inches, 
	public static final Variable VTYPE_SNOW_WC = new Variable(CommonVariable.SNOW_WATER_CONTENT_IN,"SNOW%20WC", -99999d); //SNOW, WATER CONTENT, inches, 
	public static final Variable VTYPE_RAINTIP = new Variable(CommonVariable.PRECIP_RAINTIP_IN,"RAINTIP", -99999d); //PRECIPITATION, TIPPING BUCKET, inches, 
	public static final Variable VTYPE_FLOW = new Variable(CommonVariable.STREAMFLOW_CFS,"FLOW", -99999d); //FLOW, RIVER DISCHARGE, cfs, 
	public static final Variable VTYPE_RIV_STG = new Variable(CommonVariable.GAUGE_HEIGHT_FT,"RIV%20STG", -99999d); //RIVER STAGE, feet, 
	public static final Variable VTYPE_EL_COND = new Variable(CommonVariable.SPECIFIC_CONDUCTANCE_MICROSIEMEN_AT_25C,"EL%20COND", -99999d); //ELECTRICAL CONDUCTIVTY MICRO S, us/cm, 
	public static final Variable VTYPE_EL_CND = new Variable(CommonVariable.ELECTRICAL_CONDUCTIVITY_MS_CM,"EL%20CND", -99999d); //ELECTRICAL CONDUCTIVTY MILLI S, ms/cm
	public static final Variable VTYPE_TURB_W = new Variable(CommonVariable.TURBIDITY_FNU,"TURB%20W", -99999d); //WATER, TURBIDITY, ntu, 
//	public static final Variable VTYPE_SLRR_AV = new Variable(CommonVariable.,"SLRR%20AV", -99999d); //SOLAR RADIATION AVG, w/m^2, 
//	public static final Variable VTYPE_SLRR_MX = new Variable(CommonVariable.,"SLRR%20MX", -99999d); //SOLAR RADIATION MAX, w/m^2, 
//	public static final Variable VTYPE_SLRR_MN = new Variable(CommonVariable.,"SLRR%20MN", -99999d); //SOLAR RADIATION MIN, w/m^2, 
	public static final Variable VTYPE_STAGE_F = new Variable(CommonVariable.GAUGE_HEIGHT_FT,"STAGE%20F", -99999d); //FORECASTED STAGE, feet, 
	public static final Variable VTYPE_RES_ELE = new Variable(CommonVariable.RES_ELEVATION_FT,"RES%20ELE", -99999d); //RESERVOIR ELEVATION, feet, 
	public static final Variable VTYPE_OUTFLOW = new Variable(CommonVariable.RES_OUTFLOW_CFS,"OUTFLOW", -99999d); //RESERVOIR OUTFLOW, cfs, 
	public static final Variable VTYPE_STORAGE = new Variable(CommonVariable.RES_STORAGE_ACRE_FT,"STORAGE", -99999d); //RESERVOIR STORAGE, af, 
//	public static final Variable VTYPE_BAT_VOLA = new Variable(CommonVariable.,"BAT%20VOLA", -99999d); //BATTERY VOLTAGE AUX, volts, 
//	public static final Variable VTYPE_NSLR_AV = new Variable(CommonVariable.,"NSLR%20AV", -99999d); //NET SOLAR RADIATION AVG, w/m^2, 
//	public static final Variable VTYPE_NSLR_MX = new Variable(CommonVariable.,"NSLR%20MX", -99999d); //NET SOLAR RADIATION MAX, w/m^2, 
//	public static final Variable VTYPE_NSLR_MN = new Variable(CommonVariable.,"NSLR%20MN", -99999d); //NET SOLAR RADIATION MIN, w/m^2, 
//	public static final Variable VTYPE_IRR_AVG = new Variable(CommonVariable.,"IRR%20AVG", -99999d); //IRRADIANCE AVERAGE, w/m^2, 
//	public static final Variable VTYPE_IRR_MAX = new Variable(CommonVariable.,"IRR%20MAX", -99999d); //IRRADIANCE MAXIMUM, w/m^2, 
//	public static final Variable VTYPE_IRR_MIN = new Variable(CommonVariable.,"IRR%20MIN", -99999d); //IRRADIANCE MINIMUM, w/m^2, 
//	public static final Variable VTYPE_NET_RAD = new Variable(CommonVariable.,"NET%20RAD", -99999d); //NET RADIATION, cal/cm, 
//	public static final Variable VTYPE_RIRR_AV = new Variable(CommonVariable.,"RIRR%20AV", -99999d); //REFLECTED IRRADIANCE AVG, w/m^2, 
//	public static final Variable VTYPE_RIRR_MX = new Variable(CommonVariable.,"RIRR%20MX", -99999d); //REFLECTED IRRADIANCE MAX, w/m^2, 
//	public static final Variable VTYPE_RIRR_MN = new Variable(CommonVariable.,"RIRR%20MN", -99999d); //REFLECTED IRRADIANCE MIN, w/m^2, 
	public static final Variable VTYPE_DIS_OXY = new Variable(CommonVariable.DISSOLVED_O2_MG_L,"DIS%20OXY", -99999d); //WATER, DISSOLVED OXYGEN, mg/l, 
	public static final Variable VTYPE_INFLOW = new Variable(CommonVariable.RES_INFLOW_CFS,"INFLOW", -99999d); //RESERVOIR INFLOW, cfs, 
	public static final Variable VTYPE_RIVST88 = new Variable(CommonVariable.RIVER_STAGE_ELEV_FT,"RIVST88", -99999d); //RIVER STAGE NAVD88, feet
	public static final Variable VTYPE_VLOCITY = new Variable(CommonVariable.STREAM_VELOCITY_FPS,"VLOCITY", -99999d); //WATER, VELOCITY, ft/sec, 
	public static final Variable VTYPE_RIV_REL = new Variable(CommonVariable.CONTROL_REGULATING_DISCHARGE,"RIV%20REL", -99999d); //DISCHARGE,CONTROL REGULATING, cfs, 
//	public static final Variable VTYPE_CHLORPH = new Variable(CommonVariable.,"CHLORPH", -99999d); //CHLOROPHYLL, ug/l, 
	public static final Variable VTYPE_EL_CONDB = new Variable(CommonVariable.ELECTRICAL_CONDUCTIVITY_MS_CM,"EL%20CONDB", -99999d); //ELECTRICAL COND BOTTOM MICRO S, us/cm, 
	public static final Variable VTYPE_PH_VAL = new Variable(CommonVariable.WATER_PH,"PH%20VAL", -99999d); //WATER, PH VALUE, ph, 
//	public static final Variable VTYPE_T_ORG_C = new Variable(CommonVariable.,"T ORG C", -99999d); //TOTAL ORG. CARBON, OXIDATION, mg/l, 
//	public static final Variable VTYPE_SOILMD1 = new Variable(CommonVariable.,"SOILMD1", -99999d); //SOIL MOISTR, DEPTH 1, %, 
//	public static final Variable VTYPE_SOILMD2 = new Variable(CommonVariable.,"SOILMD2", -99999d); //SOIL MOISTR, DEPTH 2, %, 
//	public static final Variable VTYPE_SOILMD3 = new Variable(CommonVariable.,"SOILMD3", -99999d); //SOIL MOISTR, DEPTH 3, %, 
//	public static final Variable VTYPE_AUX_SNO = new Variable(CommonVariable.,"AUX%20SNO", -99999d); //SNOW, AUXILIARY READING, inches, 
	public static final Variable VTYPE_POOL_EL = new Variable(CommonVariable.RES_ELEVATION_FT,"RES%20EL", -99999d); //POOL ELEVATION, feet, 
//	public static final Variable VTYPE_SNOWDP2 = new Variable(CommonVariable.,"SNOWDP2", -99999d); //SNOW DEPTH AUX, inches, 
//	public static final Variable VTYPE_AVG_INF = new Variable(CommonVariable.,"AVG%20INF", -99999d); //RESERVOIR INFLOW-LAST 6HR AVG, cfs, 
//	public static final Variable VTYPE_SPILL = new Variable(CommonVariable.,"SPILL", -99999d); //DISCHARGE, SPILLWAY, cfs, 
//	public static final Variable VTYPE_DEPTH_B = new Variable(CommonVariable.,"DEPTH%20B", -99999d); //DEPTH OF READING BLW SURFACE, feet, 
//	public static final Variable VTYPE_DIS_PWR = new Variable(CommonVariable.,"DIS%20PWR", -99999d); //DISCHARGE, POWER GENERATION, cfs
//	public static final Variable VTYPE_E_T = new Variable(CommonVariable.,"E%20T", -99999d); //EVAPOTRANSPORATION, inches
//	public static final Variable VTYPE_EVP_PAN = new Variable(CommonVariable.,"EVP%20PAN", -99999d); //EVAPORATION,  PAN INCREMENT, inches
//	public static final Variable VTYPE_EVAP = new Variable(CommonVariable.,"EVAP", -99999d); //EVAPORATION, LAKE COMPUTED, cfs
//	public static final Variable VTYPE_PPT_INC = new Variable(CommonVariable.,"PPT%20INC", -99999d); //PRECIPITATION, INCREMENTAL, inches
//	public static final Variable VTYPE_SOIL_MOI = new Variable(CommonVariable.,"SOIL%20MOI", -99999d); //SOIL MOISTURE, %
	
	public Variable[] ACCEPTED_VARIABLES = new Variable[]{
		VTYPE_FLOW,
		VTYPE_RIV_STG,
		VTYPE_STAGE_F,
		VTYPE_RIVST88,
		VTYPE_RAIN,
		VTYPE_TURB_W,
		VTYPE_VLOCITY,
		VTYPE_RES_ELE,
		VTYPE_POOL_EL,
		VTYPE_INFLOW,
		VTYPE_OUTFLOW,
		VTYPE_STORAGE,
		VTYPE_RIV_REL,
		VTYPE_SOLAR_R,
		VTYPE_BAR_PRE,
		VTYPE_CB_ELEV,
		VTYPE_SNOW_DP,
		VTYPE_SNOW_WC,
		VTYPE_RAINTIP,
		VTYPE_DIS_OXY,
		VTYPE_PH_VAL,
		VTYPE_EL_COND,
		VTYPE_EL_CND,
		VTYPE_EL_CONDB,
		VTYPE_REL_HUM,
		VTYPE_WIND_DR,
		VTYPE_PEAK_WD,
		VTYPE_PEAK_WS,
		VTYPE_WIND_SP,
		VTYPE_FUEL_MS,
		VTYPE_BAT_VOL
	};

	private HttpClientWrapper httpClientWrapper = new DefaultHttpClientWrapper();
	
	@Override
	public Variable[] getAcceptedVariables() {
		return ACCEPTED_VARIABLES;
	}
	
	@Override
	public String getAgency() {
		return AGENCY;
	}
	
	@Override
	public HttpClientWrapper getHttpClientWrapper() {
		return httpClientWrapper;
	}
	
	@Override
	public Map<SiteId, SiteData> getSiteData(List<Favorite> favorites,
			boolean hardRefresh) throws ClientProtocolException, IOException {
		//TODO if this is slow, we may have to fork each request off into its own thread, like AHPSXmlDataSource
		Map<SiteId,SiteData> result = new HashMap<SiteId,SiteData>();
		Variable[] variables = new Variable[1];
		for(Favorite favorite: favorites) {
			variables[0] = getVariable(favorite.getVariable());
			if(variables[0] == null) {
				LOG.error("unknown variable: " + favorite.getVariable());
				continue;
			}
			
			SiteData newdata = getSiteData(favorite.getSite(), true, hardRefresh);
			
			SiteData existingData = result.get(favorite.getSite().getSiteId());
			
			if(existingData != null) {
				Map<CommonVariable, Series> newDataSets = newdata.getDatasets();
				existingData.getDatasets().putAll(newDataSets);
			} else {
				result.put(favorite.getSite().getSiteId(), newdata);
			}
		}
		return result;
	}
	
	@Override
	public SiteData getSiteData(Site site, Variable[] variableTypes,
			boolean hardRefresh) throws ClientProtocolException, IOException {
		return getSiteData(site, false, hardRefresh);
	}
	
	@Override
	public Variable getVariable(String variableId) {
		for(Variable v: ACCEPTED_VARIABLES) {
			if(v.getId().equals(variableId)){
				return v;
			}
		}
		return null;
	}
	
	@Override
	public void setHttpClientWrapper(HttpClientWrapper source) {
		this.httpClientWrapper = source;
	}


	/**
	 * Always assume times are in Colorado time
	 */
	private static TimeZone cdecTimeZone = TimeZone.getTimeZone("America/Sacramento");
	
	private static SimpleDateFormat startDateFormat = new SimpleDateFormat("MM/dd/yyyy+HH:mm");
	
	private SiteData getSiteData(Site site, boolean singleReading, boolean hardRefresh) throws ClientProtocolException, IOException {
		GregorianCalendar endDate = new GregorianCalendar();
		endDate.setTime(new Date());
		
		//round up to the nearest hour to keep the cache from expiring too much
		endDate.set(Calendar.MINUTE, 0);
		endDate.add(Calendar.HOUR, 1);
		
		String timespan = null;
		if(singleReading) {
			timespan = "12hours";
		} else {
			timespan = "7days";
		}
		
		endDate.setTimeZone(cdecTimeZone);
		
		String urlStr = SITE_DATA_URL + "?s=" + site.getSiteId().getId();
		urlStr += "&d=" + startDateFormat.format(endDate.getTime());
		urlStr += "&span=" + timespan;
		urlStr += "&download=y";
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + urlStr);
		
		SiteData data = null;
		InputStream contentInputStream = null;
		BufferedInputStream bufferedStream = null;
		
		try {
			long startTime = System.currentTimeMillis();
			
			HttpGet getCmd = new HttpGet(urlStr);
			HttpResponse response = httpClientWrapper.doGet(getCmd, hardRefresh);
			contentInputStream = response.getEntity().getContent();

			Header cacheFileHeader = response.getLastHeader(HttpClientWrapper.PN_CACHE_FILE);
			
			if(cacheFileHeader == null) {
				bufferedStream = new BufferedInputStream(contentInputStream, 8192);
			} else {
				File cacheFile = new File(cacheFileHeader.getValue());
				bufferedStream = new CachingBufferedInputStream(contentInputStream, 8192, cacheFile);
			}
			
			data = parse(site, bufferedStream, urlStr);
			
			if(LOG.isInfoEnabled()) LOG.info("loaded site data in " + (System.currentTimeMillis() - startTime) + "ms");
			
		} finally {
			try {
				contentInputStream.close();
				bufferedStream.close();
			} catch(NullPointerException npe) {
				//this is the result of an error which will have already been logged
			} catch(IOException ioe) {
				LOG.error("failed to close InputStream: ", ioe);
			}
		}
		
		return data;
	}
	

	private static final Pattern readingLinePattern = Pattern.compile("<tr><td.*?>(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2})</td>(.*)</tr>");
	private static final Pattern readingValuePattern = Pattern.compile("<td.*?>\\s*(\\S+)</td><td.*?><a.*?></a></td>");
	
	/**
	 * CDEC's data is in HTML, but it is so badly-formed that I parse it as plaintext
	 * 
	 *	Data Flags
	 *	A	Precipitation accumulation
	 *	L	Waiting for observer response
	 *	N	Error in data
	 *	e	Estimated
	 *	q	New rating table
	 *	r	Revised
	 *	s	New shift started
	 *	t	Trace of precipitation
	 *	v	Out of Valid Range
	 * 
	 * @param s
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private SiteData parse(Site site, InputStream s, String url) throws IOException {
		
		DataInputStream ds = new DataInputStream(s);
		
		String line;
		int lineNum = -1;

		SiteData data = new SiteData();
		data.setSite(site);
		
		StringBuilder dataInfo = new StringBuilder("<h2>" + site.getName() + " (" + site.getId() + ")</h2>");
		
		//find the table header
		 while(true) {
			line = ds.readLine();
			lineNum++;
			if(line == null) {
				throw new RuntimeException("unexpected EOF");
			}
			
			line = line.trim();
			
			if(line.trim().startsWith("<tr><td")) {
				break;
			}
		}
		
		data.setDataInfo(dataInfo.toString());

		SimpleDateFormat readingDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		CommonVariable[] columns = parseHeader(line, ds.readLine(), data, readingDateFormat);
		
		lineNum++;
		
		while((line = ds.readLine()) != null) {
			lineNum++;
			
			line = line.trim();
			
			Matcher readingMatcher = readingLinePattern.matcher(line);
			
			if(!readingMatcher.matches()) {
				LOG.info("stopped collecting readings at " + line + " " + lineNum);
				break;
			}
			
			String dateStr = readingMatcher.group(1);

			Date readingDate = null;
			
			if(dateStr != null) {
				try {
					readingDate = readingDateFormat.parse(dateStr);
				} catch(ParseException pe) {
					LOG.error("invalid date: " + dateStr + " " + lineNum);
					continue;
				}
			} else {
				LOG.error("missing date " + lineNum);
				continue;
			}

			String valuesStr = readingMatcher.group(2);

			if(valuesStr == null) {
				LOG.error("missing values " + lineNum);
				continue;
			}
			
			Matcher readingValueMatcher = readingValuePattern.matcher(valuesStr);

			for(int colIndex = 0; colIndex < columns.length; colIndex++) {

				Reading r = new Reading();
				r.setDate(readingDate);
				
				if(!readingValueMatcher.find()) {
					LOG.error("missing column " + colIndex + " on line " + lineNum);
					break;
				}
				
				String valueStr = readingValueMatcher.group(1);
				
				try {
					r.setValue(Double.valueOf(valueStr));
				} catch(NumberFormatException nfe) {
					if(valueStr.trim().length() != 0) {
						r.setQualifiers(valueStr);
					}
				} catch(NullPointerException npe) {
					LOG.error("missing value for col " + colIndex + " on line " + lineNum);
					continue;
				}
				
				data.getDatasets().get(columns[colIndex]).getReadings().add(r);
			}
		}
		
		// CDEC sometimes likes to return "--" for the last reading in the series despite the gage being operational. Remove this reading.
		Iterator<Series> datasetsI = data.getDatasets().values().iterator();
		
		while(datasetsI.hasNext()) {
			List<Reading> readings = datasetsI.next().getReadings();
			if(readings.isEmpty()) {
				continue;
			}
			
			String q = readings.get(readings.size() - 1).getQualifiers();
			if(q == null) {
				continue;
			}
			if(q.equals("--")) {
				readings.remove(readings.size() - 1);
			}
		}
		
		return data;
	}
	
	private Pattern headerLine1Pat = Pattern.compile("<tr><td.*?><font.*?> ?Date &nbsp; / &nbsp; Time &nbsp; </font></td>(.*)");
	private Pattern headerLine1VarPat = Pattern.compile("<td.*?><font.*?><i><a href=(.*?)>(.*?)</a> &nbsp</i></font></td><td.*?> &nbsp; </td>(.*)");
	private Pattern headerLine2Pat = Pattern.compile("<tr><td.*?><font.*?>\\((\\w+)\\)</font></td>.*");
	/**
	 * Populate SiteData object from table headers.
	 * @param line1
	 * @return
	 */
	private CommonVariable[] parseHeader(String line1, String line2, SiteData data, SimpleDateFormat readingDateFormat) {
		
		ArrayList<CommonVariable> result = new ArrayList<CommonVariable>();
		
		Matcher l1Matcher = headerLine1Pat.matcher(line1);
		if(!l1Matcher.matches()) {
			throw new DataParseException("header line 1 does not fit expected format: " + line1);
		}
		line1 = l1Matcher.group(1);
		
		while(true) {
			
			l1Matcher = headerLine1VarPat.matcher(line1);
			
			if(!l1Matcher.matches()) {
				break;
			}
			
			String plotUrl = l1Matcher.group(1);
			String varName = l1Matcher.group(2);
			
			line1 = l1Matcher.group(3);
			
			Variable var = getVariable(varName.replace(" ", "%20"));
			if(var == null) {
				LOG.warn("unsupported variable: " + varName);
				continue;
			}
			
			Series s = new Series();
			s.setVariable(var);
			s.setSourceUrl(AGENCY_URL + plotUrl);
			s.setReadings(new ArrayList<Reading>());
			
			StringBuilder dataInfo = new StringBuilder(data.getDataInfo());
			
			dataInfo.append("<a href=\"" + s.getSourceUrl() + "\">" + var.getName());
			if(!var.getUnit().equals("")) {
				dataInfo.append("," + var.getUnit());
			}
			dataInfo.append("</a><br/>");
			
			data.setDataInfo(dataInfo.toString());
			
			data.getDatasets().put(var.getCommonVariable(), s);
			
			result.add(var.getCommonVariable());
		}
		
		Matcher l2Matcher = headerLine2Pat.matcher(line2);
		
		if(l2Matcher.matches()) {
			String tzStr = l2Matcher.group(1);
			try {
				USTimeZone tz = USTimeZone.valueOf(tzStr);
				readingDateFormat.setTimeZone(tz.getTimeZone());
			} catch(NullPointerException npe) {
				LOG.error("error determining timezone", npe);
			}
		} else {
			readingDateFormat.setTimeZone(USTimeZone.PDT.getTimeZone());
			LOG.error("could not find timezone");
		}
		
		CommonVariable[] columns = new CommonVariable[result.size()];
		
		result.toArray(columns);
		
		return columns;
	}
}
