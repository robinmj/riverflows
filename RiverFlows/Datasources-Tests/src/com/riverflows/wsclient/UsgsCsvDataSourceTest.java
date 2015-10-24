package com.riverflows.wsclient;

import com.riverflows.data.Reading;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.USTimeZone;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class UsgsCsvDataSourceTest extends TestCase {
	
	private static final UsgsCsvDataSource src = new UsgsCsvDataSource();
	
	static {
		src.setHttpClientWrapper(new MockUsgsCsvHttpClient());
	}
	
	public void testGetSiteData() throws Throwable {
		Variable[] mfFlatheadVars = DataSourceController.getVariablesFromStrings("USGS", new String[]{"00060", "00065"});
		Site mfFlathead = new Site(new SiteId("USGS", "12358500"), "M F Flathead River near West Glacier MT", USState.MT,
				mfFlatheadVars);
		
		SiteData result = src.getSiteData(mfFlathead,  mfFlatheadVars, true);
		
		System.out.println(result.getDataInfo());
		assertTrue(result.getDataInfo().contains(mfFlathead.getSiteId().getId()));
		assertTrue(result.getDataInfo().contains(mfFlathead.getName()));
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2011, 2, 5, 9, 45, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("GMT-07:00"));
		
		Reading r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(635.0d, r.getValue());
		assertNull(r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(1.90d, r.getValue());
		assertNull(r.getQualifiers());
		
		cal.set(2011, 2, 6, 0, 0, 0);
		
		r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(57);
		assertEquals(cal.getTime(), r.getDate());
		assertNull(r.getValue());
		assertEquals("Eqp", r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(57);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(1.91d, r.getValue());
		assertNull(r.getQualifiers());
		
		cal.set(2011, 2, 10, 6, 0, 0);
		
		r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(465);
		assertEquals(cal.getTime(), r.getDate());
		assertNull(r.getValue());
		assertEquals("Eqp", r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(465);
		assertEquals(cal.getTime(), r.getDate());
		assertNull(r.getValue());
		assertEquals("Eqp", r.getQualifiers());
	}

	
	public void testGetEDTSite() throws Throwable {
		Variable[] clinchVars = DataSourceController.getVariablesFromStrings("USGS", new String[]{"00010","00095", "00065","00060"});
		Site clinch = new Site(new SiteId("USGS", "03524000"), "CLINCH RIVER AT CLEVELAND, VA", USState.MT,
				clinchVars);
		
		SiteData result = src.getSiteData(clinch,  clinchVars, true);
		
		System.out.println(result.getDataInfo());
		assertTrue(result.getDataInfo().contains(clinch.getSiteId().getId()));
		assertTrue(result.getDataInfo().contains(clinch.getName()));
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2011, 3, 13, 20, 30, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(USTimeZone.MDT.getTimeZone());
		System.out.println("Expected date: " + cal.getTime());
		
		List<Reading> gaugeHeightReadings = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings();
		
		Reading r = gaugeHeightReadings.get(gaugeHeightReadings.size() - 1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(4.58d, r.getValue());
		assertNull(r.getQualifiers());
	}

    public void testGetBeaverDam() throws Throwable {
        Variable[] beaverDamVars = DataSourceController.getVariablesFromStrings("USGS", new String[]{"00060","00065"});
        Site beaverDam = new Site(new SiteId("USGS", "09414900"), "BEAVER DAM WASH AT BEAVER DAM, AZ", USState.AZ,
                beaverDamVars);

        SiteData result = src.getSiteData(beaverDam,  beaverDamVars, true);
    }
}
