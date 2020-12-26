package com.riverflows.wsclient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USTimeZone;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

public class CDECDataSourceTest extends TestCase {
	private static final CDECDataSource ds = new CDECDataSource();

	static {
		ds.setHttpClientWrapper(new MockCDECHttpClient());
	}

	public void testGetDataInfo() throws Throwable {

		Site ads = new Site(new SiteId("CDEC","ADS"), null, null, null);
		SiteData data = ds.getSiteData(ads, new Variable[] { CDECDataSource.VTYPE_WIND_DR }, true);

		System.out.println("dataInfo:\n" + data.getDataInfo());
	}
	
	public void testGetSiteData() throws Throwable {
		Site cbr = new Site(new SiteId("CDEC","CBR"), null, null, null);
		SiteData data = ds.getSiteData(cbr, new Variable[] { CDECDataSource.VTYPE_FLOW }, true);

		System.out.println("dataInfo:\n" + data.getDataInfo());

		Series streamflow = data.getDatasets().get(CommonVariable.STREAMFLOW_CFS);

		Reading lastObs = streamflow.getLastObservation();

		DateFormat pstFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pstFormat.setTimeZone(USTimeZone.PST.getTimeZone());

		assertEquals("12/19/20 12:00:00 PM GMT-08:00", pstFormat.format(lastObs.getDate()));
		assertEquals(1396.0d, lastObs.getValue());

		assertEquals(168, streamflow.getReadings().size());

		Series gauge_height = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);

		Reading firstObs = gauge_height.getReadings().get(0);
		assertEquals("12/12/20 1:00:00 PM GMT-08:00", pstFormat.format(firstObs.getDate()));
		assertEquals(3.72d, firstObs.getValue());
	}

	public void testGetGVOData() throws Throwable {
		Site cbr = new Site(new SiteId("CDEC","GVO"), null, null, null);
		SiteData data = ds.getSiteData(cbr, new Variable[] { CDECDataSource.VTYPE_FLOW }, true);

		System.out.println("dataInfo:\n" + data.getDataInfo());

		Series gaugeHeight = data.getDatasets().get(CommonVariable.RES_ELEVATION_FT);

		Reading lastObs = gaugeHeight.getLastObservation();

		DateFormat pstFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pstFormat.setTimeZone(USTimeZone.PST.getTimeZone());

		Reading sixtyEighth = data.getDatasets().get(CommonVariable.WATERTEMP_F).getReadings().get(67);

		assertEquals("12/15/20 8:00:00 AM GMT-08:00", pstFormat.format(sixtyEighth.getDate()));
		assertEquals(null, sixtyEighth.getValue());
		assertEquals("N", sixtyEighth.getQualifiers());

		assertEquals("12/19/20 12:00:00 PM GMT-08:00", pstFormat.format(lastObs.getDate()));
		assertEquals(2799.76d, lastObs.getValue());
	}

	public void testGetJBRData() throws Throwable {
		Site cbr = new Site(new SiteId("CDEC","JBR"), "YUBA RIVER AT JONES BAR", null, null);
		SiteData data = ds.getSiteData(cbr, new Variable[] { CDECDataSource.VTYPE_FLOW }, true);

		System.out.println("dataInfo:\n" + data.getDataInfo());

		Series gaugeHeight = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);

		DateFormat pstFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pstFormat.setTimeZone(USTimeZone.PST.getTimeZone());

		Reading hundredthReading = gaugeHeight.getReadings().get(99);

		assertEquals("4/24/19 9:00:00 PM GMT-08:00", pstFormat.format(hundredthReading.getDate()));
		assertEquals(9.51d, hundredthReading.getValue());

		Reading lastObs = gaugeHeight.getLastObservation();

		assertEquals("4/27/19 5:00:00 PM GMT-08:00", pstFormat.format(lastObs.getDate()));
		assertEquals(9.80d, lastObs.getValue());

		for(Reading r : gaugeHeight.getReadings()) {
			assertTrue("" + r.getValue(), r.getValue() < 11.0);
			assertTrue("" + r.getValue(), r.getValue() > 9.0);
		}
	}
}
