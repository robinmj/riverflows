package com.riverflows.wsclient;

import com.riverflows.data.Favorite;
import com.riverflows.data.UserAccount;

import junit.framework.TestCase;

import org.apache.http.HttpStatus;

/**
 * Created by robin on 10/14/14.
 */
public class DestinationFacetsTest extends TestCase {

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
}
