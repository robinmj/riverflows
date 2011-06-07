package com.riverflows.wsclient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.riverflows.data.Favorite;
import com.riverflows.data.Forecast;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USTimeZone;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

public class AHPSXmlDataSource implements RESTDataSource {
	private static final Log LOG = LogFactory.getLog(AHPSXmlDataSource.class);
	
	public static final String AGENCY = "AHPS";
	
	public static final Variable VTYPE_FLOW = new Variable(CommonVariable.STREAMFLOW_CFS, "Flow", -999000.0d);
	public static final Variable VTYPE_STAGE = new Variable(CommonVariable.GAUGE_HEIGHT_FT, "Stage", -999000.0d);
	
	public static final String SITE_DATA_URL = "http://water.weather.gov/ahps2/hydrograph_to_xml.php?";
	
	public static Variable[] ACCEPTED_VARIABLES = new Variable[] {VTYPE_FLOW, VTYPE_STAGE};

	private HttpClientWrapper httpClientWrapper = new DefaultHttpClientWrapper();
		
	@Override
	public Variable[] getAcceptedVariables() {
		return ACCEPTED_VARIABLES;
	}

	@Override
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
	public HttpClientWrapper getHttpClientWrapper() {
		return httpClientWrapper;
	}
	
	@Override
	public void setHttpClientWrapper(HttpClientWrapper source) {
		this.httpClientWrapper = source;
	}
	
	private class GetFavoriteDataThread extends Thread {
		Favorite favorite;
		IOException ioe;
		SiteData favData;
		boolean complete = false;
		
		public GetFavoriteDataThread(Favorite f) {
			this.favorite = f;
		}
		
		@Override
		public void run() {
			try {
				this.favData = getSiteData(favorite.getSite(), null, true);
				
				//limit data returned to the variables associated with the favorites
				//TODO don't throw this data away- filter in the UI instead
				Iterator<Entry<CommonVariable,Series>> datasets = this.favData.getDatasets().entrySet().iterator();
				while(datasets.hasNext()) {
					Entry<CommonVariable,Series> curDataset = datasets.next();
					
					if(favorite.getVariable().equals(curDataset.getValue().getVariable().getId())) {
						continue;
					}
					
					datasets.remove();
				}
			} catch(IOException ioe) {
				this.ioe = ioe;
			}
			this.complete = true;
		}
	}
	
	@Override
	public Map<SiteId, SiteData> getSiteData(List<Favorite> sites, boolean hardRefresh)
			throws ClientProtocolException, IOException {
		
		HashMap<SiteId, SiteData> result = new HashMap<SiteId, SiteData>();
		
		List<GetFavoriteDataThread> threads = new ArrayList<GetFavoriteDataThread>(sites.size());
		
		//call each request in a separate thread
		for(Favorite currentFav: sites) {
			GetFavoriteDataThread t = new GetFavoriteDataThread(currentFav);
			threads.add(t);
			t.start();
		}
		
		while(threads.size() > 0) {
			//poll the threads until they're all complete
			Iterator<GetFavoriteDataThread> threadsI = threads.iterator();
			while(threadsI.hasNext()) {
				GetFavoriteDataThread currentThread = threadsI.next();
				try {
					Thread.sleep(100);
				} catch(InterruptedException ie) {
					//wake up
				}
				
				if(currentThread.complete) {
					//if any of the threads experiences an IOException, abort the whole request
					// since it is probably a connectivity problem that affects all the threads
					if(currentThread.ioe != null) {
						throw currentThread.ioe;
					}
					
					result.put(currentThread.favorite.getSite().getSiteId(), currentThread.favData);
					threadsI.remove();
				}
			}
		}
		return result;
	}

	@Override
	public SiteData getSiteData(Site site, Variable[] variableTypes, boolean hardRefresh)
			throws ClientProtocolException, IOException {
		
		String urlStr = SITE_DATA_URL + "gage=" + site.getId();
		
		XMLReader reader = null;
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + urlStr);
		
		AHPSXmlParser dataSource = null;
		
		InputStream contentInputStream = null;
		BufferedInputStream bufferedStream = null;
		
		try {
			long startTime = System.currentTimeMillis();
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://xml.org/sax/features/namespaces", true);
			
			reader = factory.newSAXParser().getXMLReader();
			
			HttpGet getCmd = new HttpGet(urlStr);
			HttpResponse response = httpClientWrapper.doGet(getCmd, hardRefresh);
			
			dataSource = new AHPSXmlParser(site,urlStr);
			reader.setContentHandler(dataSource);
			
			contentInputStream = response.getEntity().getContent();

			Header cacheFileHeader = response.getLastHeader(HttpClientWrapper.PN_CACHE_FILE);
			
			if(cacheFileHeader == null) {
				bufferedStream = new BufferedInputStream(contentInputStream, 8192);
			} else {
				File cacheFile = new File(cacheFileHeader.getValue());
				bufferedStream = new CachingBufferedInputStream(contentInputStream, 8192, cacheFile);
			}
			
			reader.parse(new InputSource(bufferedStream));
			if(LOG.isInfoEnabled()) LOG.info("loaded site data in " + (System.currentTimeMillis() - startTime) + "ms");
		} catch(ParserConfigurationException pce) {
			throw new RuntimeException(urlStr, pce);
		} catch(SAXException se) {
			throw new RuntimeException(urlStr, se);
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
		
		//ensure that dataInfo gets set even if there is no standing element
		if(dataSource.resultData != null
				&& dataSource.resultData.getDataInfo() == null
				&& dataSource.resultData.getDatasets() != null
				&& dataSource.resultData.getDatasets().size() > 0) {
			String info = getDataInfo(site, dataSource.resultData.getDatasets().values().iterator().next().getSourceUrl(), null);
			dataSource.resultData.setDataInfo(info);
		}
		
		return dataSource.resultData;
	}
	
	private class AHPSXmlParser implements ContentHandler {
		public static final String EN_DISCLAIMERS = "disclaimers";
		public static final String EN_STANDING = "standing";
		public static final String EN_OBSERVED = "observed";
		public static final String EN_FORECAST = "forecast";
		public static final String EN_DATUM = "datum";
		public static final String EN_DATE = "valid";
		public static final String AN_TIMEZONE = "timezone";
		public static final String EN_PRIMARY = "primary";
		public static final String EN_SECONDARY = "secondary";
		public static final String AN_SERIES_NAME = "name";
		public static final String AN_SERIES_UNITS = "units";
		public static final String EN_PEDTS = "pedts";
		
		public SiteData resultData = new SiteData();
		
		private String srcUrl;
		
		/**
		 * The last element begin tag traversed, or null if the last tag traversed was
		 * an element closing tag.
		 */
		private String curElement;
		private String curStr;
		private SimpleDateFormat readingDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		private String currentUnits;
		private String curPrimaryVariable;
		private String curSecondaryVariable;
		private Reading curPrimaryReading;
		private Reading curSecondaryReading;
		private boolean inObservedSeries = false;
		private boolean inForecastSeries = false;
		private boolean inDisclaimer = false;
		
		public AHPSXmlParser(Site site, String srcUrl) throws FileNotFoundException{
			this.srcUrl = srcUrl;
			this.resultData.setSite(site);
		}

		@Override
		public void startDocument() throws SAXException {
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes atts) throws SAXException {
			curElement = localName;
			if(!(inForecastSeries || inObservedSeries || inDisclaimer)) {
				if(curElement.equals(EN_OBSERVED)) {
					inObservedSeries = true;
				} else if(curElement.equals(EN_FORECAST)) {
					inForecastSeries = true;
				} else if(curElement.equals(EN_DISCLAIMERS)) {
					inDisclaimer = true;
				}
				return;
			}
			
			if(curElement.equals(EN_DATUM)) {
				if(inForecastSeries) {
					curPrimaryReading = new Forecast();
					curSecondaryReading = new Forecast();
				} else {
					//in observed series
					curPrimaryReading = new Reading();
					curSecondaryReading = new Reading();
				}
				return;
			}

			
			if(curElement.equals(EN_DATE)) {
				String tzStr = atts.getValue(AN_TIMEZONE);

				//initialize the timezone
				USTimeZone usZone = USTimeZone.valueOf(tzStr);
				if(usZone != null) {
					TimeZone tz = usZone.getTimeZone();
					readingDateFormat.setTimeZone(tz);
				} else {
					LOG.warn("could not find timezone: " + tzStr);
				}
				return;
			}
			
			if(curElement.equals(EN_PRIMARY)) {
				curPrimaryVariable = atts.getValue(AN_SERIES_NAME);
				currentUnits = atts.getValue(AN_SERIES_UNITS);
				return;
			}
			
			if(curElement.equals(EN_SECONDARY)) {
				curSecondaryVariable = atts.getValue(AN_SERIES_NAME);
				currentUnits = atts.getValue(AN_SERIES_UNITS);
				return;
			}
		}
		
		private Series putReading(String name, Reading currentReading) {
			if(currentReading == null) {
				//something went wrong in collecting the reading, but has already been
				//  logged
				return null;
			}
			Variable primaryVar = getVariable(name);
			if(primaryVar == null) {
				LOG.warn("unknown primary series variable: " + name);
				return null;
			}
			
			//null out flag values
			if(primaryVar.getMagicNullValue().equals(currentReading.getValue())) {
				currentReading.setValue(null);
			}
			
			Series result = resultData.getDatasets().get(primaryVar.getCommonVariable());
			if(result == null) {
				result = new Series();
				result.setVariable(primaryVar);
				result.setSourceUrl(srcUrl);
				
				result.setReadings(new ArrayList<Reading>(500));
				resultData.getDatasets().put(primaryVar.getCommonVariable(), result);
			}
			result.getReadings().add(currentReading);
			return result;
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			//make sure we're in an element whose value we care about
			if(!inDisclaimer) {
				if(curPrimaryReading == null) {
					return;
				}
				if(curSecondaryReading == null) {
					LOG.error("secondary reading not initialized!? Ignoring datum");
					return;
				}
			} else {
				if(!curElement.equals(EN_STANDING)) {
					return;
				}
			}
			
			if(curStr == null) {
				curStr = new String(ch,start,length);
			} else {
				curStr = curStr + new String(ch,start,length);
			}
			
		}
		
		private void setReadingValue(Reading reading, String valStr) {
			
			try {
				reading.setValue(Double.valueOf(valStr));
				
				//convert kcfs to cfs
				if("kcfs".equals(currentUnits)) {
					reading.setValue(reading.getValue() * 1000.0d);
				}
				
			} catch(NumberFormatException nfe) {
				LOG.warn(valStr, nfe);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {

			if(curStr != null && curPrimaryReading != null) {
				
				String completeStr = curStr.trim();
				curStr = null;
				
				if(completeStr.length() >= 0) {
					
					if(localName.equals(EN_DATE)) {
						if(readingDateFormat == null) {
							LOG.error(EN_DATE + " element has no timezone");
							return;
						}
						
						try {
							Date readingDate = readingDateFormat.parse(completeStr);
							curPrimaryReading.setDate(readingDate);
							curSecondaryReading.setDate(readingDate);
						} catch(ParseException pe) {
							LOG.error("couldn't parse date: " + completeStr, pe);
							curPrimaryReading = null;
							curSecondaryReading = null;
						}
						return;
					}
					
					if(localName.equals(EN_PRIMARY)) {
						setReadingValue(curPrimaryReading,completeStr);
						return;
					}
		
					if(localName.equals(EN_SECONDARY)) {
						setReadingValue(curSecondaryReading, completeStr);
						return;
					}
					
					if(localName.equals(EN_PEDTS)) {
						curPrimaryReading.setQualifiers(completeStr);
						curSecondaryReading.setQualifiers(completeStr);
						return;
					}
				}
			}
			
			if(inDisclaimer) {
				if(localName.equals(EN_STANDING)  && curStr != null) {
					
					String info = getDataInfo(resultData.getSite(), srcUrl, curStr);
					
					resultData.setDataInfo(info);
					curStr = null;
				} else if(localName.equals(EN_DISCLAIMERS)) {
					inDisclaimer = false;
				}
				return;
			}
			
			//when leaving certain elements, set flags so that data collection stops until we hit another target element
			
			if(localName.equals(EN_OBSERVED)) {
				//data in the <observed> element is sorted descending by date, but the result needs to be
				// sorted ascending by date.  Thus, I need to reverse the order of the datasets.
				
				Set<Entry<CommonVariable,Series>> datasetEntries = resultData.getDatasets().entrySet();
				for(Entry<CommonVariable,Series> entry : datasetEntries) {
					Collections.reverse(entry.getValue().getReadings());
				}
				
				inObservedSeries = false;
				return;
			}
			if(localName.equals(EN_FORECAST)) {
				inForecastSeries = false;
				return;
			}
			
			if((inObservedSeries || inForecastSeries) && localName.equals(EN_DATUM)) {
				putReading(curPrimaryVariable, curPrimaryReading);
				putReading(curSecondaryVariable, curSecondaryReading);
				curPrimaryReading = null;
				curSecondaryReading = null;
			}
		}

		@Override
		public void endDocument() throws SAXException {
		}

		@Override
		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
		}

		@Override
		public void processingInstruction(String target, String data)
				throws SAXException {
		}

		@Override
		public void setDocumentLocator(Locator locator) {
		}

		@Override
		public void skippedEntity(String name) throws SAXException {
		}
	}
	
	private String getDataInfo(Site site, String srcUrl, String standing) {
		StringBuilder info = new StringBuilder();
		info.append("<h2>" + site.getName() + " (" + site.getId() + ")</h2>");
		info.append("<p>url: " + srcUrl + "</p>");
		info.append("<h4>Source: <a href=\"http://water.weather.gov/ahps/\">NOAA Advanced Hydrologic Prediction Service</a></h4>");
		if(standing != null) {
			info.append("<p><b>" + standing + "</b></p>");
		}
		return info.toString();
	}
	
}
