package com.riverflows.wsclient;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.riverflows.data.Series;
import com.riverflows.data.SiteData;
import com.riverflows.data.USState;
import com.riverflows.data.Variable.CommonVariable;

public class AhpsKmlSiteSourceTest extends TestCase {
	
	private static final Log LOG = LogFactory.getLog(AhpsKmlSiteSourceTest.class);
	
	AhpsKmlSiteSource siteSource = new AhpsKmlSiteSource();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		Logger.getLogger("").setLevel(Level.FINEST);
		
		siteSource.setHttpClientWrapper(new FileHttpClientWrapper("testdata/ahps/ahps_national_fcst.kmz", AhpsKmlSiteSource.SITE_LIST_URL));
	}
	
	public void testLoadSites() throws Throwable {
		List<SiteData> result = siteSource.getSiteData();
		
		for(SiteData data: result) {
			LOG.info(data.getSite().getName());
		}
		
		LOG.info(result.size() + " sites found");
		

		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2011, 2, 24, 18, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("GMT-00:00"));
		
		SiteData ashtonSD = result.get(0);
		assertEquals("James River at Ashton", ashtonSD.getSite().getName());
		assertEquals(USState.SD, ashtonSD.getSite().getState());
		assertEquals(new AHPSXmlDataSource().getAgency(), ashtonSD.getSite().getAgency());
		assertEquals("ATNS2", ashtonSD.getSite().getId());
		assertEquals(44.995833d, ashtonSD.getSite().getLatitude());
		assertEquals(-98.480556d, ashtonSD.getSite().getLongitude());
		assertTrue(Arrays.equals(AHPSXmlDataSource.ACCEPTED_VARIABLES, ashtonSD.getSite().getSupportedVariables()));
		Series forecast;
		assertNotNull(forecast = ashtonSD.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT));
		assertEquals(AHPSXmlDataSource.VTYPE_STAGE, forecast.getVariable());
		assertEquals(18.9d, forecast.getReadings().get(0).getValue());
		assertEquals(cal.getTime(), forecast.getReadings().get(0).getDate());
	}
}
