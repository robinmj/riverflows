package com.riverflows.wsclient;

import java.io.BufferedInputStream;
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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;

public class UsgsXmlDataSource implements ContentHandler {
	
	public static final String SITE_DATA_URL = "http://waterservices.usgs.gov/nwis/iv?format=waterml,1.1&";
	
	private static final Log LOG = LogFactory.getLog(UsgsXmlDataSource.class);
	
	public static final USState[] STATES_SORTED_BY_CODE = new USState[] {null,//0 is not used
		USState.AL,
		USState.AK,
		null,//American Samoa
		USState.AZ,
		USState.AR,
		USState.CA,
		null,//unknown
		USState.CO,
		USState.CT,
		USState.DE,
		USState.DC,
		USState.FL,
		USState.GA,
		null,//Guam
		USState.HI,
		USState.ID,
		USState.IL,
		USState.IN,
		USState.IA,
		USState.KS,
		USState.KY,
		USState.LA,
		USState.ME,
		USState.MD,
		USState.MA,
		USState.MI,
		USState.MN,
		USState.MS,
		USState.MO,
		USState.MT,
		USState.NE,
		USState.NV,
		USState.NH,
		USState.NJ,
		USState.NM,
		USState.NY,
		USState.NC,
		USState.ND,
		USState.OH,
		USState.OK,
		USState.OR,
		USState.PA,
		null,
		USState.RI,
		USState.SC,
		USState.SD,
		USState.TN,
		USState.TX,
		USState.UT,
		USState.VT,
		USState.VA,
		null,
		USState.WA,
		USState.WV,
		USState.WI,
		USState.WY,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		USState.PR,};
	
	/**
	 * Retrieve a week's worth of readings for the given variables at the given sites.
	 * @param siteIds
	 * @param variableTypes
	 * @param singleReading if true, only get the most recent reading for each variable type
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Map<String,SiteData> getSiteData(String[] siteIds, String[] variableTypes, boolean singleReading) throws ClientProtocolException, IOException {
		
		String sourceSite = SITE_DATA_URL + "sites=" + Utils.join(",",siteIds);
		sourceSite += "&parameterCd=" + Utils.join(",", variableTypes);
		
		if(!singleReading) {
			sourceSite += "&period=P7D";
		}
		
		return getSiteData(sourceSite, singleReading);
	}
	
	public static Map<String,SiteData> getSiteData(USState state, String[] variableTypes) throws ClientProtocolException, IOException {
		
		String sourceSite = SITE_DATA_URL + "stateCd=" + state.getAbbrev();
		
		if(variableTypes != null) {
			sourceSite += "&parameterCd=" + Utils.join(",", variableTypes);
		}
		
		return getSiteData(sourceSite, true);
	}
	
	static Map<String,SiteData> getSiteData(String urlStr, boolean singleReading) throws ClientProtocolException, IOException {
		XMLReader reader = null;
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + urlStr);
		
		UsgsXmlDataSource dataSource = new UsgsXmlDataSource(singleReading);
		
		InputStream contentInputStream = null;
		BufferedInputStream bufferedStream = null;
		
		try {
			long startTime = System.currentTimeMillis();
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://xml.org/sax/features/namespaces", true);
			factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
			
			//reader = XMLReaderFactory.createXMLReader("org.xmlpull.v1.sax2.Driver");
			reader = factory.newSAXParser().getXMLReader();
			reader.setContentHandler(dataSource);

			//URL url = new URL(urlStr);
			//URLConnection connection = url.openConnection();
			//InputStream contentInputStream = connection.getInputStream();
			
			HttpClient client = new DefaultHttpClient();
			HttpGet getCmd = new HttpGet(urlStr);
			HttpResponse response = client.execute(getCmd);
			contentInputStream = response.getEntity().getContent();
			bufferedStream = new BufferedInputStream(contentInputStream, 8192);
			
			reader.parse(new InputSource(bufferedStream));
			if(LOG.isInfoEnabled()) LOG.info("loaded site data in " + (System.currentTimeMillis() - startTime) + "ms");
		} catch(ParserConfigurationException pce) {
			throw new RuntimeException(pce);
		} catch(SAXException se) {
			throw new RuntimeException(se);
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
		
		return dataSource.siteData;
	}
	
	private Map<String,SiteData> siteData;
	private SiteData currentSiteData;
	private Series currentTimeSeries;
	private String currentNote;
	
	public static String waterMLNamespaceURI = "http://www.cuahsi.org/waterML/1.1/";
	
	public static final HashSet<String> elementNames = new HashSet<String>();
	
	public static int INITIAL_READING_CAPACITY = 700;
	private static SimpleDateFormat valueDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ");

	//IMPORTANT remember to add new elements to the elementNames set in the static constructer
	
	private static String EN_TIMESERIES = "timeSeries";
	private static String EN_SOURCE_INFO = "sourceInfo";
	private static String EN_SITE_NAME = "siteName";
	private static String EN_SITE_CODE = "siteCode";
	private static String EN_NOTE = "note";
	private static String AN_NOTE_TITLE = "title";
	private static String NOTE_TITLE_STATE_CODE = "stateCd";
	private static String EN_SITE_LOCATION = "geogLocation";
	private static String EN_SITE_LATITUDE = "latitude";
	private static String EN_SITE_LONGITUDE = "longitude";
	private static String EN_VARIABLE = "variable";
	private static String EN_VARIABLE_NAME = "variableName";
	private static String EN_VARIABLE_TYPE = "valueType";
	private static String EN_VARIABLE_MAGIC_NULL_VALUE = "noDataValue";
	private static String EN_VALUES = "values";
	private static String EN_VALUE = "value";
	private static String EN_VALUE_TIME = "dateTime";
	
	static {
		//performance:
		elementNames.add(EN_TIMESERIES);
		elementNames.add(EN_SOURCE_INFO);
		elementNames.add(EN_SITE_NAME);
		elementNames.add(EN_SITE_CODE);
		elementNames.add(EN_NOTE);
		elementNames.add(EN_SITE_LOCATION);
		elementNames.add(EN_SITE_LATITUDE);
		elementNames.add(EN_SITE_LONGITUDE);
		elementNames.add(EN_VARIABLE);
		elementNames.add(EN_VARIABLE_NAME);
		elementNames.add(EN_VARIABLE_TYPE);
		elementNames.add(EN_VARIABLE_MAGIC_NULL_VALUE);
		elementNames.add(EN_VALUES);
		elementNames.add(EN_VALUE);
		elementNames.add(EN_VALUE_TIME);
	}
	
	public static Date parseDate(String dateStr) throws ParseException {

		//remove colon in the timezone, which doesn't adhere to RFC 822
		int tzColonLoc = dateStr.lastIndexOf(':');
		dateStr = dateStr.substring(0, tzColonLoc) + dateStr.substring(tzColonLoc + 1);
		
		return valueDateFormat.parse(dateStr);
	}
	
	private final boolean singleReading;
	
	public UsgsXmlDataSource(boolean singleReading) {
		this.singleReading = singleReading;
	}
	
	/**
	 * The last element begin tag traversed, or null if the last tag traversed was
	 * an element closing tag.
	 */
	private String curElement;
	
	@Override
	public void startDocument() throws SAXException {
		siteData = new HashMap<String, SiteData>();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		/*REMOVED FOR PERFORMANCE REASONS
		//not in the WaterML namespace for whatever reason- don't try processing any elements
		if(!uri.equals(waterMLNamespaceURI)) {
			curElement = null;
			return;
		}*/
		
		curElement = localName;
		
		if(!elementNames.contains(curElement)) {
			curElement = null;
			return;
		}

		if(currentTimeSeries != null) {
			if(currentTimeSeries.getReadings() == null) {
				if(localName.equals(EN_VALUES)) {
					if(singleReading) {
						currentTimeSeries.setReadings(new ArrayList<Reading>());
					} else {
						currentTimeSeries.setReadings(new ArrayList<Reading>(INITIAL_READING_CAPACITY));
					}
					return;
				}
			} else {
				if(localName.equals(EN_VALUE)) {
					String dateStr = atts.getValue(EN_VALUE_TIME);
					
					try {
						Reading r = new Reading();
						r.setDate(parseDate(dateStr));
						currentTimeSeries.getReadings().add(r);
					} catch(ParseException pe) {
						LOG.error("invalid date: " + dateStr, pe);
					} catch(IndexOutOfBoundsException ioobe) {
						LOG.error("invalid date: " + dateStr, ioobe);
					} catch(NullPointerException npe) {
						LOG.error("missing date", npe);
					}
					return;
				}
			}
		}
		if(localName.equals(EN_TIMESERIES)) {
			
			currentSiteData = new SiteData();
			currentTimeSeries = new Series();
			return;
		}
		if(localName.equals(EN_NOTE)) {
			currentNote = atts.getValue(AN_NOTE_TITLE);
		} else {
			currentNote = null;
		}
		if(currentSiteData == null) {
			return;
		}
		if(currentSiteData.getSite() == null) {
			if(localName.equals(EN_SOURCE_INFO)) {
				currentSiteData.setSite(new Site());
				currentSiteData.getSite().setSupportedVariables(new Variable[0]);
				return;
			}
		}
		
		if(currentTimeSeries != null && currentTimeSeries.getVariable() == null) {
			if(localName.equals(EN_VARIABLE)) {
				currentTimeSeries.setVariable(new Variable());
			}
			return;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if(curElement == null)
			return;
		
		String curStr = new String(ch,start,length);
		
		if(curElement.equals(EN_SITE_LATITUDE)) {
			try {
				double latitude = Double.valueOf(curStr);
				currentSiteData.getSite().setLatitude(latitude);
			} catch(NullPointerException npe) {
//				Log.e(this.getClass().getName(), EN_SITE_LATITUDE + " in unexpected location");
			} catch(NumberFormatException nfe) {
//				Log.e(this.getClass().getName(), "not a latitude coordinate", nfe);
			}
			return;
		}
		if(curElement.equals(EN_SITE_LONGITUDE)) {
			try {
				double longitude = Double.valueOf(curStr);
				currentSiteData.getSite().setLongitude(longitude);
			} catch(NullPointerException npe) {
//				Log.e(this.getClass().getName(), EN_SITE_LONGITUDE + " in unexpected location");
			} catch(NumberFormatException nfe) {
//				Log.e(this.getClass().getName(), "not a longitude coordinate",nfe);
			}
			return;
		}
		if(curElement.equals(EN_SITE_NAME)) {
			try {
				
				String name = curStr;
				
				//typically, the site's name will end with a state abbreviation.  Chop this off.
				/*String[] words = name.split(", ");
				if(words.length > 0) {
					String stateAbbrev = words[words.length - 1];
					USState state = USState.getUSStateText(stateAbbrev);
					if(state != null) {
						name = name.substring(0, curStr.length() - stateAbbrev.length() - 2);
					}
				}*/
				//name may be spread across multiple elements if it contains entity characters
				String curName = currentSiteData.getSite().getName();
				currentSiteData.getSite().setName(((curName != null) ? curName : "") + name);
			} catch(NullPointerException npe) {
				LOG.error(EN_SITE_NAME + " in unexpected location");
			}
			return;
		}
		if(curElement.equals(EN_SITE_CODE)) {
			try {
				//check for existing SiteData object for this site
				if(siteData.get(curStr) == null) {
					currentSiteData.getSite().setSiteId(new SiteId("USGS", curStr));
					siteData.put(curStr, currentSiteData);
				} else {
					if(currentSiteData.getDatasets().size() > 0) {
//						Log.w(getClass().getSimpleName(), curStr + " data precedes " + EN_SITE_CODE + "?");
					}
					
					//discard site data collected so far- this should only be
					// information on the site itself, which will be redundant
					// with that of the existing SiteData object
					currentSiteData = siteData.get(curStr);
				}
			} catch(NullPointerException npe) {
//				Log.e(this.getClass().getName(), EN_SITE_CODE + " in unexpected location");
			}
			return;
		}
		if(curElement.equals(EN_NOTE)) {
			if(NOTE_TITLE_STATE_CODE.equals(currentNote)) {
				try {
					USState state = STATES_SORTED_BY_CODE[Integer.parseInt(curStr)];
					currentSiteData.getSite().setState(state);
				} catch(NumberFormatException nfe) {
					LOG.error("invalid state code: " + nfe.getMessage());
				} catch(ArrayIndexOutOfBoundsException aioobe) {
					LOG.error("invalid state code: " + curStr);
				}
			}
			return;
		}
		if(curElement.equals(EN_VARIABLE_TYPE)) {
			try {
				if(currentTimeSeries == null) {
					return;
				}
				
				currentTimeSeries.setVariable(DataSourceController.getVariable("USGS", curStr));
				if(currentTimeSeries.getVariable() == null) {
					LOG.error("unknown variable: " + curStr + "; ignoring series data");
					currentTimeSeries = null;
					return;
				}
				
				Series existingSeries = currentSiteData.getDatasets().put(currentTimeSeries.getVariable().getCommonVariable(), currentTimeSeries);
				if(existingSeries != null) {
					if(LOG.isWarnEnabled()) LOG.warn("duplicate data for variable " + existingSeries.getVariable().getId() + ", site " + currentSiteData.getSite().getId());
				} else {
					Variable[] supportedVars = currentSiteData.getSite().getSupportedVariables();
					
					Variable[] newSupportedVars = null;
					
					newSupportedVars = new Variable[supportedVars.length + 1];
					System.arraycopy(supportedVars, 0, newSupportedVars, 0, supportedVars.length);
	
					newSupportedVars[newSupportedVars.length - 1] = currentTimeSeries.getVariable();
					
					currentSiteData.getSite().setSupportedVariables(newSupportedVars);
				}
			} catch(NullPointerException npe) {
				LOG.error(EN_VARIABLE_TYPE + " in unexpected location");
			}
			return;
		}
		/*if(curElement.equals(EN_VARIABLE_NAME)) {
			try {
				//Log.d(this.getClass().getSimpleName(), EN_VARIABLE_NAME + ": " + curStr);
				String curName = currentTimeSeries.getVariable().getName();
				if(curName == null) {
					currentTimeSeries.getVariable().setName(curStr);
				} else {
					currentTimeSeries.getVariable().setName(curName + curStr);
				}
			} catch(NullPointerException npe) {
				Log.e(this.getClass().getName(), EN_VARIABLE_NAME + " in unexpected location");
			}
			return;
		}*/
		if(curElement.equals(EN_VARIABLE_MAGIC_NULL_VALUE)) {
			if(currentTimeSeries == null) {
				// this may happen if the variable is unrecognized
				return;
			}
			try {
				currentTimeSeries.getVariable().setMagicNullValue(Double.valueOf(curStr));
			} catch(NullPointerException npe) {
				LOG.error(EN_VARIABLE_MAGIC_NULL_VALUE + " in unexpected location");
			} catch(NumberFormatException nfe) {
				LOG.error("invalid value for " + EN_VARIABLE_MAGIC_NULL_VALUE,nfe);
			}
			return;
		}
		if(curElement.equals(EN_VALUE)) {
			if(currentTimeSeries == null) {
				// this may happen if the variable is unrecognized
				return;
			}
			try {
				List<Reading> readings = currentTimeSeries.getReadings();
				Reading currentReading = readings.get(readings.size() - 1);
				currentReading.setValue(new Double(curStr));
				try {
					if(currentReading.getValue().equals(currentTimeSeries.getVariable().getMagicNullValue())) {
						currentReading.setValue(null);
					}
				} catch(NullPointerException npe) {
					//EN_VARIABLE_MAGIC_NULL_VALUE should come earlier in the stream, so this shouldn't happen
					LOG.error(EN_VARIABLE_MAGIC_NULL_VALUE + " in unexpected location");
				}
			} catch(NullPointerException npe) {
				LOG.error(EN_VALUE + " in unexpected location");
			} catch(ArrayIndexOutOfBoundsException aioobe) {
				LOG.error("no reading to set value for");
			} catch(NumberFormatException nfe) {
				LOG.error("invalid value for " + EN_VALUE,nfe);
			}
			return;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		curElement = null;
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
