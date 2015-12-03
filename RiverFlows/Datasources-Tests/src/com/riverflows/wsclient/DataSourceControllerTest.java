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

        DataSourceController.getDataSource("USGS").setHttpClientWrapper(new MockUsgsCsvHttpClient());
        DataSourceController.getDataSource("AHPS").setHttpClientWrapper(new MockAHPSHttpClient());
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
}
