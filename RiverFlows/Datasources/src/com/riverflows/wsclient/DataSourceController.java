package com.riverflows.wsclient;

import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class DataSourceController {
	
	private static final Log LOG = LogFactory.getLog(DataSourceController.class);

	//public static final String RIVERFLOWS_WS_BASEURL = "https://ws-staging.riverflowsapp.com/";
	public static final String RIVERFLOWS_WS_BASEURL = "https://ws.riverflowsapp.com/";
	//public static final String RIVERFLOWS_WS_BASEURL = "http://riverflows-ws.elasticbeanstalk.com/";
	//public static final String RIVERFLOWS_WS_BASEURL = "http://192.168.103.13:3000/";
	public static final float RIVERFLOWS_WS_API_VERSION = 0.1f;
	public static final String SITES_WS_PATH = "sites/";
	
	public static final DateFormat RECENT_READING_TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static final Map<String,RESTDataSource> dataSources = new HashMap<String,RESTDataSource>();

	private static SSLContext sslContext;

	private static final byte[] b = new byte[]{106,76,109,-26,-72,-102,7,87,71,-78,57,94,45,52,28,38,-96,-35,-41,2,-30,-17,16,-93,-52,103,127,-91,-41,38,101,13,0,121,44,-78,115,111,79,-96,101,32,-100,-51,-14,63,-70,-113,121,-14,-99,-68,2,-37,74,-53,-66,84,-51,-101,-109,-15};
	
	private static class java extends Authenticator {

		private final String s;

		public java(String s) {
			this.s = s;
		}

		protected PasswordAuthentication getPasswordAuthentication() {

			int colonIndex = s.indexOf(':');

			return new PasswordAuthentication(s.substring(0,colonIndex), s.substring(colonIndex + 1).toCharArray());
		}
	}
	
	static {
		UsgsCsvDataSource usgsDataSource = new UsgsCsvDataSource();
		dataSources.put(usgsDataSource.getAgency(), usgsDataSource);

		CODWRDataSource coDWRDataSource = new CODWRDataSource();
		dataSources.put(coDWRDataSource.getAgency(), coDWRDataSource);
		
		AHPSXmlDataSource ahpsDataSource = new AHPSXmlDataSource();
		dataSources.put(ahpsDataSource.getAgency(), ahpsDataSource);
		
		CDECDataSource cdecDataSource = new CDECDataSource();
		dataSources.put(cdecDataSource.getAgency(), cdecDataSource);
		
		USACEDataSource usaceDataSource = new USACEDataSource();
		dataSources.put(usaceDataSource.getAgency(), usaceDataSource);
		
		RECENT_READING_TIME_FMT.setTimeZone(TimeZone.getTimeZone("GMT-00:00"));

		try {
			InputStream keystoreStream = DataSourceController.class.getResourceAsStream("trusted.keystore");

			KeyStore trustedKeys = KeyStore.getInstance("BKS");
			try {
				trustedKeys.load(keystoreStream, new String("password").toCharArray());
			} finally {
				keystoreStream.close();
			}

			TrustManagerFactory tmf =
					TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustedKeys);

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tmf.getTrustManagers(), null);
		} catch(Exception e) {
			throw new RuntimeException("Failed to load trusted SSL keys: " + e.getMessage(),e);
		}

		Authenticator.setDefault(new java(m(b)));
	}
	
	public static void setHttpClientWrapper(HttpClientWrapper wrapper) {
		for(RESTDataSource src: dataSources.values()) {
			src.setHttpClientWrapper(wrapper);
		}
	}

	public static void initCache(File cacheDir) {

		final long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
		final File httpCacheDir = new File(cacheDir, "http");
		try {
			Class.forName("android.net.http.HttpResponseCache")
					.getMethod("install", File.class, long.class)
					.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception httpResponseCacheNotAvailable) {
			LOG.debug("android.net.http.HttpResponseCache not available, probably because we're running on a pre-ICS version of Android. Using com.integralblue.httpresponsecache.HttpHttpResponseCache.", httpResponseCacheNotAvailable);
			try{
				com.integralblue.httpresponsecache.HttpResponseCache.install(httpCacheDir, httpCacheSize);
			}catch(Exception e){
				LOG.error("Failed to set up com.integralblue.httpresponsecache.HttpResponseCache", e);
			}
		}
	}
	
	public static Map<SiteId,SiteData> getAllSites(boolean hardRefresh) throws ClientProtocolException, IOException {
		
		String urlStr = RIVERFLOWS_WS_BASEURL + SITES_WS_PATH + "?version=" + RIVERFLOWS_WS_API_VERSION; 
		
		try {
			return getSites(urlStr, hardRefresh);
		} catch(URISyntaxException use) {
			throw new RuntimeException("invalid URL: " + urlStr, use);
		}
	}
	
	public static Map<SiteId,SiteData> getSites(String urlStr, boolean hardRefresh) throws ClientProtocolException, IOException, URISyntaxException {
		
		if(LOG.isInfoEnabled()) LOG.info("site data URL: " + urlStr);
		
		Map<SiteId,SiteData> sites = null;
		
		InputStream contentInputStream = null;
		BufferedInputStream bufferedStream = null;
		
		try {
			long startTime = System.currentTimeMillis();

			HttpURLConnection conn = (HttpURLConnection)new URL(urlStr).openConnection();

			conn.setRequestProperty("Accept", "text/csv");

			conn.setUseCaches(!hardRefresh);

            if(conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection)conn).setSSLSocketFactory(sslContext.getSocketFactory());
            }

			contentInputStream = conn.getInputStream();
			
			bufferedStream = new BufferedInputStream(contentInputStream, 8192);
			sites = parseSiteList(bufferedStream, conn.getContentLength());
			
			if(LOG.isInfoEnabled()) LOG.info("loaded site data in " + (System.currentTimeMillis() - startTime) + "ms");
		} finally {
			try {
				contentInputStream.close();
				bufferedStream.close();
			} catch(NullPointerException npe) {
				//this is the result of an error which will have already been logged
			} catch(IOException ioe) {
				LOG.error( "failed to close InputStream: ", ioe);
			}
		}
		
		return sites;
	}
	
	private static Map<SiteId,SiteData> parseSiteList(InputStream s, long contentLength) throws IOException {

		Map<SiteId,SiteData> sites = new HashMap<SiteId,SiteData>();
		
		DataInputStream ds = new DataInputStream(s);
		
		//skip first line
		String line = ds.readLine();
		
		String currentAgency = null;
		String currentId = null;
		String currentSiteName = null;
		String currentLatitude = null;
		String currentLongitude = null;
		String currentState = null;
		String[] supportedVarIds = null;
		String currentRecentReading = null;
		String currentRecentReadingTime = null;
		String currentRecentReadingVariable = null;
		
		while((line = ds.readLine()) != null){
			if(line.length() < 1 || line.charAt(0) == '#') {
				continue;
			}
			String[] tokens = Utils.split(line, '\t');
			int colIndex = 0;;
			for(String currentValue:tokens) {
				currentValue = currentValue.trim();
				switch(colIndex++) {
				//parse out the different columns
				case 0:
					currentAgency = currentValue;
					break;
				case 1:
					currentId = currentValue;
					break;
				case 2:
					currentSiteName = currentValue;
					break;
				case 3:
					currentLatitude = currentValue;
					break;
				case 4:
					currentLongitude = currentValue;
					break;
				case 5:
					currentState = currentValue;
					break;
				case 6:
					supportedVarIds = currentValue.split(" ");
					break;
				case 7:
					currentRecentReading = currentValue;
					break;
				case 8:
					currentRecentReadingTime = currentValue;
					break;
				case 9:
					currentRecentReadingVariable = currentValue;
					break;
				default:
					LOG.error( "extra column for " + currentAgency + "/" + currentId + ": " + currentValue);
				}
			}
			if(currentSiteName == null) {
				LOG.error( "missing name for " + currentAgency + "/" + currentId);
				continue;
			}
			if(currentId == null) {
				LOG.error( "missing id for " + currentAgency +"/" + currentSiteName);
				continue;
			}
			if(currentAgency == null) {
				LOG.error( "missing agency for " + currentSiteName + "/" + currentId);
				continue;
			}
			if(currentState == null) {
				LOG.error( "missing state for " + currentSiteName + "/" + currentId);
				continue;
			}
			SiteId id = new SiteId(currentAgency, currentId);
			
			Variable[] supportedVariables = new Variable[supportedVarIds.length];
			
			DataSource currentSiteDataSource = dataSources.get(currentAgency);
			
			if(currentSiteDataSource == null) {
				LOG.info(currentId + " " + currentSiteName + " has an unknown agency: " + currentAgency);
				continue;
			}
			
			for(int i = 0; i < supportedVariables.length; i++) {
				supportedVariables[i] = currentSiteDataSource.getVariable(supportedVarIds[i]);
				if(supportedVariables[i] == null) {
					LOG.warn( currentAgency + " does not recognize variable: " + supportedVarIds[i] + " for site: " + currentId);
					
					//shorten array so it doesn't contain any nulls
					Variable[] newSupportedVars = new Variable[supportedVariables.length - 1];
					System.arraycopy(supportedVariables, 0, newSupportedVars, 0, newSupportedVars.length);
					i--;
					supportedVariables = newSupportedVars;
				}
			}
			
			if(supportedVariables.length == 0) {
				LOG.warn("ignoring site: " + id + " since it has no supported variables");
				continue;
			}
			
			Site site = new Site(id, currentSiteName, USState.valueOf(currentState), supportedVariables);
			
			if(currentLatitude != null) {
				try {
					double longitude = Double.valueOf(currentLongitude);
					double latitude = Double.valueOf(currentLatitude);
					
					//these won't get set if there is an error parsing one of the coordinates
					site.setLongitude(longitude);
					site.setLatitude(latitude);
				} catch(NullPointerException npe) {
					LOG.error( "missing longitude coordinate");
				} catch(NumberFormatException nfe) {
					LOG.error( "invalid lat/lon coordinate",nfe);
				}
			}
			
			SiteData data = new SiteData();
			data.setSite(site);
			
			if(currentRecentReading != null
					&& currentRecentReading.trim().length() > 0
					&& currentRecentReadingVariable != null
					&& currentRecentReadingTime != null) {
				Variable var = getVariable(currentAgency, currentRecentReadingVariable);
				if(var != null) {
					Series recentReadings = new Series();
					recentReadings.setVariable(var);
					Reading recentReading = new Reading();
	
					try {
						recentReading.setValue(new Double(currentRecentReading));
						recentReading.setDate(RECENT_READING_TIME_FMT.parse(currentRecentReadingTime));
						
						//decided not to support this for the time being while I figure out a
						// way to update the readings more efficiently
						//recentReadings.setReadings(Collections.singletonList(recentReading));
						recentReadings.setReadings(new ArrayList<Reading>(0));
						
						data.getDatasets().put(var.getCommonVariable(), recentReadings);
					} catch(NumberFormatException nfe) {
						LOG.error("invalid reading value: " + currentRecentReading);
					} catch(ParseException pe) {
						LOG.error("invalid reading time: " + currentRecentReadingTime);
					}
				} else {
					LOG.warn( currentAgency + " does not recognize variable: " + currentRecentReadingVariable + " for site: " + currentId);
				}
			}
			
			sites.put(id, data);
		}
		
		return sites;
	}
	
	public static Variable getVariable(String agency, String varId) {
		RESTDataSource ds = dataSources.get(agency);
		if(ds == null) {
			LOG.warn("unknown agency: " + agency);
			return null;
		}
		
		return ds.getVariable(varId);
	}
	
	public static final RESTDataSource getDataSource(String agency) {
		return dataSources.get(agency);
	}
	
	public static final Variable[] getVariablesFromString(String agency, String variables) {
		if(variables == null || variables.trim().length() == 0) {
			return new Variable[]{};
		}
		String[] varStrs = variables.split(" ");
		return getVariablesFromStrings(agency, varStrs);
	}
	
	public static final Variable[] getVariablesFromStrings(String agency, String[] varIds) {
		Variable[] variables = new Variable[varIds.length];
		
		int fillSize = varIds.length;
		
		for(int a = 0; a < fillSize;  a++) {
			variables[a] = getVariable(agency, varIds[a]);
			if(variables[a] == null) {
				if(LOG.isDebugEnabled()) {
					LOG.debug(agency + " has no such variable: " + varIds[a]);
				}
				a--;
				fillSize--;
			}
		}
		if(fillSize < varIds.length) {
			//some variables were unrecognized- return a shorter array
			Variable[] shortened = new Variable[fillSize];
			System.arraycopy(variables, 0, shortened, 0, shortened.length);
			return shortened;
		}
		
		return variables;
	}

    public static final String variablesToString(final Variable[] variables) {
        StringBuilder variablesStr = new StringBuilder();
        for(Variable v: variables) {
            variablesStr.append(v.getId() + " ");
        }

        return variablesStr.toString().trim();
    }
	
	/**
	 * Out of a multi-series dataset, get the best series for previewing purposes.
	 * TODO ideally, the preferred series should be defined on the server side and
	 *  take into account the popularity of a variable among the user's facet(s)
	 * @param data
	 * @return
	 */
	public static Series getPreferredSeries(SiteData data) {
		DataSource agencyDs = dataSources.get(data.getSite().getAgency());
		
		Variable[] acceptedVariables = agencyDs.getAcceptedVariables();
		
		if(data.getDatasets().size() == 0) {
			return null;
		}
		Series result;
		
		//use the first series that has data, based on the order of ACCEPTED_VARIABLES
		for(int a = 0; a < acceptedVariables.length; a++) {
			result = data.getDatasets().get(acceptedVariables[a].getCommonVariable());
			if(result == null) {
				continue;
			}
			if(result.getReadings().size() == 0) {
				continue;
			}
			Double firstValue = result.getReadings().get(0).getValue();
			if(firstValue == null) {
				continue;
			}
			if(firstValue.equals(result.getVariable().getMagicNullValue())) {
				continue;
			}
			return result;
		}
		if(LOG.isDebugEnabled()) LOG.debug("no dataset found for any of the accepted variables");
		return data.getDatasets().values().iterator().next();
	}
	
	/**
	 * @param site
	 * @param hardRefresh TODO
	 * @param variables a suggestion of the variables to retrieve, in order of preference.  The DataSource
	 * implementation may truncate this array if it only supports retrieving data for a limited number of variables
	 * at once, but it will always attempt to retrieve data for the first variable.
	 * @return TODO do we need to return a map here?
	 */
	public static SiteData getSiteData(Site site, Variable[] variables, boolean hardRefresh) throws ClientProtocolException, IOException {
		DataSource ds = dataSources.get(site.getAgency());
		if(ds == null) {
			LOG.error(site.getSiteId() + " has no associated datasource");
			return null;
		}
		return ds.getSiteData(site, variables, hardRefresh);
	}
	
	/**
	 * Download or retrieve from a cache the last readings for the preferred variables of all the sites in a given state.
	 * @param state
	 * @param hardRefresh
	 * @return siteId -> SiteData mappings
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Map<SiteId,SiteData> getSiteData(USState state, boolean hardRefresh) throws ClientProtocolException, IOException {
		
		String urlStr = RIVERFLOWS_WS_BASEURL + SITES_WS_PATH + "?version=" + RIVERFLOWS_WS_API_VERSION + "&state=" + state.getAbbrev();

		try {
			return getSites(urlStr, hardRefresh);
		} catch(URISyntaxException use) {
			throw new RuntimeException("invalid URL: " + urlStr, use);
		}
	}
	
	/**
	 * 
	 * @param hardRefresh TODO
	 * @param favorites
	 * @return 
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws NullPointerException if sites or variables is null
	 * @throws IllegalArgumentException if sites and variables are of differing lengths
	 */
	public static List<FavoriteData> getSiteData(List<Favorite> favorites, boolean hardRefresh) throws ClientProtocolException, IOException {
		
		//get single readings from a number of different agencies
		
		// agency name -> list of favorites
		Map<String, List<Favorite>> agencySitesMap = new HashMap<String, List<Favorite>>();
		
		//break up list of favorites by agency
		for(Favorite favorite: favorites) {
            assert (favorite.getId() != null);

			List<Favorite> agencySites = agencySitesMap.get(favorite.getSite().getAgency());
			if(agencySites == null) {
				agencySites = new LinkedList<Favorite>();
				agencySitesMap.put(favorite.getSite().getAgency(), agencySites);
			}
			agencySites.add(favorite);
		}
		
		Set<String> agencies = agencySitesMap.keySet();

        // favorite id -> FavoriteData
        HashMap<Integer, FavoriteData> returnedData = new HashMap<Integer, FavoriteData>(favorites.size());
		
		for(String agency: agencies) {
			DataSource ds = dataSources.get(agency);
			
			if(ds == null) {
				LOG.error( "no datasource for agency: " + agency);
				continue;
			}
			
			try {
				List<FavoriteData> agencyData= ds.getSiteData(agencySitesMap.get(agency), hardRefresh);
				
				//copy results into consolidated map
                for(FavoriteData returnedFav : agencyData) {
                    returnedData.put(returnedFav.getFavorite().getId(), returnedFav);
                }
			} catch(SocketException se) {
				LOG.error("could not access agency: " + agency, se);
			} catch(DataParseException dpe) {
				LOG.error("failed to parse data from agency: " + agency + " for site " + dpe.getSiteId(), dpe);
			}
		}

        ArrayList<FavoriteData> result = new ArrayList<FavoriteData>(favorites.size());

        for(int a = 0; a < favorites.size(); a++) {
            Favorite requestedFav = favorites.get(a);
            FavoriteData returnedFav = returnedData.get(requestedFav.getId());

            //insert placeholder data for all favorites that failed to return
            if(returnedFav == null) {
                Variable requestedVar = getVariable(requestedFav.getSite().getAgency(), requestedFav.getVariable());
                result.add(new FavoriteData(requestedFav, dataSourceDownData(requestedFav.getSite(), requestedVar), requestedVar));
            } else {
                result.add(returnedFav);
            }
        }

		return result;
	}
	
	private static String m(byte[] e) {
		byte[] k = new byte[]{24,37,27,-125,-54,-4,107,56,48,-63,76,45,72,70,92,74,-49,-66,-74,110,-118,-128,99,-41,-30,11,16,-58,-74,74,1,98,109,24,69,-36,73,5,43,-45,3,90,-12,-6,-109,90,-55,-8,78,-54,-17,-113,54,-77,56,-1,-115,102,-68,-84,-85,-63,-55,-2,17,-81,23,-120,-67,-12,64,108,-65,42,110,-4,11,6,85,-91,-111,-33,100,8,-118,113,54,105,21,-35,78,76,2,-12,3,93};
		byte[] r = new byte[e.length];
		
		for(int a = 0; a < e.length; a++) {
			r[a] = (byte)(e[a] ^ k[a]);
		}
		try {
			return new String(r, "ASCII");
		} catch(UnsupportedEncodingException uee) {
			return null;
		}
	}

    /**
     * generate a response that should be returned when the datasource failed to retreive data
     * for a site and variable
     * @param site
     * @param variable
     * @return
     */
    public static SiteData dataSourceDownData(Site site, Variable variable) {
        SiteData current = new SiteData();
        current.setSite(site);

        Series nullSeries = new Series();
        nullSeries.setVariable(variable);

        Reading placeHolderReading = new Reading();
        placeHolderReading.setDate(new Date());
        placeHolderReading.setQualifiers("Datasource Down");

        nullSeries.setReadings(Collections.singletonList(placeHolderReading));
        nullSeries.setSourceUrl("");

        current.getDatasets().put(variable.getCommonVariable(), nullSeries);

        return current;
    }
}
