package com.riverflows.wsclient;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import com.riverflows.data.Reading;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

public class CODWRDataSourceTest extends TestCase {

	private static final CODWRDataSource src = new CODWRDataSource();
	
	static {
		src.setHttpClientWrapper(new MockCoDwrCsvHttpClient("testdata/codwr/"));
	}

	public void testGetSites() throws Throwable {
		Site clearCreek = new Site();
		clearCreek.setSiteId(new SiteId(CODWRDataSource.AGENCY, "CCACCRCO"));
		clearCreek.setName("CLEAR CREEK ABOVE CLEAR CREEK RESERVOIR");
		clearCreek.setState(USState.CO);
		clearCreek.setSupportedVariables(new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT});
		
		SiteData result = src.getSiteData(clearCreek, new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT}, true);
		
		System.out.println(result.getDataInfo());
		
		assertTrue(result.getDataInfo().contains(clearCreek.getId()));
		assertTrue(result.getDataInfo().contains(clearCreek.getName()));

		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2011, 2, 7, 13, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("GMT-07:00"));
		
		Reading r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertNull(r.getValue());
		assertEquals("B", r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(-541.18d, r.getValue());
		assertNull(r.getQualifiers());
		
		cal.set(2011, 2, 7, 13, 30, 0);
		
		r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(1);
		assertEquals(cal.getTime(), r.getDate());
		assertNull(r.getValue());
		assertEquals("B", r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(-312.08d, r.getValue());
		assertNull(r.getQualifiers());
		
		cal.set(2011, 2, 7, 13, 45, 0);
		
		r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(2);
		assertEquals(cal.getTime(), r.getDate());
		assertNull(r.getValue());
		assertEquals("E", r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(2);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(1077.06d, r.getValue());
		assertNull(r.getQualifiers());
		
		//test unsupported variables
		//nonexistent site
		//site with no data (PLABAICO)
		//variable with qualified null
	}
}
