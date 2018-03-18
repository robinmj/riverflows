package com.riverflows.wsclient;

import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.WrappedHttpResponse;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USTimeZone;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
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

public class CDECDataSource implements RESTDataSource {
	
	private static final Log LOG = LogFactory.getLog(CDECDataSource.class);
	
	public static final String AGENCY = "CDEC";

	public static final String AGENCY_URL = "http://cdec.water.ca.gov";
	public static final String SITE_DATA_URL = AGENCY_URL + "/dynamicapp/QueryF";
	
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
	public static final Variable VTYPE_TEMP_F = new Variable(CommonVariable.AIRTEMP_F,"TEMP", -99999d); //WATER TEMPERATURE, F
	public static final Variable VTYPE_WATERTEMP_F = new Variable(CommonVariable.WATERTEMP_F,"TEMP%20W", -99999d); //WATER TEMPERATURE, F
	
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
		VTYPE_BAT_VOL,
		VTYPE_TEMP_F,
		VTYPE_WATERTEMP_F
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
	public List<FavoriteData> getSiteData(List<Favorite> favorites,
			boolean hardRefresh) throws ClientProtocolException, IOException {
		//TODO if this is slow, we may have to fork each request off into its own thread, like AHPSXmlDataSource
		List<FavoriteData> result = new ArrayList<FavoriteData>();
		HashMap<SiteId, SiteData> siteData = new HashMap<SiteId, SiteData>(favorites.size());
		Variable variable = null;
		for (Favorite favorite : favorites) {
			variable = getVariable(favorite.getVariable());
			if (variable == null) {
				LOG.error("unknown variable: " + favorite.getVariable());
				continue;
			}

			SiteData newdata = null;

			SiteData existingData = null;
			Exception exception = null;

			try {
				newdata = getSiteData(favorite.getSite(), true, hardRefresh);
			} catch (Exception e) {
				newdata = DataSourceController.dataSourceDownData(favorite.getSite(), variable);
				exception = e;
			}

			existingData = siteData.get(favorite.getSite().getSiteId());

			//each FavoriteData object returned should contain data for other favorite
			// variables at the same site, if there are any
			if (existingData != null) {
				Map<CommonVariable, Series> newDataSets = newdata.getDatasets();
				existingData.getDatasets().putAll(newDataSets);
			} else {
				existingData = (newdata != null) ? newdata
						: DataSourceController.dataSourceDownData(favorite.getSite(), variable);
				siteData.put(favorite.getSite().getSiteId(), existingData);
			}

			result.add(new FavoriteData(favorite, existingData, variable, exception));
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
	 * Always assume times are in California time
	 */
	private static TimeZone cdecTimeZone = TimeZone.getTimeZone("America/Los_Angeles");
	
	private static SimpleDateFormat startDateFormat = new SimpleDateFormat("dd-MMM-yyyy+HH:mm");
	
	static {		
		startDateFormat.setTimeZone(cdecTimeZone);
	}
	
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
		
		String urlStr = SITE_DATA_URL + "?s=" + site.getSiteId().getId();
		urlStr += "&d=" + startDateFormat.format(endDate.getTime());
		urlStr += "&span=" + timespan;
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + urlStr);
		
		SiteData data = null;
		InputStream contentInputStream = null;
		BufferedInputStream bufferedStream = null;
		
		try {
			long startTime = System.currentTimeMillis();

			WrappedHttpResponse response = httpClientWrapper.doGet(urlStr, hardRefresh);
			contentInputStream = response.responseStream;

			if(response.cacheFile == null) {
				bufferedStream = new BufferedInputStream(contentInputStream, 8192);
			} else {
				bufferedStream = new CachingBufferedInputStream(contentInputStream, 8192, response.cacheFile);
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
	

	private static final Pattern readingDatePattern = Pattern.compile("<td.*?>(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2})</td>");
	private static final Pattern readingValuePattern = Pattern.compile("<td.*?><font.*?>\\s*(\\S+)</font></td><td.*?><a.*?>(.+)</a></td>");
	
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
		
		StringBuilder dataInfo = new StringBuilder("<h2>" + StringEscapeUtils.escapeHtml(site.getName()) + " (<a href=\"http://cdec.water.ca.gov/dynamicapp/staMeta?station_id=" + site.getId() + "\">" + site.getId() + "</a>)</h2><div>");
		
		//find the table header
		 while(true) {
			line = ds.readLine();
			lineNum++;
			if(line == null) {
				throw new RuntimeException("unexpected EOF");
			}
			
			line = line.trim();
			
			if(line.trim().startsWith("<hr><h3>Hourly Data</h3><p")) {
				break;
			}
		}
		
		data.setDataInfo(dataInfo.toString());

		int[] lineCounter = new int[] { lineNum };

		SimpleDateFormat readingDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		CommonVariable[] columns = parseHeader(ds, data, readingDateFormat, lineCounter);

		seekRow(ds, lineCounter);
		
		while(true) {

			lineNum = lineCounter[0];

			String[] dateArr = readColumn(readingDatePattern, 1, ds, lineCounter);
			
			if(dateArr == null) {
				LOG.info("stopped collecting readings after " + lineNum);
				break;
			}
			
			String dateStr = dateArr[0];

			Date readingDate = null;
			
			if(dateStr != null) {
				try {
					readingDate = readingDateFormat.parse(dateStr);
				} catch(ParseException pe) {
					LOG.error("invalid date: " + dateStr + " after " + lineNum);
					continue;
				}
			} else {
				LOG.error("missing date after " + lineNum);
				continue;
			}

			lineNum = lineCounter[0];

			List<String[]> values = readRow(readingValuePattern, 2, ds, lineCounter);

			if(values == null) {
				break;
			}

			for(int colIndex = 0; colIndex < columns.length; colIndex++) {

				if(columns[colIndex] == null) {
					// Reading for unsupported variable
					continue;
				}

				if(values.size() <= colIndex) {
					LOG.error("missing col " + colIndex + " after " + lineNum);
					break;
				}

				String[] valueArr = values.get(colIndex);

				if (valueArr[0] == null) {
					LOG.error("missing value for col " + colIndex + " after " + lineNum);
					continue;
				}

				Reading r = new Reading();
				r.setDate(readingDate);
				
				StringBuilder valueFiltered = new StringBuilder(valueArr[0].length());

				for(char c : valueArr[0].trim().toCharArray()) {
					if(c == ',') {
						continue;
					}
					valueFiltered.append(c);
				}
				
				try {
					r.setValue(Double.valueOf(valueFiltered.toString()));
				} catch(NumberFormatException nfe) {
					if(valueFiltered.length() != 0) {
						r.setQualifiers(valueFiltered.toString());
					}
				}

				if(valueArr[1] != null) {
					String qualifiers = valueArr[1].trim();
					if(qualifiers.length() > 0) {
						r.setQualifiers(qualifiers);
						r.setValue(null);
					}
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

			for(int index = readings.size() - 1; index >=0; index--) {
				String q = readings.get(index).getQualifiers();
				if (q == null) {
					continue;
				}
				if (q.equals("--")) {
					readings.remove(index);
				} else {
					break;
				}
			}
		}
		
		return data;
	}

	private Pattern headerVarPat = Pattern.compile("<td.*?><font.*?><i><a href=\"(.*?)\">(.*?)</a> &nbsp</i></font></td>");
	private Pattern headerLine2Pat = Pattern.compile("<td.*?><font.*?>\\((\\w+)\\)</font></td>");

	/**
	 * Populate SiteData object from table headers.
	 * @return
	 */
	private CommonVariable[] parseHeader(DataInputStream inputStream, SiteData data, SimpleDateFormat readingDateFormat, int[] lineCounter) throws IOException {
		
		ArrayList<CommonVariable> result = new ArrayList<CommonVariable>();

		List<String[]> headerLine1 = readRow(headerVarPat, 2, inputStream, lineCounter);

		for(String[] values : headerLine1) {
			
			String plotUrl = values[0];
			String varName = values[1];
			
			Variable var = getVariable(varName.replace(" ", "%20"));
			if(var == null) {
				LOG.warn("unsupported variable: " + varName);
				result.add(null);
				continue;
			}
			
			Series s = new Series();
			s.setVariable(var);
			s.setSourceUrl(AGENCY_URL + plotUrl);
			s.setReadings(new ArrayList<Reading>());
			
			StringBuilder dataInfo = new StringBuilder(data.getDataInfo());
			
			dataInfo.append("<a href=\"" + s.getSourceUrl() + "\">");
			dataInfo.append(StringEscapeUtils.escapeHtml(var.getName()));
			if(!var.getUnit().equals("")) {
				dataInfo.append("," + StringEscapeUtils.escapeHtml(var.getUnit()));
			}
			dataInfo.append("</a><br/><br/>");
			
			data.setDataInfo(dataInfo.toString());
			
			data.getDatasets().put(var.getCommonVariable(), s);
			
			result.add(var.getCommonVariable());
		}
		
		data.setDataInfo(data.getDataInfo() + "</div>");
		
		String[] headerLine2 = readColumn(headerLine2Pat, 1, inputStream, lineCounter);
		
		if(headerLine2 != null) {
			String tzStr = headerLine2[0];
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

	private void seekRow(DataInputStream inputStream, int[] lineCounter) throws IOException {
		while(true) {
			String line = inputStream.readLine();
			if (line == null) {
				return;
			}
			lineCounter[0]++;

			if (line.contains("<tr>")) {
				break;
			}
		}
	}

	private String[] readColumn(Pattern matchingPattern, int count, DataInputStream inputStream, int[] lineCounter) throws IOException {
		List<String[]> result = readRowHelper(matchingPattern, count, inputStream, lineCounter, true);

		if(result.size() > 0) {
			return result.get(0);
		}
		return null;
	}

	private List<String[]> readRow(Pattern matchingPattern, int count, DataInputStream inputStream, int[] lineCounter) throws IOException {
		return readRowHelper(matchingPattern, count, inputStream, lineCounter, false);
	}

	private List<String[]> readRowHelper(Pattern matchingPattern, int count, DataInputStream inputStream, int[] lineCounter, boolean single) throws IOException {
		ArrayList<String[]> result = new ArrayList<>();

		while(true) {
			String line = inputStream.readLine();
			if(line == null) {
				return result;
			}
			line = line.trim();
			lineCounter[0]++;

			if (line.contains("</tr>")) {
				break;
			}

			Matcher matcher = matchingPattern.matcher(line);

			if (!matcher.matches()) {
				continue;
			}

			String[] columnValues = new String[count];

			for(int a = 1; a <= count; a++) {
				columnValues[a - 1] = matcher.group(a);
			}

			result.add(columnValues);

			if(single) {
				break;
			}
		}
		return result;
	}
	
	@Override
	public String getExternalGraphUrl(String siteId, String variableId) {
		return null;
	}
	
	@Override
	public String getExternalSiteUrl(String siteId) {
		return "http://cdec.water.ca.gov/dynamicapp/staMeta?station_id=" + siteId;
	}
}
