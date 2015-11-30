package com.riverflows.factory;

import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.UserAccount;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.CODWRDataSource;

/**
 * TODO figure out how to consolidate this with the version in App. Maybe after moving
 * Datasources-Tests into Datasources?
 */
public final class DestinationFacetFactory {
    public static DestinationFacet getClearCreekKayak() {

        Site clearCreek = new Site();
        clearCreek.setSiteId(new SiteId(CODWRDataSource.AGENCY, "CCACCRCO", 20161));
        clearCreek.setName("CLEAR CREEK ABOVE CLEAR CREEK RESERVOIR");
        clearCreek.setState(USState.CO);
        clearCreek.setSupportedVariables(new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT});

        Destination clearCreekDest = new Destination();
        clearCreekDest.setUser(new UserAccount());
        clearCreekDest.setName("Clear Creek");
        clearCreekDest.setShared(true);
        clearCreekDest.setSite(clearCreek);

        DestinationFacet clearCreekKayak = new DestinationFacet();
        clearCreekKayak.setHigh(800.0);
        clearCreekKayak.setMed(600.0);
        clearCreekKayak.setLow(400.0);
        clearCreekKayak.setUser(new UserAccount());
        clearCreekKayak.setDestination(clearCreekDest);
        clearCreekKayak.setVariable(CODWRDataSource.VTYPE_STREAMFLOW_CFS);

        return clearCreekKayak;
    }
}
