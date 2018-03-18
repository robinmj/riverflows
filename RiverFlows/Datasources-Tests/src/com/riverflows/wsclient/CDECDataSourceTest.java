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

	public void testGetFavorites() throws Throwable {
		Site cbr = new Site(new SiteId("CDEC", "CBR"), null, null, null);

		List<Favorite> favs = new ArrayList<Favorite>();
		favs.add(new Favorite(cbr, CDECDataSource.VTYPE_FLOW.getId()));

		List<FavoriteData> result = ds.getSiteData(favs, true);

		assertEquals(favs.get(0), result.get(0).getFavorite());

		SiteData data = result.get(0).getSiteData();

		System.out.println("dataInfo:\n" + data.getDataInfo());

		Series streamflow = data.getDatasets().get(CommonVariable.STREAMFLOW_CFS);

		Reading lastObs = streamflow.getLastObservation();

		DateFormat pstFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pstFormat.setTimeZone(USTimeZone.PST.getTimeZone());

		assertEquals("3/15/18 4:00:00 AM GMT-08:00", pstFormat.format(lastObs.getDate()));
		assertEquals(2344.0d, lastObs.getValue());

		assertEquals(165, streamflow.getReadings().size());

		Series gauge_height = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);

		Reading firstObs = gauge_height.getReadings().get(0);
		assertEquals("3/8/18 6:00:00 AM GMT-08:00", pstFormat.format(firstObs.getDate()));
		assertEquals(1.85d, firstObs.getValue());
	}

	public void testGetDataInfo() throws Throwable {

		Site ads = new Site(new SiteId("CDEC","ADS"), null, null, null);
		SiteData data = ds.getSiteData(ads, new Variable[] { CDECDataSource.VTYPE_WIND_DR }, true);

		System.out.println("dataInfo:\n" + data.getDataInfo());
	}

	public void testGetNonexistentFavorites() throws Throwable {
		Site fake = new Site(new SiteId("CDEC","ABCDEFG"), null, null, null);
		Site cbr = new Site(new SiteId("CDEC","CBR"), null, null, null);

		List<Favorite> favs = new ArrayList<Favorite>();
		favs.add(new Favorite(fake, CDECDataSource.VTYPE_BAT_VOL.getId()));
		favs.add(new Favorite(cbr, CDECDataSource.VTYPE_FLOW.getId()));

		List<FavoriteData> result = ds.getSiteData(favs, true);

		assertEquals(favs.get(0), result.get(0).getFavorite());
		assertEquals(favs.get(1), result.get(1).getFavorite());

		SiteData data = result.get(0).getSiteData();

		assertNull(data.getDatasets().get(CDECDataSource.VTYPE_BAT_VOL.getCommonVariable()).getLastObservation().getValue());
		assertEquals("Datasource Down", data.getDatasets().get(CDECDataSource.VTYPE_BAT_VOL.getCommonVariable()).getLastObservation().getQualifiers());

		data = result.get(1).getSiteData();

		System.out.println("dataInfo:\n" + data.getDataInfo());

		Series streamflow = data.getDatasets().get(CommonVariable.STREAMFLOW_CFS);

		Reading lastObs = streamflow.getLastObservation();

		DateFormat pstFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pstFormat.setTimeZone(USTimeZone.PST.getTimeZone());

		assertEquals("3/15/18 4:00:00 AM GMT-08:00", pstFormat.format(lastObs.getDate()));
		assertEquals(2344.0d, lastObs.getValue());

		assertEquals(165, streamflow.getReadings().size());

		Series gauge_height = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);

		Reading firstObs = gauge_height.getReadings().get(0);
		assertEquals("3/8/18 6:00:00 AM GMT-08:00", pstFormat.format(firstObs.getDate()));
		assertEquals(1.85d, firstObs.getValue());
	}
	
	public void testGetSiteData() throws Throwable {
		Site cbr = new Site(new SiteId("CDEC","CBR"), null, null, null);
		SiteData data = ds.getSiteData(cbr, new Variable[] { CDECDataSource.VTYPE_FLOW }, true);

		System.out.println("dataInfo:\n" + data.getDataInfo());

		Series streamflow = data.getDatasets().get(CommonVariable.STREAMFLOW_CFS);

		Reading lastObs = streamflow.getLastObservation();

		DateFormat pstFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pstFormat.setTimeZone(USTimeZone.PST.getTimeZone());

		assertEquals("3/15/18 4:00:00 AM GMT-08:00", pstFormat.format(lastObs.getDate()));
		assertEquals(2344.0d, lastObs.getValue());

		assertEquals(165, streamflow.getReadings().size());

		Series gauge_height = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);

		Reading firstObs = gauge_height.getReadings().get(0);
		assertEquals("3/8/18 6:00:00 AM GMT-08:00", pstFormat.format(firstObs.getDate()));
		assertEquals(1.85d, firstObs.getValue());
	}

	public void testGetGVOData() throws Throwable {
		Site cbr = new Site(new SiteId("CDEC","GVO"), null, null, null);
		SiteData data = ds.getSiteData(cbr, new Variable[] { CDECDataSource.VTYPE_FLOW }, true);

		System.out.println("dataInfo:\n" + data.getDataInfo());

		Series gaugeHeight = data.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);

		Reading lastObs = gaugeHeight.getLastObservation();

		DateFormat pstFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		pstFormat.setTimeZone(USTimeZone.PST.getTimeZone());

		assertEquals("3/16/18 9:00:00 AM GMT-08:00", pstFormat.format(lastObs.getDate()));
		assertEquals(0.38d, lastObs.getValue());

		for(Reading r : gaugeHeight.getReadings()) {
			if(r.getValue() == null) {
				assertEquals("N", r.getQualifiers());
			} else {
				assertTrue("" + r.getValue(), r.getValue() < 1.0);
				assertTrue("" + r.getValue(), r.getValue() > 0.0);
			}
		}
	}
}
