package com.riverflows.wsclient;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.riverflows.data.WrappedHttpResponse;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.USTimeZone;




public class AhpsKmlSiteSource {
	
	private static final Log LOG = LogFactory.getLog(AhpsKmlSiteSource.class);

	 public static final String SITE_LIST_URL = "https://water.weather.gov/ahps/worldfiles/ahps_national_fcst.kmz";

	 private HttpClientWrapper httpClientWrapper = new DefaultHttpClientWrapper();
	 
	 public List<SiteData> getSiteData() throws ClientProtocolException, IOException {
		XMLReader reader = null;
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + SITE_LIST_URL);
		
		AhpsKmlParser parser = new AhpsKmlParser();
		
		InputStream contentInputStream = null;
		//BufferedInputStream bufferedStream = null;
		ZipInputStream zipStream = null;
		
		try {
			long startTime = System.currentTimeMillis();
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://xml.org/sax/features/namespaces", true);
			factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
			
			//reader = XMLReaderFactory.createXMLReader("org.xmlpull.v1.sax2.Driver");
			reader = factory.newSAXParser().getXMLReader();
			reader.setContentHandler(parser);

			WrappedHttpResponse response = httpClientWrapper.doGet(SITE_LIST_URL, true);
			contentInputStream = response.responseStream;
			zipStream = new ZipInputStream(contentInputStream);
			ZipEntry currentEntry = null;
			while(true) {
				try {
					currentEntry = zipStream.getNextEntry();
					
					if(LOG.isInfoEnabled()) LOG.info("ZipEntry: " + currentEntry.getName());
					
					if(currentEntry.getName().equals("ahps_national_fcst.kml")) {
						break;
					}
					zipStream.closeEntry();
				} catch(IOException ioe) {
					if(currentEntry == null) {
						//couldn't find the first entry- fail loudly
						throw ioe;
					}
					LOG.debug("stopped reading zip entries", ioe);
					break;
				}
			}
			
			//bufferedStream = new BufferedInputStream(contentInputStream, 8192);
			
			
			reader.parse(new InputSource(zipStream));
			if(LOG.isInfoEnabled()) LOG.info("loaded site data in " + (System.currentTimeMillis() - startTime) + "ms");
		} catch(ParserConfigurationException pce) {
			throw new RuntimeException(pce);
		} catch(SAXException se) {
			throw new RuntimeException(se);
		} finally {
			try {
				contentInputStream.close();
				zipStream.close();
				//bufferedStream.close();
			} catch(NullPointerException npe) {
				//this is the result of an error which will have already been logged
			} catch(IOException ioe) {
				LOG.error("failed to close InputStream: ", ioe);
			}
		}
		
		return parser.sites;
	}
	
	private class AhpsKmlParser extends KmlParser {
		
		public static final String KN_SITE_ID = "NWSLID";
		public static final String KN_STATE = "State";
		public static final String KN_WATERBODY = "Waterbody";
		public static final String KN_PEAK_FORECAST = "Highest Forecast Value";
		public static final String KN_PEAK_FORECAST_TIME = "UTC Forecast Value Time";

		private final SimpleDateFormat readingDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		public AhpsKmlParser() {
			super();
			readingDateFormat.setTimeZone(USTimeZone.UTC.getTimeZone());
		}
		
		/*
		 * Typical Description String:
		 <b>NWSLID:</b> CAXM7<br>
    <b>Location:</b> Carrollton<br>
    <b>Waterbody:</b> Wakenda Creek<br>
    <b>State:</b> MO<br>
          <b>Highest Forecast Value:</b>  20.83 ft<br>
          <b>UTC Forecast Value Time:</b>  2010-09-23 00:00:00<br><b>UTC Forecast Issuance Time: </b> 2010-09-22 13:09:10
<b>Lat/Long:</b>  39.343333, -93.485556<br>
          <b>WFO:</b> Pleasant Hill/Kansas City, MO<br>
    <hr>
    <b>Major Flood Stage:</b>  21 ft <br>
    <b>Moderate Flood Stage:</b>  19 ft<br>
    <b>Flood Stage:</b>  16 ft<br>
    <b>Action Flood Stage:</b>  10 ft<br>
          <a href="http://water.weather.gov/ahps2/hydrograph.php?wfo=eax&gage=caxm7&view=1,1,1,1,1,1">Link to Gauge Hydrograph</a>

		 */
		
		public ArrayList<SiteData> sites = null;
		
		public void startDocument() throws SAXException {
			super.startDocument();
			this.sites = new ArrayList<SiteData>(6000);
		}
		
		@Override
		public void handlePlacemark(Placemark placemark) {
			HashMap<String,String> descMap = getDescriptionMap(placemark.description);
			
			SiteId newSiteId = new SiteId("AHPS",descMap.get(KN_SITE_ID));
			SiteData newSiteData = new SiteData();
			USState siteState = USState.getUSStateText(descMap.get(KN_STATE).trim());
			
			String siteName = placemark.name;
			
			if(descMap.get(KN_WATERBODY) != null && descMap.get(KN_WATERBODY).trim().length() > 0) {
				siteName = descMap.get(KN_WATERBODY) + " at " + placemark.name;
			}
			newSiteData.setSite(new Site(newSiteId, siteName, placemark.longitude, placemark.latitude, siteState,AHPSXmlDataSource.ACCEPTED_VARIABLES));
			
			sites.add(newSiteData);
			
			String peakForecastValue = descMap.get(KN_PEAK_FORECAST);
			if(peakForecastValue == null) {
				LOG.warn("missing forecast for site " + placemark.name);
				return;
			}
			
			StringTokenizer tokenizer = new StringTokenizer(peakForecastValue.trim());
			
			if(!tokenizer.hasMoreTokens()) {
				LOG.warn("unexpected peak forecast value: " + peakForecastValue);
				return;
			}
			
			peakForecastValue = tokenizer.nextToken();
			if(peakForecastValue.equals("N/A")) {
				return;
			}
			
			try {
				Series peakForecastSeries = new Series();
				peakForecastSeries.setSourceUrl("http://water.weather.gov/ahps/worldfiles/ahps_national_fcst.kmz");
				peakForecastSeries.setVariable(AHPSXmlDataSource.VTYPE_STAGE);
				
				Reading peakForecastReading = new Reading();
				Date peakForecastTime = readingDateFormat.parse(descMap.get(KN_PEAK_FORECAST_TIME));
				peakForecastReading.setDate(peakForecastTime);
				peakForecastReading.setValue(Double.parseDouble(peakForecastValue));
				
				peakForecastSeries.setReadings(Collections.singletonList(peakForecastReading));
				newSiteData.getDatasets().put(AHPSXmlDataSource.VTYPE_STAGE.getCommonVariable(), peakForecastSeries);
			} catch(NumberFormatException nfe) {
				LOG.error("invalid forecast value for site " + placemark.name, nfe);
			} catch(ParseException pe) {
				LOG.error("invalid forecast date for site " + placemark.name, pe);
			} catch(NullPointerException npe) {
				LOG.warn("missing forecast time for site " + placemark.name);
			}
		}
		
		private HashMap<String, String> getDescriptionMap(String placeDesc) {
			HashMap<String, String> descMap = new HashMap<String,String>();
			
			StringBuilder currentToken = new StringBuilder();
			String currentKey = null;
			StringBuilder currentTag = null;
			char currentChar;
			for(int charLoc = 0; charLoc < placeDesc.length(); charLoc++) {
				currentChar = placeDesc.charAt(charLoc);
				if(currentChar == '<') {
					currentTag = new StringBuilder();
					continue;
				}
				if(currentKey == null && currentChar == ':') {
					currentKey = currentToken.toString().trim();
					currentToken = new StringBuilder();
					continue;
				}
				if(currentChar == '>') {
					String currentTagStr = currentTag.toString();
					currentTag = null;
					if(currentTagStr.equals("br")) {
						descMap.put(currentKey, currentToken.toString().trim());
						currentKey = null;
						currentToken = new StringBuilder();
					}
					continue;
				}
				if(currentTag != null) {
					currentTag.append(currentChar);
					continue;
				}
				currentToken.append(currentChar);
			}
			
			return descMap;
		}
		
	}

	public HttpClientWrapper getHttpClientWrapper() {
		return httpClientWrapper;
	}

	public void setHttpClientWrapper(HttpClientWrapper httpClientWrapper) {
		this.httpClientWrapper = httpClientWrapper;
	}
}
