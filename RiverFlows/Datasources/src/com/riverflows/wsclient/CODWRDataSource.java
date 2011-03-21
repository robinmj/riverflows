package com.riverflows.wsclient;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

public class CODWRDataSource implements RESTDataSource {
	private static final Log LOG = LogFactory.getLog(CODWRDataSource.class);
	
	public static final Variable VTYPE_STREAMFLOW_CFS = new Variable(CommonVariable.STREAMFLOW_CFS,"DISCHRG", -99999d);
	public static final Variable VTYPE_STREAMFLOW_CFS_1 = new Variable(CommonVariable.STREAMFLOW_CFS,"DISCHRG1", -99999d);
	public static final Variable VTYPE_STREAMFLOW_CFS_2 = new Variable(CommonVariable.STREAMFLOW_CFS,"DISCHRG2", -99999d);
	public static final Variable VTYPE_STREAMFLOW_CFS_3 = new Variable(CommonVariable.STREAMFLOW_CFS,"DISCHRG3", -99999d);
	public static final Variable VTYPE_GAUGE_HEIGHT_FT = new Variable(CommonVariable.GAUGE_HEIGHT_FT,"GAGE_HT", -99999d);
	public static final Variable VTYPE_GAUGE_HEIGHT_FT_1 = new Variable(CommonVariable.GAUGE_HEIGHT_FT,"GAGE_HT1", -99999d);
	public static final Variable VTYPE_GAUGE_HEIGHT_FT_2 = new Variable(CommonVariable.GAUGE_HEIGHT_FT,"GAGE_HT2", -99999d);
	public static final Variable VTYPE_WATERTEMP = new Variable(CommonVariable.WATERTEMP_C,"WATTEMP", -99999d);
	public static final Variable VTYPE_AIRTEMP = new Variable(CommonVariable.AIRTEMP_F,"AIRTEMP", -99999d);
	public static final Variable VTYPE_RES_STORAGE_ACRE_FT = new Variable(CommonVariable.RES_STORAGE_ACRE_FT,"STORAGE", -99999d);
	public static final Variable VTYPE_RES_ELEVATION = new Variable(CommonVariable.RES_ELEVATION_FT,"ELEV", -99999d);
	public static final Variable VTYPE_PRECIP_IN = new Variable(CommonVariable.PRECIPITATION_TOTAL_IN,"PRECIP", -99999d);
	public static final Variable VTYPE_CONDUCTANCE = new Variable(CommonVariable.SPECIFIC_CONDUCTANCE_MICROSIEMEN_AT_25C,"COND", -99999d);
	public static final Variable VTYPE_ATM_PRESSURE_MB = new Variable(CommonVariable.ATM_PRESSURE_MM_HG, "BAR_P", -99999d);
	public static final Variable VTYPE_HUMIDITY_PCT = new Variable(CommonVariable.RELATIVE_HUMIDITY_PCT, "HUMID", -99999d);
	
	public static final Variable[] ACCEPTED_VARIABLES = new Variable[]{
		VTYPE_STREAMFLOW_CFS,
		VTYPE_STREAMFLOW_CFS_1,
		VTYPE_STREAMFLOW_CFS_2,
		VTYPE_STREAMFLOW_CFS_3,
		VTYPE_GAUGE_HEIGHT_FT,
		VTYPE_GAUGE_HEIGHT_FT_1,
		VTYPE_GAUGE_HEIGHT_FT_2,
		VTYPE_RES_STORAGE_ACRE_FT,
		VTYPE_RES_ELEVATION,
		VTYPE_PRECIP_IN,
		VTYPE_WATERTEMP,
		VTYPE_AIRTEMP,
		VTYPE_CONDUCTANCE,
		VTYPE_ATM_PRESSURE_MB,
		VTYPE_HUMIDITY_PCT};
	
	private static final SimpleDateFormat valueDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static final SimpleDateFormat rangeDateFormat = new SimpleDateFormat("MM/dd/yy");
	
	private HttpClientWrapper httpClientWrapper = new DefaultHttpClientWrapper();
	
	/**
	 * Always assume times are in Colorado time
	 */
	private TimeZone codwrTimeZone = TimeZone.getTimeZone("America/Denver");
	
	public String getAgency() {
		return "CODWR";
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
	
	public static final String SITE_DATA_URL = "http://www.dwr.state.co.us/SurfaceWater/data/export_tabular.aspx?";
	
	public void populateDataSets(SiteData site) {
		if(!site.getSite().getAgency().equals(getAgency())) {
			throw new IllegalArgumentException();
		}
		site.getDatasets().keySet();
	}
	
	@Override
	public Map<SiteId, SiteData> getSiteData(List<Favorite> favorites)
			throws ClientProtocolException, IOException {
		Map<SiteId,SiteData> result = new HashMap<SiteId,SiteData>();
		Variable[] variables = new Variable[1];
		for(Favorite favorite: favorites) {
			variables[0] = getVariable(favorite.getVariable());
			if(variables[0] == null) {
				LOG.error("unknown variable: " + favorite.getVariable());
				continue;
			}
			
			Map<SiteId,SiteData> newdata = getSiteData(favorite.getSite(), variables, 3, new Date(), null);
			
			SiteData existingData = result.get(favorite.getSite().getSiteId());
			
			if(existingData != null) {
				Map<CommonVariable, Series> newDataSets = newdata.get(favorite.getSite().getSiteId()).getDatasets();
				existingData.getDatasets().putAll(newDataSets);
			} else {
				result.putAll(newdata);
			}
		}
		return result;
	}
	
	public SiteData getSiteData(Site site, Variable[] variableTypes) throws ClientProtocolException, IOException {

		GregorianCalendar startDate = new GregorianCalendar();
		startDate.setTime(new Date());
		
		//set the start date to one week ago
		startDate.set(Calendar.WEEK_OF_YEAR, startDate.get(Calendar.WEEK_OF_YEAR) - 1);
		
		startDate.setTimeZone(codwrTimeZone);
		
		if(variableTypes.length > 2) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("CODWR doesn't accept more than 2 variables at a time- trimming variable list to " + variableTypes[0].getId() + "," + variableTypes[1].getId());
			}
			Variable[] trimmedVarTypes = new Variable[2];
			System.arraycopy(variableTypes, 0, trimmedVarTypes, 0, trimmedVarTypes.length);
			variableTypes = trimmedVarTypes;
		}
		
		Map<SiteId,SiteData> result = getSiteData(site, variableTypes, 1, startDate.getTime(), new Date());
		if(result == null) {
			return null;
		}
		return result.get(site.getSiteId());
	}
	

	public Map<SiteId,SiteData> getSiteData(Site site, Variable[] variableTypes, int interval, Date startDate, Date endDate) throws ClientProtocolException, IOException {

		String[] variableIds = new String[variableTypes.length];
		for(int a = 0; a < variableIds.length; a++) {
			variableIds[a] = variableTypes[a].getId();
		}
		
		
		String sourceUrl = SITE_DATA_URL + "ID=" + site.getId();
		sourceUrl += "&MTYPE=" + Utils.join(",", variableIds);
		sourceUrl += "&INTERVAL=" + interval;
		if(startDate != null) {
			sourceUrl += "&START=" + rangeDateFormat.format(startDate);
		}
		if(endDate != null) {
			sourceUrl += "&END=" + rangeDateFormat.format(endDate);
		}
		
		return getSiteData(site, variableTypes, sourceUrl);
	}
	
	private Map<SiteId,SiteData> getSiteData(Site site, Variable[] variables, String urlStr) throws ClientProtocolException, IOException {
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + urlStr);
		
		Map<SiteId,SiteData> data = null;
		
		InputStream contentInputStream = null;
		BufferedInputStream bufferedStream = null;
		
		try {
			long startTime = System.currentTimeMillis();
			
			HttpGet getCmd = new HttpGet(urlStr);
			HttpResponse response = httpClientWrapper.doGet(getCmd);
			contentInputStream = response.getEntity().getContent();
			bufferedStream = new BufferedInputStream(contentInputStream, 8192);
			data = parse(site, variables, bufferedStream);
			
			if(LOG.isDebugEnabled()) LOG.debug("loaded site data in " + (System.currentTimeMillis() - startTime) + "ms");
			
			//set the source URL for all datasets
			Collection<SiteData> collectedSiteData = data.values();
			Collection<Series> siteDatasets;
			for(SiteData currentSiteData:collectedSiteData) {
				siteDatasets = currentSiteData.getDatasets().values();
				for(Series currentSeries: siteDatasets) {
					currentSeries.setSourceUrl(urlStr);
				}
			}
			
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
		"Station",
		"Date/Time"
	};
	
	private Map<SiteId,SiteData> parse(Site site, Variable[] variables, InputStream s) throws IOException {

		HashMap<SiteId,SiteData> siteDataMap = new HashMap<SiteId,SiteData>();
		
		DataInputStream ds = new DataInputStream(s);
		
		String line;
		
		//find the header line
		do {
			line = ds.readLine();
			if(line == null) {
				throw new RuntimeException("unexpected EOF");
			}
		} while(line.trim().startsWith("#"));
		
		SiteData currentSiteData = null;
		List<String> titleOrder = null;
		
		do {
			if(line.charAt(0) == '#') {
				continue;
			}
			if(line.startsWith(EXPECTED_COLUMNS[0])) {
				titleOrder = new ArrayList<String>();
				currentSiteData = parseHeaders(variables, line, titleOrder);
				continue;
			}
			
			if(currentSiteData == null) {
				throw new DataParseException("missing headers");
			}
			
			String[] values = line.split("\t");
			
			String siteId = values[0];
			
			currentSiteData.setSite(site);

			siteDataMap.put(new SiteId(getAgency(), siteId), currentSiteData);
			
			String readingTimeStr = values[1];
			
			Date currentDate = null;
			
			try {
				currentDate = valueDateFormat.parse(readingTimeStr);
			} catch(ParseException pe) {
				throw new DataParseException("invalid date: " + readingTimeStr, pe);
			}
			
			//get values for each series
			for(int a = EXPECTED_COLUMNS.length; a < titleOrder.size(); a++) {
				Variable var = getVariable(titleOrder.get(a));
				if(var == null) {
					//not a series value column
					continue;
				}
				Series currentSeries = currentSiteData.getDatasets().get(var.getCommonVariable());
				
				Reading newReading = new Reading();
				try {
					newReading.setValue(Double.valueOf(values[a]));
				} catch(NumberFormatException nfe) {
					if(LOG.isDebugEnabled()) LOG.debug("couldn't parse value: " + values[a], nfe);
					newReading.setValue(null);
					newReading.setQualifiers(values[a]);
				}
				newReading.setDate(currentDate);
				
				currentSeries.getReadings().add(newReading);
			}
		}while((line = ds.readLine()) != null);
		
		return siteDataMap;
	}
	
	private SiteData parseHeaders(Variable[] variables, String headerLine, List<String> titleOrder) {
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
		
		//get variable types
		for(; a < headers.length; a++) {
			String[] pieces = headers[a].split(" ");
			
			titleOrder.add(pieces[0]);
			
			//find the full Variable object for the variable ID
			Variable v = getVariable(pieces[0]);
			if(v == null) {
				if(LOG.isWarnEnabled()) LOG.warn("unexpected variable: " + pieces[0]);
			}
			
			//create a series for each variable
			Series s = new Series();
			s.setVariable(v);
			s.setReadings(new ArrayList<Reading>());
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
}
