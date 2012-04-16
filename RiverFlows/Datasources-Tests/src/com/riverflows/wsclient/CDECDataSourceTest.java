package com.riverflows.wsclient;

import java.util.Calendar;
import java.util.GregorianCalendar;

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
		
		GregorianCalendar lastObsDate = new GregorianCalendar(USTimeZone.PDT.getTimeZone());
		lastObsDate.set(Calendar.YEAR, 2012);
		lastObsDate.set(Calendar.MONTH, Calendar.APRIL);
		lastObsDate.set(Calendar.DAY_OF_MONTH, 14);
		lastObsDate.set(Calendar.HOUR_OF_DAY, 18);
		lastObsDate.set(Calendar.MINUTE, 0);
		lastObsDate.set(Calendar.SECOND, 0);
		lastObsDate.set(Calendar.MILLISECOND, 0);
		
		System.out.println("last streamflow obs date: " + lastObs.getDate());
		assertEquals(lastObsDate.getTime().getTime(), lastObs.getDate().getTime());
		assertEquals(2506.0d, lastObs.getValue());
		
		assertEquals(169, streamflow.getReadings().size());
		
		Series gauge_height = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);
		
		Reading firstObs = gauge_height.getReadings().get(0);
		assertEquals(1.18d, firstObs.getValue());
		
		GregorianCalendar firstObsDate = new GregorianCalendar(USTimeZone.PDT.getTimeZone());
		firstObsDate.set(Calendar.YEAR, 2012);
		firstObsDate.set(Calendar.MONTH, Calendar.APRIL);
		firstObsDate.set(Calendar.DAY_OF_MONTH, 7);
		firstObsDate.set(Calendar.HOUR_OF_DAY, 18);
		firstObsDate.set(Calendar.MINUTE, 0);
		firstObsDate.set(Calendar.SECOND, 0);
		firstObsDate.set(Calendar.MILLISECOND, 0);
	}
}
