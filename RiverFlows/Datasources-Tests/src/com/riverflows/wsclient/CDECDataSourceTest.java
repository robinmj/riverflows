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
		
		DateFormat pdtFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pdtFormat.setTimeZone(USTimeZone.PDT.getTimeZone());
		
		assertEquals("4/17/12 12:00:00 PM GMT-07:00", pdtFormat.format(lastObs.getDate()));
		assertEquals(2551.0d, lastObs.getValue());
		
		assertEquals(167, streamflow.getReadings().size());
		
		Series gauge_height = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);
		
		Reading firstObs = gauge_height.getReadings().get(0);
		assertEquals("4/10/12 2:00:00 PM GMT-07:00", pdtFormat.format(firstObs.getDate()));
		assertEquals(4.14d, firstObs.getValue());
	}
}
