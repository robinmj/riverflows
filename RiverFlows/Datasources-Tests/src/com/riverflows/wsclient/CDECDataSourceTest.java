package com.riverflows.wsclient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;

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
	
	public void testGetSiteData() throws Throwable {
		Site cbr = new Site(new SiteId("CDEC","CBR"), null, null, null);
		SiteData data = ds.getSiteData(cbr, new Variable[] { CDECDataSource.VTYPE_FLOW }, true);
		
		System.out.println("dataInfo:\n" + data.getDataInfo());
		
		Series streamflow = data.getDatasets().get(CommonVariable.STREAMFLOW_CFS);
		
		Reading lastObs = streamflow.getLastObservation();
		
		DateFormat pstFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pstFormat.setTimeZone(USTimeZone.PST.getTimeZone());
		
		assertEquals("12/17/16 2:00:00 PM GMT-08:00", pstFormat.format(lastObs.getDate()));
		assertEquals(3346.0d, lastObs.getValue());
		
		assertEquals(168, streamflow.getReadings().size());
		
		Series gauge_height = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);
		
		Reading firstObs = gauge_height.getReadings().get(0);
		assertEquals("12/10/16 5:00:00 PM GMT-08:00", pstFormat.format(firstObs.getDate()));
		assertEquals(9.69d, firstObs.getValue());

		Site ads = new Site(new SiteId("CDEC","ADS"), null, null, null);
		data = ds.getSiteData(ads, new Variable[] { CDECDataSource.VTYPE_WIND_DR }, true);
		
		System.out.println("dataInfo:\n" + data.getDataInfo());
	}
}
