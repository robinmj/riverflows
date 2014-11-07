package com.riverflows.wsclient;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.UserAccount;
import com.riverflows.data.Variable;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import java.util.List;

/**
 * Created by robin on 10/14/14.
 */
public class DestinationFacetsTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(DestinationFacetsTest.class);

    public void testGetFavorites() throws Throwable {
        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        List<DestinationFacet> favorites = DestinationFacets.instance.getFavorites(session);

        for(DestinationFacet facet: favorites) {
            LOG.info("destination_facet.id=" + facet.getId());
            assertNotNull(facet.getVariable().getCommonVariable());
            assertNotNull(facet.getDestination().getId());
        }
    }

    public void testSaveFavorite() throws Throwable {
        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        Favorite result = DestinationFacets.instance.saveFavorite(session, 20);

        assertEquals(20, result.getDestinationFacet().getId().intValue());
    }

    public void testRemoveFavorite() throws Throwable {
        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        DestinationFacets.instance.removeFavorite(session, 20);
    }

    public void testRemoveNonexistantFavorite() throws Throwable {
        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        try {
            DestinationFacets.instance.removeFavorite(session, 0);
        } catch(UnexpectedResultException ure) {
            assertEquals(HttpStatus.SC_GONE, ure.getStatusCode());
        }
    }

    public void testGetDestinationFacet() throws Throwable {

        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        DestinationFacet testFacet = DestinationFacets.instance.get(session, 20);

        assertEquals(20, testFacet.getId().intValue());
        assertNull(testFacet.getDescription());
        assertEquals(2, testFacet.getUser().getId().intValue());
        assertNull(testFacet.getTooLow());
        assertEquals(1000.0, testFacet.getLow());
        assertEquals(1500.0, testFacet.getMed());
        assertEquals(2500.0, testFacet.getHigh());
        assertNull(testFacet.getHighPlus());
        assertNull(testFacet.getLowDifficulty());
        assertNull(testFacet.getMedDifficulty());
        assertNull(testFacet.getHighDifficulty());
        assertNotNull(testFacet.getCreationDate());
        assertNotNull(testFacet.getModificationDate());
        assertEquals(2, testFacet.getFacetType());
        assertEquals("08276500", testFacet.getDestination().getSite().getId());
        assertEquals("00060", testFacet.getVariable().getId());
        assertNull(testFacet.getLowPortDifficulty());
        assertNull(testFacet.getMedPortDifficulty());
        assertNull(testFacet.getHighPortDifficulty());
        assertNull(testFacet.getQualityLow());
        assertNull(testFacet.getQualityMed());
        assertNull(testFacet.getQualityHigh());
    }

    public void testUpdateDestinationFacet() throws Throwable {
        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        DestinationFacet testFacet = DestinationFacets.instance.get(session, 20);

        testFacet.setHighPlus(5000.0);
        testFacet.setLowDifficulty(8);

        DestinationFacets.instance.update(session, testFacet);

        DestinationFacet updatedFacet = DestinationFacets.instance.get(session, 20);

        assertNull(updatedFacet.getDescription());
        assertNull(updatedFacet.getTooLow());
        assertEquals(1000.0, updatedFacet.getLow());
        assertEquals(1500.0, updatedFacet.getMed());
        assertEquals(2500.0, updatedFacet.getHigh());
        assertEquals(5000.0, updatedFacet.getHighPlus());
        assertEquals(8, updatedFacet.getLowDifficulty().intValue());
        assertNull(updatedFacet.getMedDifficulty());
        assertNull(updatedFacet.getHighDifficulty());
        assertNotNull(updatedFacet.getCreationDate());
        assertNotNull(updatedFacet.getModificationDate());
        assertEquals(2, updatedFacet.getFacetType());
        assertEquals("08276500", updatedFacet.getDestination().getSite().getId());
        assertEquals("00060", updatedFacet.getVariable().getId());
        assertNull(updatedFacet.getLowPortDifficulty());
        assertNull(updatedFacet.getMedPortDifficulty());
        assertNull(updatedFacet.getHighPortDifficulty());
        assertNull(updatedFacet.getQualityLow());
        assertNull(updatedFacet.getQualityMed());
        assertNull(updatedFacet.getQualityHigh());

        //revert database
        testFacet.setHighPlus(null);
        testFacet.setLowDifficulty(null);
        DestinationFacets.instance.update(session, testFacet);
        updatedFacet = DestinationFacets.instance.get(session, 20);
        assertNull(updatedFacet.getHighPlus());
        assertNull(updatedFacet.getLowDifficulty());
    }

    public void testUpdateNonexistentDestinationFacet() throws Throwable {
        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        DestinationFacet testFacet = new DestinationFacet();
        testFacet.setId(0);
        testFacet.setVariable(new Variable(null, "FLOW", null));

        try {
            DestinationFacets.instance.update(session, testFacet);
        } catch(UnexpectedResultException ure) {
            assertEquals(HttpStatus.SC_NOT_FOUND, ure.getStatusCode());
        }
    }
}
