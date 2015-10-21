package com.riverflows.factory;

import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UsgsCsvDataSource;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;

/**
 * Created by robin on 11/16/14.
 */
public class SiteFactory {
    private static Site clearCreek = null;
    private static Site fountainCreek = null;

    public static Site getClearCreek() {

        if(clearCreek != null) return clearCreek;

        clearCreek = new Site();
        clearCreek.setSiteId(new SiteId(CODWRDataSource.AGENCY, "CCACCRCO"));
        clearCreek.setName("CLEAR CREEK ABOVE CLEAR CREEK RESERVOIR");
        clearCreek.setState(USState.CO);
        clearCreek.setSupportedVariables(new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT});

        return clearCreek;
    }

    public static Site getFountainCreek() {

        if(fountainCreek != null) return fountainCreek;

        fountainCreek = new Site();
        fountainCreek.setSiteId(new SiteId(UsgsCsvDataSource.AGENCY, "07106000"));
        fountainCreek.setName("FOUNTAIN CREEK NEAR FOUNTAIN, CO.");
        fountainCreek.setState(USState.CO);
        fountainCreek.setSupportedVariables(new Variable[]{
                UsgsCsvDataSource.VTYPE_WATER_TEMP_C,
                UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS,
                UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT,
                UsgsCsvDataSource.VTYPE_DISSOLVED_O2_MG_L,
                UsgsCsvDataSource.VTYPE_WATER_PH
        });

        return fountainCreek;
    }

    public static Site getVallecitoCreek() {

        Variable[] vars = { UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS, UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT };
        return new Site(new SiteId("USGS", "09352900"), "Vallecito Creek Near Bayfield, CO", USState.CO,
                vars);

    }

    public static Site getSouthPlatteAtNorthPlatte() {
        Variable[] vars = { UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS, UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT };
        return new Site(new SiteId("USGS", "06765500"), "South Platte River at North Platte, NE", USState.NE, vars);
    }

    public static Site getVerde() {
        Variable[] vars = { UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS, UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT };
        return new Site(new SiteId("USGS", "09510000"), "VERDE RIVER BELOW BARTLETT DAM, AZ", USState.AZ, vars);
    }

    public static Matcher<Site> matches(final Site s) {
        return new BaseMatcher<Site>() {
            @Override
            public boolean matches(Object item) {
                if(item == null) return false;
                try {
                    Site otherSite = (Site)item;
                    return s.getSiteId().equals(otherSite.getSiteId());
                }catch(ClassCastException cce) {
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }
}
