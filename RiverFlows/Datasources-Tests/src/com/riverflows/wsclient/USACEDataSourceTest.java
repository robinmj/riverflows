package com.riverflows.wsclient;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Reading;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.wsclient.USACEDataSource;

import junit.framework.TestCase;


public class USACEDataSourceTest extends TestCase {
	public static final USACEDataSource src = new USACEDataSource();

	static {
		src.setHttpClientWrapper(new MockUSACEHttpClient());
	}


	public void testGetNonexistantFavorite() throws Throwable {

		//no testdata for this site, so we will get a 404
		Site efga4 = new Site(new SiteId("USACE", "03276000"), "", USState.NY,
				USACEDataSource.ACCEPTED_VARIABLES);

		Site mgoi4 = new Site(new SiteId("USACE", "MGOI4"), "Indian Creek near Mingo, IA", USState.IA,
				USACEDataSource.ACCEPTED_VARIABLES);

		List<Favorite> favs = new ArrayList<Favorite>();
		favs.add(new Favorite(efga4, USACEDataSource.STAGE.getId()));
		favs.add(new Favorite(mgoi4, USACEDataSource.STAGE.getId()));

		List<FavoriteData> result = src.getSiteData(favs, true);

		assertEquals(favs.get(0), result.get(0).getFavorite());
		assertEquals(favs.get(1), result.get(1).getFavorite());

		assertEquals("Datasource Down", result.get(0).getSiteData().getDatasets().get(USACEDataSource.STAGE.getCommonVariable()).getLastObservation().getQualifiers());

		Reading mgoi4LastFlowObs = result.get(1).getSiteData().getDatasets().get(USACEDataSource.FLOW.getCommonVariable()).getLastObservation();
		Reading mgoi4LastStageObs = result.get(1).getSiteData().getDatasets().get(USACEDataSource.STAGE.getCommonVariable()).getLastObservation();

		assertEquals(5.57d, mgoi4LastStageObs.getValue());
		assertEquals(427.0d, mgoi4LastFlowObs.getValue());
	}
	
	public void testGetSiteData() throws Exception {
		Site genesee = new Site(new SiteId("USACE", "BLBN6"), "Genesee River at Ballantyne Bridge near Mortimer, NY", USState.NY,
				USACEDataSource.ACCEPTED_VARIABLES);

		SiteData result = src.getSiteData(genesee, new Variable[]{ USACEDataSource.STAGE }, true);

		assertTrue(result.getDataInfo().contains(genesee.getName()));
		result.getDataInfo().contains(USACEDataSource.SITE_DATA_URL + "Districts/LRB/districtdefault.cfm");

		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2013, 4, 13, 12, 00, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("US/Eastern"));

		List<Reading> gageHtReadings = result.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT).getReadings();
		List<Reading> precipReadings = result.getDatasets().get(CommonVariable.PRECIPITATION_TOTAL_IN).getReadings();

		Reading r = gageHtReadings.get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(11.88d, r.getValue());
		assertNull(r.getQualifiers());

		r = precipReadings.get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(67.0d, r.getValue());
		assertNull(r.getQualifiers());

		cal = new GregorianCalendar();
		cal.set(2013, 4, 14, 8, 00, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("US/Eastern"));

		r = gageHtReadings.get(20);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(11.71d, r.getValue());
		assertNull(r.getQualifiers());

		r = precipReadings.get(20);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(67.0d, r.getValue());
		assertNull(r.getQualifiers());

		cal = new GregorianCalendar();
		cal.set(2013, 4, 20, 11, 00, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("US/Eastern"));

		r = gageHtReadings.get(gageHtReadings.size() - 1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(11.53d, r.getValue());
		assertNull(r.getQualifiers());

		r = precipReadings.get(precipReadings.size() - 1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(67.26d, r.getValue());
		assertNull(r.getQualifiers());

		assertEquals(168, gageHtReadings.size());
		assertEquals(168, precipReadings.size());
	}

	public void testGetHockett() throws Exception {

		Site hockett = new Site(new SiteId("USACE", "HCK"), "Hockett Meadow Weather", USState.CA,
				USACEDataSource.ACCEPTED_VARIABLES);

		SiteData result = src.getSiteData(hockett, new Variable[]{USACEDataSource.AIRTEMP_F}, true);

		assertTrue(result.getDataInfo().contains(hockett.getName()));
		result.getDataInfo().contains(USACEDataSource.SITE_DATA_URL + "Districts/LRB/districtdefault.cfm");

		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2013, 4, 15, 7, 00, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("US/Pacific"));

		List<Reading> airtempReadings = result.getDatasets().get(CommonVariable.AIRTEMP_F).getReadings();
		List<Reading> precipReadings = result.getDatasets().get(CommonVariable.PRECIPITATION_TOTAL_IN).getReadings();

		Reading r = airtempReadings.get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(31.0d, r.getValue());
		assertNull(r.getQualifiers());

		r = precipReadings.get(0);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(23.64d, r.getValue());
		assertNull(r.getQualifiers());

		cal = new GregorianCalendar();
		cal.set(2013, 4, 16, 4, 00, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("US/Pacific"));

		r = airtempReadings.get(20);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(28.0d, r.getValue());
		assertNull(r.getQualifiers());

		r = precipReadings.get(20);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(23.64d, r.getValue());
		assertNull(r.getQualifiers());

		cal = new GregorianCalendar();
		cal.set(2013, 4, 22, 6, 00, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("US/Pacific"));

		r = airtempReadings.get(airtempReadings.size() - 1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(28.0d, r.getValue());
		assertNull(r.getQualifiers());

		r = precipReadings.get(precipReadings.size() - 1);
		assertEquals(cal.getTime(), r.getDate());
		assertEquals(23.64d, r.getValue());
		assertNull(r.getQualifiers());

		assertEquals(165, airtempReadings.size());
		assertEquals(165, precipReadings.size());

	}

	public void testGetRogue() throws Exception {
		Site rygo = new Site(new SiteId("USACE", "RYGO"), "Rogue River at Raygold", USState.OR,
				USACEDataSource.ACCEPTED_VARIABLES);
		
		SiteData result = src.getSiteData(rygo, new Variable[]{ USACEDataSource.ELEVATION }, true);

		List<Reading> elevReadings = result.getDatasets().get(CommonVariable.RES_ELEVATION_FT).getReadings();
		List<Reading> watertempReadings = result.getDatasets().get(CommonVariable.WATERTEMP_F).getReadings();
		List<Reading> flowReadings = result.getDatasets().get(CommonVariable.STREAMFLOW_CFS).getReadings();

		assertEquals(0, elevReadings.size());
		assertEquals(0, watertempReadings.size());
		assertEquals(0, flowReadings.size());
	}

	public void testGetEFGA4() throws Exception {
		Site efga4 = new Site(new SiteId("USACE", "EFGA4"), "", USState.NY,
				USACEDataSource.ACCEPTED_VARIABLES);

		SiteData result = src.getSiteData(efga4, new Variable[]{USACEDataSource.STAGE}, true);
	}

	public void testGetQLDI2() throws Exception {
		Site qldi2 = new Site(new SiteId("USACE", "QLDI2"), "", USState.NY,
				USACEDataSource.ACCEPTED_VARIABLES);

		SiteData result = src.getSiteData(qldi2, new Variable[]{USACEDataSource.STAGE}, true);
	}

	public void testGetSORI2() throws Exception {
		Site sori2 = new Site(new SiteId("USACE", "SORI2"), "", USState.NY,
				USACEDataSource.ACCEPTED_VARIABLES);

		SiteData result = src.getSiteData(sori2, new Variable[]{USACEDataSource.FLOW}, true);
	}
}
