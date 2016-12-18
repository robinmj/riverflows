package com.riverflows.wsclient;

import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.WrappedHttpResponse;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.USTimeZone;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class UsgsCsvDataSource implements RESTDataSource {
	
	private static final Log LOG = LogFactory.getLog(UsgsCsvDataSource.class);
	
	public static final String AGENCY = "USGS";
	
	public static final Variable VTYPE_STREAMFLOW_CFS = new Variable(CommonVariable.STREAMFLOW_CFS, "00060", -99999d);
	public static final Variable VTYPE_GAUGE_HEIGHT_FT = new Variable(CommonVariable.GAUGE_HEIGHT_FT, "00065", -99999d);
	public static final Variable VTYPE_GAUGE_HEIGHT_ABOVE_DATUM_M = new Variable(CommonVariable.GAUGE_HEIGHT_ABOVE_DATUM_M, "99065", -99999d);//Gage height, above datum, m
	public static final Variable VTYPE_DEPTH_TO_WATER_LEVEL_FT = new Variable(CommonVariable.GAUGE_DEPTH_TO_WATER_LEVEL_FT, "72019", -99999d);
	public static final Variable VTYPE_OBSERVED_DEPTH_FT = new Variable(CommonVariable.OBSERVED_DEPTH_FT, "50415", -99999d);
	public static final Variable VTYPE_STREAM_VELOCITY_FPS = new Variable(CommonVariable.STREAM_VELOCITY_FPS,"00055" , -99999d);
	public static final Variable VTYPE_RES_ELEVATION = new Variable(CommonVariable.RES_ELEVATION_FT, "72020",-99999d);
	public static final Variable VTYPE_RES_ELEVATION_2 = new Variable(CommonVariable.RES_ELEVATION_FT, "62614",-99999d);
	public static final Variable VTYPE_RES_ELEVATION_3 = new Variable(CommonVariable.RES_ELEVATION_FT, "00062", -99999d); //Elevation of reservoir water surface above datum, ft
	public static final Variable VTYPE_WATER_TEMP_F = new Variable(CommonVariable.WATERTEMP_F,"00011" , -99999d);
	public static final Variable VTYPE_WATER_TEMP_C = new Variable(CommonVariable.WATERTEMP_C,"00010" , -99999d);
	public static final Variable VTYPE_PRECIPITATION_TOTAL_IN = new Variable(CommonVariable.PRECIPITATION_TOTAL_IN,"00045" , -99999d);
	public static final Variable VTYPE_AIRTEMP_C = new Variable(CommonVariable.AIRTEMP_C, "00020", -99999d);
	public static final Variable VTYPE_AIRTEMP_F = new Variable(CommonVariable.AIRTEMP_F, "00021", -99999d);
	public static final Variable VTYPE_WIND_SPEED_MPH = new Variable(CommonVariable.WIND_SPEED_MPH,"00035" , -99999d);
	public static final Variable VTYPE_WIND_SPEED_M_S = new Variable(CommonVariable.WIND_SPEED_M_S,"62625" , -99999d);
	public static final Variable VTYPE_WIND_DIRECTION_DEGREES = new Variable(CommonVariable.WIND_DIRECTION_DEGREES,"00036" , -99999d);
	public static final Variable VTYPE_WIND_GUST_SPEED_MPH = new Variable(CommonVariable.WIND_GUST_SPEED_MPH, "61728", -99999d);
	public static final Variable VTYPE_SPECIFIC_CONDUCTANCE_MICROSIEMENS = new Variable(CommonVariable.SPECIFIC_CONDUCTANCE_MICROSIEMEN_AT_25C, "00095" , -99999d);
	public static final Variable VTYPE_DISSOLVED_O2_MG_L = new Variable(CommonVariable.DISSOLVED_O2_MG_L,"00300",-99999d);
	public static final Variable VTYPE_DISSOLVED_O2_PCT = new Variable(CommonVariable.DISSOLVED_O2_PCT, "00301", -99999d);
	public static final Variable VTYPE_WATER_PH = new Variable(CommonVariable.WATER_PH,"00400",-99999d);
	public static final Variable VTYPE_TURBIDITY = new Variable(CommonVariable.TURBIDITY_FNU, "63680",-99999d);
	public static final Variable VTYPE_DCP_BATTERY_VOLTAGE = new Variable(CommonVariable.DCP_BATTERY_VOLTAGE, "70969",-99999d);
	public static final Variable VTYPE_DCP_SIGNAL_MODULATION_INDEX_DB = new Variable(CommonVariable.DCP_SIGNAL_MODULATION_INDEX_DB,"72113",-99999d);
	public static final Variable VTYPE_INTERGRAVEL_WATER_TEMP_C = new Variable(CommonVariable.INTERGRAVEL_WATERTEMP_C,"85583",-99999d);
	public static final Variable VTYPE_SAMPLE_ELEVATION_FT = new Variable(CommonVariable.SAMPLE_ELEVATION_FT,"72106",-99999d);
	public static final Variable VTYPE_ATM_PRESSURE_MM_HG = new Variable(CommonVariable.ATM_PRESSURE_MM_HG,"00025",-99999d);
	public static final Variable VTYPE_RES_STORAGE_ACRE_FT = new Variable(CommonVariable.RES_STORAGE_ACRE_FT,"00054",-99999d);
	public static final Variable VTYPE_SAMPLE_COUNT = new Variable(CommonVariable.SAMPLE_COUNT,"99234",-99999d);
	public static final Variable VTYPE_NUMBER_OF_SAMPLING_POINTS = new Variable(CommonVariable.NUMBER_OF_SAMPLING_POINTS,"00063",-99999d);
	public static final Variable VTYPE_ACOUSTIC_DOPPLER_VELOCITY_METER_SIGNAL_NOISE_RATIO = new Variable(CommonVariable.ACOUSTIC_DOPPLER_VELOCITY_METER_SIGNAL_NOISE_RATIO,"99237",-99999d);
	public static final Variable VTYPE_TIDE_ELEVATION_FT = new Variable(CommonVariable.TIDE_ELEVATION_FT,"62620",-99999d);
	public static final Variable VTYPE_RELATIVE_HUMIDITY_PCT = new Variable(CommonVariable.RELATIVE_HUMIDITY_PCT,"00052",-99999d);
	
	private HttpClientWrapper httpClientWrapper = new DefaultHttpClientWrapper();
	
	/**
	 * The order of this array affects the behavior of {@link com.riverflows.wsclient.DataSourceController#getPreferredSeries(SiteData)}
	 */
	public static final Variable[] ACCEPTED_VARIABLES = new Variable[]{
		VTYPE_STREAMFLOW_CFS,
		VTYPE_GAUGE_HEIGHT_FT,
		VTYPE_GAUGE_HEIGHT_ABOVE_DATUM_M,
		VTYPE_DEPTH_TO_WATER_LEVEL_FT,
        VTYPE_OBSERVED_DEPTH_FT,
		VTYPE_STREAM_VELOCITY_FPS,
		VTYPE_RES_STORAGE_ACRE_FT,
		VTYPE_RES_ELEVATION_3,
		VTYPE_RES_ELEVATION,
		VTYPE_RES_ELEVATION_2,
		VTYPE_WATER_TEMP_F,
		VTYPE_WATER_TEMP_C,
		VTYPE_INTERGRAVEL_WATER_TEMP_C,
		VTYPE_TIDE_ELEVATION_FT,
		VTYPE_PRECIPITATION_TOTAL_IN,
		VTYPE_AIRTEMP_F,
		VTYPE_AIRTEMP_C,
		VTYPE_WIND_SPEED_MPH,
		VTYPE_WIND_SPEED_M_S,
		VTYPE_WIND_DIRECTION_DEGREES,
		VTYPE_WIND_GUST_SPEED_MPH,
		VTYPE_SPECIFIC_CONDUCTANCE_MICROSIEMENS,
		VTYPE_DISSOLVED_O2_MG_L,
		VTYPE_DISSOLVED_O2_PCT,
		VTYPE_WATER_PH,
		VTYPE_TURBIDITY,
		VTYPE_RELATIVE_HUMIDITY_PCT,
		VTYPE_ATM_PRESSURE_MM_HG,
		VTYPE_SAMPLE_ELEVATION_FT,
		VTYPE_SAMPLE_COUNT,
		VTYPE_NUMBER_OF_SAMPLING_POINTS,
		VTYPE_ACOUSTIC_DOPPLER_VELOCITY_METER_SIGNAL_NOISE_RATIO,
		VTYPE_DCP_BATTERY_VOLTAGE,
		VTYPE_DCP_SIGNAL_MODULATION_INDEX_DB};
	
	public String getAgency() {
		return AGENCY;
	}
	
	public Variable getVariable(String id) {
		for(Variable v: ACCEPTED_VARIABLES) {
			if(v.getId().equals(id)){
				return v;
			}
		}
		return null;
	}
	
	@Override
	public Variable[] getAcceptedVariables() {
		return ACCEPTED_VARIABLES;
	}
	
	public static final String SITE_DATA_URL = "https://waterservices.usgs.gov/nwis/iv/?format=rdb,1.0&";
	
	public void populateDataSets(SiteData site) {
		if(!site.getSite().getAgency().equals(getAgency())) {
			throw new IllegalArgumentException();
		}
		site.getDatasets().keySet();
	}
	
	@Override
	public List<FavoriteData> getSiteData(List<Favorite> favorites, boolean hardRefresh)
			throws ClientProtocolException, IOException {
		Site[] sitesArray = new Site[favorites.size()];
		Variable[] vars = new Variable[favorites.size()];
		for(int a = 0; a < favorites.size(); a++) {
			sitesArray[a] = favorites.get(a).getSite();
			vars[a] = getVariable(favorites.get(a).getVariable());
		}

        ArrayList<FavoriteData> result = new ArrayList<FavoriteData>(favorites.size());

        Map<SiteId,SiteData> data = getSiteData(sitesArray, vars, true, hardRefresh);

        for(int a = 0; a < favorites.size(); a++) {

            Favorite fav = favorites.get(a);

			SiteData siteData = data.get(fav.getSite().getSiteId());

			if(siteData == null) {
				siteData = DataSourceController.dataSourceDownData(fav.getSite(), getVariable(fav.getVariable()));
				siteData.setDataInfo(getExternalSiteUrl(fav.getSite().getId()));
			}

            result.add(new FavoriteData(fav, siteData, vars[a]));
        }
		
		return result;
	}
	

	/**
	 * Download the last readings for the preferred variables of all sites in a given state
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public Map<SiteId, SiteData> getSiteData(USState state, Site[] sites, boolean hardRefresh)
			throws ClientProtocolException, IOException {
		
		String[] variableTypes = new String[ACCEPTED_VARIABLES.length];
		for(int a = 0; a < variableTypes.length; a++) {
			variableTypes[a] = ACCEPTED_VARIABLES[a].getId();
		}
		
		String sourceSite = SITE_DATA_URL + "stateCd=" + state.getAbbrev();
		sourceSite += "&parameterCd=" + Utils.join(",", variableTypes);

		return getSiteData(sites, ACCEPTED_VARIABLES, sourceSite, hardRefresh);
	}
	
	@Override
	public SiteData getSiteData(Site site,
			Variable[] variableTypes, boolean hardRefresh) throws ClientProtocolException,
			IOException {
		Map<SiteId,SiteData> result = getSiteData(new Site[]{site}, variableTypes, false, hardRefresh);
		if(result == null) {
			return null;
		}
		return result.get(site.getSiteId());
	}

	public Map<SiteId,SiteData> getSiteData(Site[] sites, Variable[] variableTypes, boolean singleReading, boolean hardRefresh) throws ClientProtocolException, IOException {
		
		//remove duplicate sites
		Set<String> siteIdsSet = new HashSet<String>(sites.length);
		for(int a = 0; a < sites.length; a++) {
			siteIdsSet.add(sites[a].getId());
		}
		String[] siteIds = new String[siteIdsSet.size()];
		siteIdsSet.toArray(siteIds);

		//remove duplicate variables, which will cause a server error
		Set<String> variableIdsSet = new HashSet<String>(variableTypes.length);
		for(int a = 0; a < variableTypes.length; a++) {
			variableIdsSet.add(variableTypes[a].getId());
		}
		String[] variableIds = new String[variableIdsSet.size()];
		variableIdsSet.toArray(variableIds);
		
		String sourceUrl = SITE_DATA_URL + "sites=" + Utils.join(",",siteIds);
		sourceUrl += "&parameterCd=" + Utils.join(",", variableIds);
		
		if(!singleReading) {
			sourceUrl += "&period=P7D";
		}
		
		return getSiteData(sites, variableTypes, sourceUrl, hardRefresh);
	}
	
	/*
	public Map<String,SiteData> getSiteData(USState state, String[] variableTypes) throws ClientProtocolException, IOException {
		
		String sourceSite = SITE_DATA_URL + "stateCd=" + state.getAbbrev();
		sourceSite += "&parameterCd=" + Utils.join(",", variableTypes);
		
		return getSiteData(sourceSite);
	}*/
	
	private Map<SiteId,SiteData> getSiteData(Site[] sites, Variable[] variables, String urlStr, boolean hardRefresh) throws ClientProtocolException, IOException {
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + urlStr);
		
		Map<SiteId,SiteData> data = null;
		
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
			
			data = parse(sites, variables, bufferedStream, urlStr);
			
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
	
	private static final String[] EXPECTED_COLUMNS = new String[] {
		"agency_cd",
		"site_no",
		"datetime",
		"tz_cd"
	};
	
	private Map<SiteId,SiteData> parse(Site[] sites, Variable[] variables, InputStream s, String sourceUrl) throws IOException {

		HashMap<SiteId,SiteData> siteDataMap = new HashMap<SiteId,SiteData>();
		
		DataInputStream ds = new DataInputStream(s);
		
		String line;
		
		StringBuilder dataInfo = new StringBuilder();
		dataInfo.append("<div><strong>Warning:</strong>");
		
		//find the header line, save boilerplate comment
		 while(true) {
			line = ds.readLine();
			if(line == null) {
				throw new RuntimeException("unexpected EOF");
			}
			if(!line.trim().startsWith("#")) {
				break;
			}
			
			if(line.startsWith("# ------")) {
				dataInfo.append("<hr/>");
				continue;
			}
			//put heavily indented info on its own line
			if(line.startsWith("#    ")) {
				dataInfo.append("<p>" + line.substring(5) + "</p>");
				continue;
			}
			//put label: value info on its own line
			if(line.contains(":")) {
				dataInfo.append("<p>" + line.substring(1) + "</p>");
				continue;
			}
			
			if(line.trim().equals("#")) {
				dataInfo.append("<br/>");
				continue;
			}
			
			dataInfo.append(line.substring(1));
		}
		
		dataInfo.append("</div>");
		dataInfo.append("<p>url: " + sourceUrl + "</p>");
		
		if(LOG.isDebugEnabled()) LOG.debug( "boilerplate end");
		
		SiteData currentSiteData = null;
		List<String> titleOrder = null;
		
		long lineCount = 0;
		
		long dateParseTimeCount = 0;
		
		//looking up the timezone is a spendy operation.  So,
		// I'll parse/resolve the timezone only once per site and then associate it
		// with the SimpleDateFormat so it is appended to all the dates.
		SimpleDateFormat valueDateFormat = null;
		
		do {
			if(line.length() == 0 || line.charAt(0) == '#') {
				continue;
			}
			lineCount++;
			if(line.startsWith(EXPECTED_COLUMNS[0])) {
				
				if(LOG.isDebugEnabled()) LOG.debug( "found site header");
				
				titleOrder = new ArrayList<String>();
				currentSiteData = parseHeaders(variables, line, titleOrder, sourceUrl, dataInfo.toString());
				
				//changing sites, ergo changing timezones
				valueDateFormat = null;
				
				if(ds.readLine() == null) {
					if(LOG.isWarnEnabled()) LOG.warn( "missing parsing instructions");
				}

				if(LOG.isDebugEnabled()) LOG.debug( "parsed site header");
				continue;
			}
			
			if(currentSiteData == null) {
				throw new DataParseException("missing headers");
			}
			
			String[] values = Utils.split(line, '\t');
			
			String siteId = values[1];
			
			Site currentSite = null;
			
			//get the Station object that this siteId references
			for(int a = 0; a < sites.length; a++) {
				if(sites[a].getId().equals(siteId)) {
					currentSite = sites[a];
					break;
				}
			}
			
			if(currentSite == null) {
				if(LOG.isWarnEnabled()) LOG.warn( "unexpected site: " + siteId);
			}
			
			currentSiteData.setSite(currentSite);

			siteDataMap.put(new SiteId(getAgency(), siteId), currentSiteData);
			
			Date currentDate = null;
			
			long dateParseStartTime = System.currentTimeMillis();
			
			if(valueDateFormat == null) {
				valueDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

				//initialize the timezone
				USTimeZone usZone = USTimeZone.valueOf(values[3]);
				if(usZone != null) {
					TimeZone tz = usZone.getTimeZone();
					valueDateFormat.setTimeZone(tz);
				} else {
					LOG.warn("could not find timezone: " + values[3]);
				}
			}
			try {
				currentDate = valueDateFormat.parse(values[2]);
			} catch(ParseException pe) {
				throw new DataParseException("invalid date: " + values[2], pe);
			} catch(ArrayIndexOutOfBoundsException aioobe) {
				throw new DataParseException("missing value date column");
			}
			
			dateParseTimeCount += (System.currentTimeMillis() - dateParseStartTime);
			
			if(titleOrder.size() > values.length) {
				LOG.warn("unexpected number of value columns. found: " + values.length + " columns in \"" + line + "\", but expected " + titleOrder.size());
			}
			
			//get values for each series
			for(int a = EXPECTED_COLUMNS.length; a < titleOrder.size(); a++) {
				Variable var = getVariable(titleOrder.get(a));
				if(var == null) {
					//not a series value column
					continue;
				}
				
				Series currentSeries = currentSiteData.getDatasets().get(var.getCommonVariable());

                if(currentSeries == null) {
                    throw new NullPointerException("dataset for " + var.getCommonVariable() + " not found");
                }
				
				Reading newReading = new Reading();
				try {
					Double value = Double.valueOf(values[a]);
					if(currentSeries.getVariable().getMagicNullValue().equals(value)) {
						value = null;
					}
					
					newReading.setValue(value);
				} catch(NumberFormatException nfe) {
					if(LOG.isDebugEnabled()) LOG.debug("couldn't parse value: " + values[a]);
					newReading.setValue(null);
					newReading.setQualifiers(values[a]);
				} catch(ArrayIndexOutOfBoundsException aioobe) {
					throw new DataParseException("missing reading column " + a);
				}
				newReading.setDate(currentDate);
				
				currentSeries.getReadings().add(newReading);
			}
		}while((line = ds.readLine()) != null);

		
		if(LOG.isDebugEnabled()) LOG.debug("parsed dates in " + dateParseTimeCount);
		if(LOG.isDebugEnabled()) LOG.debug("parsed " + lineCount + " lines");
		
		return siteDataMap;
	}
	
	private SiteData parseHeaders(Variable[] variables, String headerLine, List<String> titleOrder, String sourceUrl, String dataInfo) {
		String[] headers = headerLine.split("\t");
		if(headers.length <= EXPECTED_COLUMNS.length) {
			throw new DataParseException("missing reading column(s)");
		}
		int a = 0;
		for(; a < EXPECTED_COLUMNS.length; a++) {
			if(!headers[a].equals(EXPECTED_COLUMNS[a])) {
				throw new DataParseException("header in unexpected location: " + headers[a]);
			}
			titleOrder.add(headers[a]);
		}
		
		SiteData currentSiteData = new SiteData();
		currentSiteData.setDataInfo(dataInfo);
		
		//get variable types
		for(; a < headers.length; a++) {
			String[] pieces = headers[a].split("_");
			
			if(pieces.length > 2) {
				titleOrder.add(headers[a]);
				continue;
			}
			
			titleOrder.add(pieces[1]);

			Variable v = null;
			
			//find the full Variable object for the variable ID
			for(int b = 0; b < variables.length; b++) {
				if(variables[b].getId().equals(pieces[1])) {
					v = variables[b];
					break;
				}
			}
			if(v == null) {
				if(LOG.isWarnEnabled()) LOG.warn( "unexpected variable: " + pieces[1]);
				v = getVariable(pieces[1]);
			}
			
			//create a series for each variable
			Series s = new Series();
			s.setVariable(v);
			s.setReadings(new ArrayList<Reading>());
			s.setSourceUrl(sourceUrl);
			currentSiteData.getDatasets().put(v.getCommonVariable(), s);
		}
		
		return currentSiteData;
	}

	@Override
	public HttpClientWrapper getHttpClientWrapper() {
		return httpClientWrapper;
	}

	@Override
	public void setHttpClientWrapper(HttpClientWrapper httpClientSource) {
		this.httpClientWrapper = httpClientSource;
	}
	
	@Override
	public String getExternalGraphUrl(String siteId, String variableId) {
		return "http://waterdata.usgs.gov/nwisweb/graph?agency_cd=USGS&site_no=" + siteId + "&parm_cd=" + variableId + "&period=7";
	}
	
	@Override
	public String getExternalSiteUrl(String siteId) {
		return "http://waterdata.usgs.gov/nwis/inventory?agency_code=USGS&site_no=" + siteId;
	}
}
