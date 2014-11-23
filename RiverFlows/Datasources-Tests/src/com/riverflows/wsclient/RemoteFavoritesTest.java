package com.riverflows.wsclient;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.UserAccount;

import junit.framework.TestCase;

import java.util.List;

/**
 * Created by robin on 10/14/14.
 */
public class RemoteFavoritesTest extends TestCase {

    private DestinationFacets destinationFacets = new DestinationFacets();

    public void testReorderFavorites() throws Throwable {
        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        try {
            RemoteFavorites.instance.reorderFavorites(session, new int[]{});
        } catch(Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        int[] newOrder = new int[]{14, 20, 9};
        List<Favorite> favorites = RemoteFavorites.instance.reorderFavorites(session, newOrder);

        for(int a = 0; a < newOrder.length; a++) {
            assertEquals(newOrder[a], favorites.get(a).getDestinationFacet().getId().intValue());
        }

        List<DestinationFacet> facets = destinationFacets.getFavorites(session);

        for(int a = 0; a < newOrder.length; a++) {
            assertEquals(newOrder[a], facets.get(a).getId().intValue());
        }
    }
}
