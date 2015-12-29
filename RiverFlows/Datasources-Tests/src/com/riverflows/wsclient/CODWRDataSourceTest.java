package com.riverflows.wsclient;

import com.riverflows.DataSourceTestCase;
import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Reading;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import co.freeside.betamax.TapeMode;

public class CODWRDataSourceTest extends DataSourceTestCase {

	private final CODWRDataSource src = new CODWRDataSource();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        src.setHttpClientWrapper(httpClientWrapper);
    }

	public void testGetStartDate() {

		Date endDate = new Date(1451395259233l);
		Date startDate = src.getStartDate(endDate);

		assertEquals("12/29/15", CODWRDataSource.rangeDateFormat.format(endDate));
		assertEquals(7, (endDate.getTime() - startDate.getTime()) / (long)(24 * 60 * 60 * 1000));
		assertEquals("12/22/15", CODWRDataSource.rangeDateFormat.format(startDate));
	}

    public void testGetSites() throws Throwable {
        recorder.insertTape("codwr_getSites", Collections.singletonMap("mode", TapeMode.READ_SEQUENTIAL));

		Site clearCreek = new Site();
		clearCreek.setSiteId(new SiteId(CODWRDataSource.AGENCY, "CCACCRCO"));
		clearCreek.setName("CLEAR CREEK ABOVE CLEAR CREEK RESERVOIR");
		clearCreek.setState(USState.CO);
		clearCreek.setSupportedVariables(new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT});
		
		SiteData result = src.getSiteData(clearCreek, new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT}, true, new GregorianCalendar(2015, 2, 30).getTime());
		
		System.out.println(result.getDataInfo());
		
		assertTrue(result.getDataInfo().contains(clearCreek.getId()));
		assertTrue(result.getDataInfo().contains(clearCreek.getName()));

		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2015, Calendar.MARCH, 23, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("GMT-06:00"));
		
		Reading r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(15.7, r.getValue());
		assertNull(r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(2.83d, r.getValue());
		assertNull(r.getQualifiers());

        cal.set(2015, Calendar.MARCH, 23, 0, 15, 0);
		
		r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(15.7d, r.getValue());
		assertNull(r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(2.83d, r.getValue());
		assertNull(r.getQualifiers());

        cal.set(2015, Calendar.MARCH, 23, 0, 45, 0);
		
		r = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings().get(3);
		assertEquals(cal.getTime(), r.getDate());
        assertEquals(15.0d, r.getValue());
		assertNull(r.getQualifiers());
		
		r = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings().get(3);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(2.82d, r.getValue());
		assertNull(r.getQualifiers());

		//test unsupported variables
		//nonexistent site
		//site with no data (PLABAICO)
		//variable with qualified null
	}

    public void testGetFavorites() throws Throwable {
        recorder.insertTape("codwr_getFavorites", Collections.singletonMap("mode", TapeMode.READ_SEQUENTIAL));
        Site lakeCreek = new Site();
        lakeCreek.setSiteId(new SiteId(CODWRDataSource.AGENCY, "LAKATLCO"));
        lakeCreek.setName("LAKE CREEK ABOVE TWIN LAKES");
        lakeCreek.setState(USState.CO);
        lakeCreek.setSupportedVariables(new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_AIRTEMP});

        List<Favorite> favs = Collections.singletonList(new Favorite(lakeCreek, CODWRDataSource.VTYPE_STREAMFLOW_CFS.getId()));

        List<FavoriteData> result = src.getSiteData(favs, true, new GregorianCalendar(2015, 2, 30).getTime());

        Reading r = result.get(0).getSeries().getLastObservation();

        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2015, Calendar.MARCH, 30, 13, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone("GMT-06:00"));

        assertEquals(cal.getTime(), r.getDate());
        assertEquals(63.1d, r.getValue());
        assertNull(r.getQualifiers());
        assertEquals(favs.get(0), result.get(0).getFavorite());
    }
}
