package com.riverflows.wsclient;

import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class DataSourceControllerTest extends TestCase {

    static {

        DataSourceController.getDataSource("USGS").setHttpClientWrapper(new FileHttpClientWrapper("testdata/usgs/csv/", UsgsCsvDataSource.SITE_DATA_URL));
        DataSourceController.getDataSource("AHPS").setHttpClientWrapper(new FileHttpClientWrapper("testdata/ahps/", AHPSXmlDataSource.SITE_DATA_URL));
    }

	public void testGetFavorites() throws Throwable {
        Variable[] blackVars = DataSourceController.getVariablesFromStrings("USGS", new String[]{"00010"});
        Site blackRiver = new Site(new SiteId("USGS", "04200500"), "Black River at Elyria OH", USState.OH,
                blackVars);

        Site lagoDosBocas = new Site(new SiteId(AHPSXmlDataSource.AGENCY,"argp4"),
                "Lago Dos Bocas at Utuado", -11.1111d, 11.1111d, USState.PR,
                AHPSXmlDataSource.ACCEPTED_VARIABLES);

        List<Favorite> favs = new ArrayList<>();
        favs.add(new Favorite(blackRiver, "00010"));
        favs.add(new Favorite(lagoDosBocas, AHPSXmlDataSource.VTYPE_STAGE.getId()));
        favs.get(0).setId(0);
        favs.get(0).setName("Best Run Ever");
        favs.get(1).setId(1);

		List<FavoriteData> data = DataSourceController.getSiteData(favs, true);

        assertEquals("Best Run Ever", data.get(0).getFavorite().getName());
        //local primary key should be preserved
        assertEquals(Integer.valueOf(0), data.get(0).getFavorite().getId());
        assertEquals(UsgsCsvDataSource.VTYPE_WATER_TEMP_C, data.get(0).getVariable());
        assertNull(data.get(1).getFavorite().getName());
        //local primary key should be preserved
        assertEquals(Integer.valueOf(1), data.get(1).getFavorite().getId());
        assertEquals("argp4", data.get(1).getFavorite().getSite().getSiteId().getId());
        assertEquals("Lago Dos Bocas at Utuado", data.get(1).getSiteData().getSite().getName());
        assertEquals(AHPSXmlDataSource.VTYPE_STAGE, data.get(1).getSeries().getVariable());
        assertEquals(2, data.size());
	}

    public void testGetInitialFavorites() throws Throwable {
        Variable[] blackVars = DataSourceController.getVariablesFromStrings("USGS", new String[]{"00010"});
        Site blackRiver = new Site(new SiteId("USGS", "04200500"), "Black River at Elyria OH", USState.OH,
                blackVars);

        Site lagoDosBocas = new Site(new SiteId(AHPSXmlDataSource.AGENCY,"argp4"),
                "Lago Dos Bocas at Utuado", -11.1111d, 11.1111d, USState.PR,
                AHPSXmlDataSource.ACCEPTED_VARIABLES);

        List<Favorite> favs = new ArrayList<Favorite>();
        favs.add(new Favorite(blackRiver, "00010"));
        //favorite IDs are null because they do not exist in the local DB yet
        favs.add(new Favorite(lagoDosBocas, AHPSXmlDataSource.VTYPE_STAGE.getId()));
        favs.get(0).setName("Best Run Ever");

        List<FavoriteData> data = DataSourceController.getSiteData(favs, true);

        assertEquals("Best Run Ever", data.get(0).getFavorite().getName());
        assertEquals(UsgsCsvDataSource.VTYPE_WATER_TEMP_C, data.get(0).getVariable());
        assertNull(data.get(1).getFavorite().getName());
        assertEquals("argp4", data.get(1).getFavorite().getSite().getSiteId().getId());
        assertEquals("Lago Dos Bocas at Utuado", data.get(1).getSiteData().getSite().getName());
        assertEquals(AHPSXmlDataSource.VTYPE_STAGE, data.get(1).getSeries().getVariable());
        assertEquals(2, data.size());
    }


    public void testHandlePartialResponse() throws Throwable {
        Site fsso3 = new Site(new SiteId(AHPSXmlDataSource.AGENCY,"fsso3"),
                "Nehalem River  AT Foss", -11.1111d, 11.1111d, USState.WA,
                AHPSXmlDataSource.ACCEPTED_VARIABLES);

        List<Favorite> favorites = new ArrayList<Favorite>();

        Favorite fsso3Fav = new Favorite(fsso3, AHPSXmlDataSource.VTYPE_FLOW.getId());

        favorites.add(fsso3Fav);

        Site sori2 = new Site(new SiteId(AHPSXmlDataSource.AGENCY,"sori2"),
                "Du Page River  AT Shorewood", -11.1111d, 11.1111d, USState.MI,
                AHPSXmlDataSource.ACCEPTED_VARIABLES);

        Favorite sori2Fav = new Favorite(sori2, AHPSXmlDataSource.VTYPE_STAGE.getId());

        favorites.add(sori2Fav);

        Site molg1 = new Site(new SiteId(AHPSXmlDataSource.AGENCY,"molg1"),
                "Flint River 3 NW Molena", -11.1111d, 11.1111d, USState.MI,
                AHPSXmlDataSource.ACCEPTED_VARIABLES);

        Favorite molg1Fav = new Favorite(molg1, AHPSXmlDataSource.VTYPE_STAGE.getId());

        favorites.add(molg1Fav);

        List<FavoriteData> favoriteData = DataSourceController.getSiteData(favorites, true);

        assertEquals(3, favoriteData.size());

        FavoriteData fssoData = favoriteData.get(0);

        assertNotNull(fssoData.getSiteData().getSite());

        //Series streamflowDataset = fssoData.getSeries();

        FavoriteData sori2Data = favoriteData.get(1);

        assertNotNull(sori2Data.getSiteData().getSite());
        assertNotNull(sori2Data.getException());
        assertEquals("Error", sori2Data.getSiteData().getDatasets().values().iterator().next().getLastObservation().getQualifiers());

        FavoriteData molg1Data = favoriteData.get(2);

        assertNotNull(molg1Data.getSiteData().getSite());

        //streamflowDataset = molg1Data.getSeries();
    }
}
