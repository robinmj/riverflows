package com.riverflows.wsclient;

import com.riverflows.WebModelTestCase;
import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.SiteId;
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
        assertFalse("not shared publicly", dest.isShared());

        dest.setName("Lower Taos Box");

        dest.setShared(true);
        destinations.update(session, dest);

        testFacet = destinationFacets.get(session, 20);
        dest = testFacet.getDestination();

        assertEquals("Lower Taos Box", dest.getName());
        assertTrue("shared publicly", dest.isShared());

        //reverse the change
        dest.setName("lower t box");
        dest.setShared(false);
        destinations.update(session, dest);

        assertFalse("not shared publicly", dest.isShared());
    }

    public void testSaveDestinationWithFacet() throws Throwable {
        recorder.insertTape("testSaveDestinationWithFacet");

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        clearCreekKayak.getDestination().setShared(true);

        clearCreekKayak = destinations.saveDestinationWithFacet(session, clearCreekKayak);

        assertNotNull(clearCreekKayak.getId());
        assertNotNull(clearCreekKayak.getDestination().getSite().getName());
        assertNotNull(clearCreekKayak.getDestination().getSite().getSiteId().getPrimaryKey());
        assertNotNull(clearCreekKayak.getVariable().getCommonVariable());
        assertNotNull(clearCreekKayak.getVariable().getName());

        assertTrue("shared publicly", clearCreekKayak.getDestination().isShared());

        clearCreekKayak = destinationFacets.get(session, clearCreekKayak.getId());

        assertTrue("shared publicly", clearCreekKayak.getDestination().isShared());
    }

    public void testSaveDestinationWithoutSitePk() throws Throwable {
        recorder.insertTape("testSaveDestinationWithoutSitePk");

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        clearCreekKayak.getDestination().setShared(true);
        clearCreekKayak.getDestination().getSite().getSiteId().setPrimaryKey(null);

        clearCreekKayak = destinations.saveDestinationWithFacet(session, clearCreekKayak);

        assertNotNull(clearCreekKayak.getId());
        assertNotNull(clearCreekKayak.getDestination().getSite().getName());
        assertNotNull(clearCreekKayak.getDestination().getSite().getSiteId().getPrimaryKey());
        assertNotNull(clearCreekKayak.getVariable().getCommonVariable());
        assertNotNull(clearCreekKayak.getVariable().getName());
    }

    public void testSaveDestinationWithNonexistantAgencyId() throws Throwable {
        recorder.insertTape("testSaveDestinationWithNonexistantAgencyId");

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        SiteId oldSiteId = clearCreekKayak.getDestination().getSite().getSiteId();

        clearCreekKayak.getDestination().getSite().setSiteId(new SiteId(oldSiteId.getAgency(), "invalid agency_specific_id"));

        try {
            destinations.saveDestinationWithFacet(session, clearCreekKayak);
            throw new RuntimeException("expected UnexpectedResultException");
        } catch(UnexpectedResultException ure) {
            assertEquals(422, ure.getStatusCode());
        }

    }
}
