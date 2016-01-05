package com.riverflows.wsclient;

import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

public class USACEDataSource implements RESTDataSource {
	
	private static final Log LOG = LogFactory.getLog(USACEDataSource.class);
	
	public static final String SITE_DATA_URL = "http://rivergages.mvr.usace.army.mil/WaterControl/";
	
	public static final Variable STAGE = new Variable(CommonVariable.GAUGE_HEIGHT_FT, "HG", -1d, "Stage (Ft)");
	public static final Variable ELEVATION = new Variable(CommonVariable.RES_ELEVATION_FT, "E", -1d, "Elevation (Ft)");
	public static final Variable CUMULATIVE_PRECIP = new Variable(CommonVariable.PRECIPITATION_TOTAL_IN, "PC", -1d, "Cumulative Precipitation (In)");
	public static final Variable AIRTEMP_F = new Variable(CommonVariable.AIRTEMP_F, "TA", -1d, "Air Temperature (F)");
	public static final Variable BATTERY_VOLTAGE = new Variable(CommonVariable.DCP_BATTERY_VOLTAGE, "VB", -1d, "Battery Voltage (V)");
	public static final Variable ELEVATION_LAKE = new Variable(CommonVariable.RES_ELEVATION_FT, "HL", -1d, "Elevation, Natural Lake");
	public static final Variable WATERTEMP_F = new Variable(CommonVariable.WATERTEMP_F, "TW", -1d, "Water Temperature (F)");
	public static final Variable ROLLER_LEVEL_FT = new Variable(CommonVariable.ROLLER_LEVEL_FT, "YR", -1d, "Roller (Ft)");
	public static final Variable POOL_LEVEL = new Variable(CommonVariable.GAUGE_HEIGHT_FT, "HP", -1d, "Pool Level (Ft)");
	public static final Variable FLOW = new Variable(CommonVariable.STREAMFLOW_CFS, "QR", -1d, "Flow (CFS)");
	
	private HttpClientWrapper httpClientWrapper = new DefaultHttpClientWrapper();
	
	private static final Map<String,TimeZone> timeZoneMap = new HashMap<String,TimeZone>();
	
	static {
		timeZoneMap.put("Eastern Time Zone", TimeZone.getTimeZone("US/Eastern"));
		timeZoneMap.put("Central Time Zone", TimeZone.getTimeZone("US/Central"));
		timeZoneMap.put("Mountain Time Zone", TimeZone.getTimeZone("US/Mountain"));
		timeZoneMap.put("Pacific Time Zone", TimeZone.getTimeZone("US/Pacific"));
	}
	
	public static final Variable[] ACCEPTED_VARIABLES = new Variable[]{
		STAGE,
		ELEVATION,
		CUMULATIVE_PRECIP,
		AIRTEMP_F,
		BATTERY_VOLTAGE,
		ELEVATION_LAKE,
		WATERTEMP_F,
		ROLLER_LEVEL_FT,
		POOL_LEVEL,
		FLOW
	};
	
	public static final String AGENCY = "USACE";

	@Override
	public String getAgency() {
		return AGENCY;
	}

	@Override
	public Variable getVariable(String variableId) {
		for(Variable var: ACCEPTED_VARIABLES) {
			if(var.getId().equals(variableId)) {
				return var;
			}
		}
		return null;
	}
	
	private Variable getVariableByExactName(String exactName) {
		for(Variable var: ACCEPTED_VARIABLES) {
			if(exactName.equals(var.getExactName())) {
				return var;
			}
		}
		return null;
	}

	@Override
	public Variable[] getAcceptedVariables() {
		return ACCEPTED_VARIABLES;
	}

	@Override
	public List<FavoriteData> getSiteData(List<Favorite> favorites,
			boolean hardRefresh) throws ClientProtocolException, IOException {
			//TODO if this is slow, we may have to fork each request off into its own thread, like AHPSXmlDataSource
			List<FavoriteData> result = new ArrayList<FavoriteData>();
            HashMap<SiteId, SiteData> siteData = new HashMap<SiteId, SiteData>(favorites.size());
			Variable variable = null;
			for(Favorite favorite: favorites) {
				variable = getVariable(favorite.getVariable());
				if(variable == null) {
					LOG.error("unknown variable: " + favorite.getVariable());
					continue;
				}

				SiteData newdata = null;
				Exception exception = null;

				try {
					newdata = getSiteData(favorite.getSite(), variable, getSiteDataUrl(favorite.getSite().getId(), variable, 1), hardRefresh);
				} catch(Exception e) {
					newdata = DataSourceController.dataSourceDownData(favorite.getSite(), variable);
					exception = e;
				}
				
				SiteData existingData = siteData.get(favorite.getSite().getSiteId());

                //each FavoriteData object returned should contain data for other favorite
                // variables at the same site, if there are any
				if(existingData != null) {
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
	
	private String getSiteDataUrl(String siteId, Variable var, int days) {
		StringBuilder url = new StringBuilder(SITE_DATA_URL);
		url.append("shefdata2.cfm?sid=").append(siteId);
		url.append("&d=" + days);
		if((var.getCommonVariable() == CommonVariable.GAUGE_HEIGHT_FT && !var.equals(STAGE)) || var.equals(ELEVATION)) {
			//request elevation instead of stage if the user specifically requested elevation or
			// they requested a different GAUGE_HEIGHT_FT variable than stage
			url.append("&dt=").append("E");
		} else {
			url.append("&dt=").append("S");
		}
		return url.toString();
	}

	@Override
	public SiteData getSiteData(Site site, Variable[] variableTypes,
			boolean hardRefresh) throws ClientProtocolException, IOException {
		SiteData data = null;
		for(Variable var:variableTypes) {
			SiteData newData = getSiteData(site, var, getSiteDataUrl(site.getId(), var, 7), hardRefresh);
			if(data == null) {
				data = newData;
			} else if(newData != null){
				data.getDatasets().putAll(newData.getDatasets());
			}
		}
		return data;
	}

	@Override
	public String getExternalSiteUrl(String siteId) {
		return SITE_DATA_URL + "stationrecords.cfm?sid=" + siteId;
	}

	@Override
	public String getExternalGraphUrl(String siteId, String variableId) {
		return null;
	}

	@Override
	public HttpClientWrapper getHttpClientWrapper() {
		return httpClientWrapper;
	}

	@Override
	public void setHttpClientWrapper(HttpClientWrapper source) {
		this.httpClientWrapper = source;
	}

	private SiteData getSiteData(Site sites, Variable variable, String urlStr, boolean hardRefresh) throws ClientProtocolException, IOException {
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + urlStr);
		
		SiteData data = null;
		
		InputStream contentInputStream = null;
		BufferedInputStream bufferedStream = null;
		
		try {
			long startTime = System.currentTimeMillis();
			
			HttpGet getCmd = new HttpGet(urlStr);
			HttpResponse response = httpClientWrapper.doGet(getCmd, hardRefresh);
			
			if(response.getStatusLine().getStatusCode() != 200) {
				throw new IOException(response.getStatusLine() + " response from " + urlStr);
			}
			
			contentInputStream = response.getEntity().getContent();

			Header cacheFileHeader = response.getLastHeader(HttpClientWrapper.PN_CACHE_FILE);
			
			if(cacheFileHeader == null) {
				bufferedStream = new BufferedInputStream(contentInputStream, 8192);
			} else {
				File cacheFile = new File(cacheFileHeader.getValue());
				bufferedStream = new CachingBufferedInputStream(contentInputStream, 8192, cacheFile);
			}
			
			data = parse(sites, variable, bufferedStream, urlStr);
			
			if(LOG.isInfoEnabled()) LOG.info("loaded site data in " + (System.currentTimeMillis() - startTime) + "ms");
		} catch(Exception e) {
			LOG.error("",e);
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

	private SiteData parse(Site site, Variable variable, InputStream s, String sourceUrl) throws IOException, ParserConfigurationException, SAXException {
		SiteData data = new SiteData();
		data.setSite(site);
		
		Document doc = Jsoup.parse(s, "iso-8859-1", sourceUrl, Parser.htmlParser());
		
		Elements timezoneRow = doc.select("form[name=frm_daily] > table > tbody > tr:eq(2)");
		
		LOG.info("timezoneRow: " + timezoneRow);
		
		String timezone = timezoneRow.first().text().trim();
		
		LOG.info("timezone: " + timezone);
		
		TimeZone zone = timeZoneMap.get(timezone);
		
		if(zone == null) {
			throw new DataParseException("no timezone found for " + timezone);
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		dateFormat.setTimeZone(zone);

		Elements infoRow1 = doc.select("form[name=frm_daily] > table > tbody > tr:eq(0) td");
		Elements infoRow2 = doc.select("form[name=frm_daily] > table > tbody > tr.style11:gt(4) td");
		
		StringBuilder dataInfo = new StringBuilder();
		dataInfo.append("<h2> <a href=\"" + getExternalSiteUrl(site.getId()) + "\">" + site.getName() + " (" + site.getId() + ")</a></h2>");
		dataInfo.append("<h4>Source: <a href=\"");
		dataInfo.append(SITE_DATA_URL).append("new/layout.cfm");
		dataInfo.append("\">RiverGages.com - United States Army Corps of Engineers</a></h4>");
		dataInfo.append("<h5><a href=\"").append(sourceUrl).append("\">original data</a></h5><br/>");
		
		if(infoRow1.first() != null) {
			dataInfo.append(infoRow1.first().html());
		} else {
			LOG.warn("could not find data info row 1");
		}
		
		if(infoRow2.first() != null) {
			String info2 = infoRow2.first().html();
			
			//convert to absolute URLs
			dataInfo.append(info2.replace("/WaterControl/", SITE_DATA_URL));
		} else {
			LOG.warn("could not find data info row 2");
		}
		
		if(LOG.isDebugEnabled()) LOG.debug("Data Info:" + dataInfo);
		
		data.setDataInfo(dataInfo.toString());
		
		Elements headerColumns = doc.select("form[name=frm_daily] > table > tbody > tr.style2 td");
		
		if(LOG.isInfoEnabled()) LOG.info("header columns: " + headerColumns.html());
		
		Elements dataRows = doc.select("form[name=frm_daily] > table > tbody > tr.style3");
		
		Variable[] columns = parseColumns(headerColumns, data, dataRows.size(), sourceUrl);
		
		for(int rowIndex = (dataRows.size() - 1); rowIndex >= 0; rowIndex--) {
			Element row = dataRows.get(rowIndex);
			
			if(LOG.isInfoEnabled()) LOG.info(row);
			
			Elements dataColumns = row.select("td");
			
			if(dataColumns.size() != columns.length) {
				LOG.error("unexpected number of columns in row " + rowIndex);
				continue;
			}
			
			String dateStr = dataColumns.first().text().trim();
			Date date = null;
			
			try {
				date = dateFormat.parse(dateStr);
			} catch(ParseException pe) {
				LOG.error("couldn't parse date: " + dateStr,pe);
				continue;
			}
			
			for(int colIndex = 1; colIndex < dataColumns.size(); colIndex++) {
				
				if(columns[colIndex] == null) {
					//ignore column- this error has already been logged
					continue;
				}
				
				String valueStr = dataColumns.get(colIndex).text().trim();
				
				if(LOG.isInfoEnabled()) LOG.info(dateStr + " " + valueStr + " (" + columns[colIndex] + ")");
				
				Reading r = new Reading();
				
				try {
					r.setValue(Double.valueOf(valueStr));
				} catch(NumberFormatException nfe) {
					if(LOG.isInfoEnabled()) LOG.info("couldn't parse value: " + valueStr);
					r.setQualifiers(valueStr);
				}
				
				r.setDate(date);
				
				Series series = data.getDatasets().get(columns[colIndex].getCommonVariable());

				series.getReadings().add(r);
			}
			
		}
		
	    return data;
	}
	
	private Variable[] parseColumns(Elements headerColumns, SiteData data, int dataRowCount, String sourceUrl) {
		Variable[] columns = new Variable[headerColumns.size()];
		
		if(LOG.isInfoEnabled()) LOG.info("header column count: " + columns.length);
		
		//skip first column because it is the Date/Time column
		for(int colIndex = 1; colIndex < headerColumns.size(); colIndex++) {
			columns[colIndex] = getVariableByExactName(headerColumns.get(colIndex).text());
			if(columns[colIndex] == null) {
				LOG.warn("no variable found for " + headerColumns.get(colIndex).text());
				continue;
			}
			
			Series s = new Series();
			s.setSourceUrl(sourceUrl);
			s.setVariable(columns[colIndex]);
			s.setReadings(new ArrayList<Reading>(dataRowCount));
			
			data.getDatasets().put(columns[colIndex].getCommonVariable(), s);
		}
		
		return columns;
	}
}
