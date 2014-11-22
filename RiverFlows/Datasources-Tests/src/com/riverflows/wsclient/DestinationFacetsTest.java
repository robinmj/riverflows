package com.riverflows.wsclient;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Page;
import com.riverflows.data.USState;
import com.riverflows.data.UserAccount;
import com.riverflows.data.Variable;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.junit.Rule;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import co.freeside.betamax.Betamax;
import co.freeside.betamax.Recorder;
import co.freeside.betamax.TapeMode;
import co.freeside.betamax.httpclient.BetamaxHttpClient;

/**
 * Created by robin on 10/14/14.
 */
public class DestinationFacetsTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(DestinationFacetsTest.class);

    @Rule
    public Recorder recorder = new Recorder();

    private WsSession session = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        DestinationFacets.setHttpClientFactory(new HttpClientFactory() {
            BetamaxHttpClient client = new BetamaxHttpClient(recorder);

            @Override
            public HttpClient getHttpClient() {
                return client;
            }
        });
    }

    @Override
    protected void tearDown() throws Exception {
        recorder.ejectTape();
        super.tearDown();
    }

    public void testGetFavorites() throws Throwable {
        recorder.insertTape("testGetFavorites");

        List<DestinationFacet> favorites = DestinationFacets.instance.getFavorites(session);

        for(DestinationFacet facet: favorites) {
            LOG.info("destination_facet.id=" + facet.getId());
            assertNotNull(facet.getVariable().getCommonVariable());
            assertNotNull(facet.getDestination().getId());
        }
    }

    public void testGetDestinationFacets() throws Throwable {
        recorder.insertTape("testGetDestinationFacets");

        Page<DestinationFacet> facets = DestinationFacets.instance.get(session,
                Collections.singletonMap("state", Collections.singletonList(USState.AZ.getAbbrev())),
                null,
                null);

        assertEquals(0, facets.pageElements.size());
        assertNull(facets.totalElementCount);

        facets = DestinationFacets.instance.get(session,
                Collections.singletonMap("state", Collections.singletonList(USState.CA.getAbbrev())),
                null,
                null);

        assertEquals(1, facets.pageElements.size());
        assertNull(facets.totalElementCount);

        facets = DestinationFacets.instance.get(session,
                Collections.singletonMap("state", Collections.singletonList(USState.CO.getAbbrev())),
                null,
                4);

        assertEquals(4, facets.pageElements.size());
        assertEquals(4, facets.totalElementCount.intValue());

        facets = DestinationFacets.instance.get(session,
                Collections.singletonMap("state", Collections.singletonList(USState.CO.getAbbrev())),
                2,
                4);

        assertEquals(4, facets.pageElements.size());
        assertEquals(4, facets.totalElementCount.intValue());

        facets = DestinationFacets.instance.get(session,
                Collections.singletonMap("state", Collections.singletonList(USState.CO.getAbbrev())),
                null,
                null);

        assertEquals(11, facets.pageElements.size());
        assertNull(facets.totalElementCount);

        HashMap<String, List<String>> params = new HashMap<String, List<String>>();
        params.put("state", Collections.singletonList(USState.CO.getAbbrev()));
        params.put("facet_types", Collections.singletonList("2"));

        facets = DestinationFacets.instance.get(session,
                params,
                null,
                null);

        assertEquals(8, facets.pageElements.size());
        assertNull(facets.totalElementCount);

        assertNotNull(facets.pageElements.get(3).getDestination());
        assertNotNull(facets.pageElements.get(3).getDestination().getSite());
    }

    public void testSaveFavorite() throws Throwable {
        recorder.insertTape("testSaveFavorite");

        Favorite result = DestinationFacets.instance.saveFavorite(session, 20);

        assertEquals(20, result.getDestinationFacet().getId().intValue());
    }

    public void testRemoveFavorite() throws Throwable {
        recorder.insertTape("testRemoveFavorite");

        DestinationFacets.instance.removeFavorite(session, 20);
    }

    public void testRemoveNonexistantFavorite() throws Throwable {
        recorder.insertTape("testRemoveNonexistantFavorite");

        try {
            DestinationFacets.instance.removeFavorite(session, 0);
        } catch(UnexpectedResultException ure) {
            assertEquals(HttpStatus.SC_GONE, ure.getStatusCode());
        }
    }

    public void testGetDestinationFacet() throws Throwable {
        recorder.insertTape("testGetDestinationFacet");

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
        recorder.insertTape("testUpdateDestinationFacet", Collections.singletonMap("mode", TapeMode.READ_SEQUENTIAL));

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
        recorder.insertTape("testUpdateNonexistentDestinationFacet");

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
