package com.riverflows.factory;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.UserAccount;
import com.riverflows.wsclient.CODWRDataSource;

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
}
