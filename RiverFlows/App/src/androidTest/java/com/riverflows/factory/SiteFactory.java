package com.riverflows.factory;

import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.CODWRDataSource;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;

/**
 * Created by robin on 11/16/14.
 */
public class SiteFactory {
    private static Site clearCreek = null;

    public static Site getClearCreek() {

        if(clearCreek != null) return clearCreek;

        clearCreek = new Site();
        clearCreek.setSiteId(new SiteId(CODWRDataSource.AGENCY, "CCACCRCO"));
        clearCreek.setName("CLEAR CREEK ABOVE CLEAR CREEK RESERVOIR");
        clearCreek.setState(USState.CO);
        clearCreek.setSupportedVariables(new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT});

        return clearCreek;
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
