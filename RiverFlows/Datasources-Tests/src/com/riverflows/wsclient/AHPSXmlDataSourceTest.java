package com.riverflows.wsclient;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import com.riverflows.data.Forecast;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable.CommonVariable;

public class AHPSXmlDataSourceTest extends TestCase {
	
	private AHPSXmlDataSource ds = new AHPSXmlDataSource();
	
	public void setUp() {
		ds.setHttpClientWrapper(new MockAHPSHttpClient());
	}
	
	public void testGetSATW1() throws Throwable {
		Site satsop = new Site(new SiteId("AHPS","satw1"),
				"Satsop", -123.493611d, 47.000833d, USState.WA,
				AHPSXmlDataSource.ACCEPTED_VARIABLES);
		SiteData result = ds.getSiteData(satsop,
				null);
		Series streamflowDataset = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS);
		
		assertNotNull(result.getSite());
		assertEquals(satsop.getKey(), result.getSite().getKey());
		assertEquals(satsop.getId(), result.getSite().getId());
		assertEquals(satsop.getAgency(), result.getSite().getAgency());
		assertEquals(satsop.getName(), result.getSite().getName());
		assertEquals(satsop.getLatitude(), result.getSite().getLatitude());
		assertEquals(satsop.getLongitude(), result.getSite().getLongitude());
		assertEquals(satsop.getState(), result.getSite().getState());
		assertEquals(satsop.getSupportedVariables().length, result.getSite().getSupportedVariables().length);
		assertTrue(Arrays.equals(satsop.getSupportedVariables(), result.getSite().getSupportedVariables()));
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2011, 2, 9, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("GMT-00:00"));
		
		Reading r = streamflowDataset.getReadings().get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(2280.0d, r.getValue());
		assertFalse(r instanceof Forecast);
		
		cal.set(2011, 2, 9, 2, 30, 0);
		
		r = streamflowDataset.getReadings().get(10);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(2240.0d, r.getValue());
		assertFalse(r instanceof Forecast);
		
		cal.set(2011, 2, 18, 12, 15, 0);
		//last observed reading
		r = streamflowDataset.getReadings().get(913);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(5060.0d, r.getValue());
		assertFalse(r instanceof Forecast);
		
		cal.set(2011, 2, 17, 18, 0, 0);
		//first forecasted reading
		r = streamflowDataset.getReadings().get(914);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(6380.0d, r.getValue());
		assertTrue(r instanceof Forecast);

		cal.set(2011, 2, 27, 12, 0, 0);
		//last forecasted reading
		r = streamflowDataset.getReadings().get(streamflowDataset.getReadings().size() - 1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(4000.0d, r.getValue());
		assertTrue(r instanceof Forecast);

		Series gaugeHeightDataset = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);

		cal.set(2011, 2, 9, 0, 0, 0);

		r = gaugeHeightDataset.getReadings().get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(28.01d, r.getValue());
		assertFalse(r instanceof Forecast);
		
		cal.set(2011, 2, 9, 2, 30, 0);
		
		r = gaugeHeightDataset.getReadings().get(10);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(27.99d, r.getValue());
		assertFalse(r instanceof Forecast);
		
		cal.set(2011, 2, 18, 12, 15, 0);
		//last observed reading
		r = gaugeHeightDataset.getReadings().get(913);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(29.36d, r.getValue());
		assertFalse(r instanceof Forecast);
		
		cal.set(2011, 2, 17, 18, 0, 0);
		//first forecasted reading
		r = gaugeHeightDataset.getReadings().get(914);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(29.92d, r.getValue());
		assertTrue(r instanceof Forecast);

		cal.set(2011, 2, 27, 12, 0, 0);
		//last forecasted reading
		r = gaugeHeightDataset.getReadings().get(streamflowDataset.getReadings().size() - 1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(28.85d, r.getValue());
		assertTrue(r instanceof Forecast);
	}
}
