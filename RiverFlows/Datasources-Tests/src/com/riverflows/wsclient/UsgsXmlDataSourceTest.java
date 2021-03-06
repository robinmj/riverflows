package com.riverflows.wsclient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import junit.framework.TestCase;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

import org.apache.http.client.HttpClient;
import org.junit.Rule;

import co.freeside.betamax.Recorder;
import co.freeside.betamax.httpclient.BetamaxHttpClient;

public class UsgsXmlDataSourceTest extends TestCase {

	private static SimpleDateFormat valueDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ");

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		UsgsXmlDataSource.setHttpClientWrapper(new FileHttpClientWrapper("testdata/usgs/xml/", "http://waterservices.usgs.gov/nwis/iv/?format=waterml,1.1&stateCd="));
	}

	/*
	public void testGetAllSites() throws Throwable {
		recorder.insertTape("testGetAllSites");
		for(int a = 0; a < USState.values().length; a++) {
			USState state = USState.values()[a];
			
			Map<String,SiteData> data = UsgsXmlDataSource.getSiteData(state, null);
			Site site = data.values().iterator().next().getSite();
			USState assignedState = site.getState();
			
			//TODO remove Puerto Rico from the list of states?
			if(state != USState.PR) {
				assertEquals(state, assignedState);
			}
		}
	}*/

	public void testGetFlorida() throws Throwable {
		Map<String, SiteData> data = UsgsXmlDataSource.getSiteData(USState.FL, null);

		SiteData siteData = data.get("02358754");


		assertEquals("APALACHICOLA R.AB CHIPOLA CONR WEWAHITCHKA,FLA", siteData.getSite().getName());
		assertEquals(30.13408877d, siteData.getSite().getLatitude());
		assertEquals(-85.1440822d, siteData.getSite().getLongitude());
		assertEquals(USState.FL, siteData.getSite().getState());
		assertEquals(1, siteData.getSite().getSupportedVariables().length);
		assertEquals("00065", siteData.getSite().getSupportedVariables()[0].getId());

		Series dataset = siteData.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);

		assertEquals(1, dataset.getReadings().size());

		Reading recentReading = dataset.getLastObservation();

		assertEquals(14.58d, recentReading.getValue());

		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2015, 8, 18, 10, 15, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("GMT-05:00"));

 		assertEquals(cal.getTime(), recentReading.getDate());
 		assertEquals(UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT, dataset.getVariable());

	}

	public void testGetAlabama() throws Throwable {
		Map<String, SiteData> data = UsgsXmlDataSource.getSiteData(USState.AL, null);

		SiteData d1 = data.get("02428400");

		assertEquals(USState.AL, d1.getSite().getState());
		assertEquals("02428400", d1.getSite().getId());
		assertEquals(31.61515849d, d1.getSite().getLatitude());
		assertEquals(-87.550548d, d1.getSite().getLongitude());
		assertEquals("ALABAMA RIVER AT CLAIBORNE L&D NEAR MONROEVILLE", d1.getSite().getName());

		Set<String> expectedVars = new HashSet<String>();
		expectedVars.add(UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS.getId());
		expectedVars.add(UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT.getId());

		Variable[] supportedVars = d1.getSite().getSupportedVariables();
		for (int a = 0; a < supportedVars.length; a++) {
			assertTrue("unexpected variable: " + supportedVars[a].getId() + " " + supportedVars[a].getName(), expectedVars.contains(supportedVars[a].getId()));
		}

	}

	public void testGetColorado() throws Throwable {
		Map<String, SiteData> data = UsgsXmlDataSource.getSiteData(USState.CO, null);

		SiteData silverton = data.get("09359020");
		Series ht = silverton.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);
		Reading recent = ht.getReadings().get(0);
		//qualifiers are not currently supported by UsgsXmlDataSource
		//assertEquals("P Ice", recent.getQualifiers());
		//assertEquals(null, recent.getValue());
		//assertEquals("2011-02-13T06:00:00.000-0700", valueDateFormat.format(recent.getDate()));
	}

	public void testGetWyoming() throws Throwable {
		Map<String, SiteData> data =  UsgsXmlDataSource.getSiteData(USState.WY, null);

		SiteData yellowstone = data.get("06186500");
		Series cfs = yellowstone.getDatasets().get(CommonVariable.STREAMFLOW_CFS);
		Reading recent = cfs.getReadings().get(0);
		//qualifiers are not currently supported by UsgsXmlDataSource
		//assertEquals("P Ice", recent.getQualifiers());
		assertEquals(null, recent.getValue());
		assertEquals("2011-12-26T19:30:00.000-0700", valueDateFormat.format(recent.getDate()));
		
	}
}
