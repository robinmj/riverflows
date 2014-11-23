package com.riverflows.wsclient;

import com.riverflows.WebModelTestCase;
import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.factory.DestinationFacetFactory;

import java.util.Collections;

import co.freeside.betamax.TapeMode;

/**
 * Created by robin on 11/4/14.
 */
public class DestinationsTest extends WebModelTestCase {

    private DestinationFacets destinationFacets = new DestinationFacets();

    private Destinations destinations = new Destinations();

    public void testUpdateDestination() throws Throwable {
        recorder.insertTape("testUpdateDestination", Collections.singletonMap("mode", TapeMode.READ_SEQUENTIAL));

        DestinationFacet testFacet = destinationFacets.get(session, 20);
        Destination dest = testFacet.getDestination();

        assertEquals("lower t box", dest.getName());

        dest.setName("Lower Taos Box");

        destinations.update(session, dest);

        testFacet = destinationFacets.get(session, 20);
        dest = testFacet.getDestination();

        assertEquals("Lower Taos Box", dest.getName());

        //reverse the change
        dest.setName("lower t box");
        destinations.update(session, dest);
    }

    public void testSaveDestinationWithFacet() throws Throwable {
        recorder.insertTape("testSaveDestinationWithFacet");

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();

        clearCreekKayak = destinations.saveDestinationWithFacet(session, clearCreekKayak);

        assertNotNull(clearCreekKayak.getId());
        assertNotNull(clearCreekKayak.getDestination().getSite().getName());
        assertNotNull(clearCreekKayak.getDestination().getSite().getSiteId().getPrimaryKey());
        assertNotNull(clearCreekKayak.getVariable().getCommonVariable());
        assertNotNull(clearCreekKayak.getVariable().getName());

        clearCreekKayak = destinationFacets.get(session, clearCreekKayak.getId());
    }
}
