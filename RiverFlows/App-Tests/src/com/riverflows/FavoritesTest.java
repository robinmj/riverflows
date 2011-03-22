package com.riverflows;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import android.test.ActivityInstrumentationTestCase2;

import com.riverflows.data.Favorite;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.SitesDaoImpl;
import com.riverflows.test.AssetAhpsXmlHttpClient;
import com.riverflows.test.AssetCoDwrCsvHttpClient;
import com.riverflows.test.AssetUsgsCsvHttpClient;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.RESTDataSource;

public class FavoritesTest extends ActivityInstrumentationTestCase2<Favorites> {
	private static final Log LOG = LogFactory.getLog(FavoritesTest.class);
	
	public FavoritesTest() {
		super("com.riverflows",Favorites.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		((RESTDataSource)DataSourceController.getDataSource("USGS")).setHttpClientWrapper(new AssetUsgsCsvHttpClient(getInstrumentation().getContext().getAssets()));
		((RESTDataSource)DataSourceController.getDataSource("CODWR")).setHttpClientWrapper(new AssetCoDwrCsvHttpClient(getInstrumentation().getContext().getAssets()));
		((RESTDataSource)DataSourceController.getDataSource("AHPS")).setHttpClientWrapper(new AssetAhpsXmlHttpClient(getInstrumentation().getContext().getAssets()));
	}
	
	public void testLoad() throws Throwable {
		
		//no way to abort loading of sites in onCreate(), so allow it to finish before proceeding
		// to ensure consistent test results
		getActivity();
		waitUntilLoad();
		

		Site animasAtDurango = new Site(new SiteId("AHPS", "DRGC2"), "Animas River at Durango", USState.CO, AHPSXmlDataSource.ACCEPTED_VARIABLES);
		
		SiteData animasAtDurangoData = new SiteData();
		animasAtDurangoData.setSite(animasAtDurango);
		
		ArrayList<SiteData> preloadedSites = new ArrayList<SiteData>();
		preloadedSites.add(animasAtDurangoData);
		
		SitesDaoImpl.saveSites(getActivity().getApplicationContext(), preloadedSites);
		FavoritesDaoImpl.createFavorite(getActivity().getApplicationContext(), new Favorite(animasAtDurango, AHPSXmlDataSource.VTYPE_FLOW.getId()));
		
		getActivity().loadSites(false);
		
		waitUntilLoad();
		
		for(SiteData favoriteData : getActivity().loadTask.gauges) {
			LOG.info("favorite loaded: " + favoriteData.getSite().getAgency() + " " + favoriteData.getSite().getName());
			for(Series dataset : favoriteData.getDatasets().values()) {
				LOG.info(" dataset: " + dataset.getVariable().getId());
			}
		}
		
		assertSitesEqual(animasAtDurango, getActivity().loadTask.gauges.get(0).getSite());
		
		Series streamflow = getActivity().loadTask.gauges.get(0).getDatasets().get(CommonVariable.STREAMFLOW_CFS);
		assertNotNull(streamflow);
		assertTrue(streamflow.getReadings().size() > 0);
		
		assertEquals(1, getActivity().loadTask.gauges.size());
	}
	
	private void assertSitesEqual(Site expected, Site result) {
		assertNotNull(result);
		assertEquals(expected.getKey(), result.getKey());
		assertEquals(expected.getId(), result.getId());
		assertEquals(expected.getAgency(), result.getAgency());
		assertEquals(expected.getName(), result.getName());
		assertEquals(expected.getLatitude(), result.getLatitude());
		assertEquals(expected.getLongitude(), result.getLongitude());
		assertEquals(expected.getState(), result.getState());
		assertEquals(expected.getSupportedVariables().length, result.getSupportedVariables().length);
		assertTrue(Arrays.equals(expected.getSupportedVariables(), result.getSupportedVariables()));
	}

	private void waitUntilLoad() throws Throwable {
		//wait 100 seconds, then give up
		for(int a = 0; a < 1000; a++) {
			Thread.sleep(100);
			if(getActivity().loadTask == null) {
				continue;
			}
			if(getActivity().loadTask.gauges != null) {
				break;
			}
			if(getActivity().loadTask.errorMsg != null) {
				throw new RuntimeException("failed to load data: " + getActivity().loadTask.errorMsg);
			}
		}
	}
}
