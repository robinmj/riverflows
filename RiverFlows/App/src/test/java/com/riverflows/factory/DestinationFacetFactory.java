package com.riverflows.factory;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.UserAccount;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;

/**
 * Created by robin on 11/16/14.
 */
public final class DestinationFacetFactory {
    public static DestinationFacet getClearCreekKayak() {
        DestinationFacet clearCreekKayak = new DestinationFacet();
        clearCreekKayak.setId(23);
        clearCreekKayak.setHigh(800.0);
        clearCreekKayak.setMed(600.0);
        clearCreekKayak.setLow(400.0);
        clearCreekKayak.setUser(new UserAccount());
        clearCreekKayak.setDestination(DestinationFactory.getClearCreek());
        clearCreekKayak.setVariable(CODWRDataSource.VTYPE_STREAMFLOW_CFS);

        return clearCreekKayak;
    }

    public static DestinationFacet getFountainCreekKayak() {
        DestinationFacet facet = new DestinationFacet();
        facet.setId(56);
        facet.setHigh(6.0);
        facet.setMed(4.0);
        facet.setLow(3.0);
        facet.setUser(new UserAccount());
        facet.setDestination(DestinationFactory.getFountainCreek());
        facet.setVariable(UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT);
        facet.setFacetType(2);
        return facet;
    }
}
