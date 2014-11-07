package com.riverflows.wsclient;

import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.UserAccount;

import junit.framework.TestCase;

/**
 * Created by robin on 11/4/14.
 */
public class DestinationsTest extends TestCase {
    public void testUpdateDestination() throws Throwable {
        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        DestinationFacet testFacet = DestinationFacets.instance.get(session, 20);
        Destination dest = testFacet.getDestination();

        assertEquals("lower t box", dest.getName());

        dest.setName("Lower Taos Box");

        Destinations.instance.update(session, dest);

        testFacet = DestinationFacets.instance.get(session, 20);
        dest = testFacet.getDestination();

        assertEquals("Lower Taos Box", dest.getName());

        //reverse the change
        dest.setName("lower t box");
        Destinations.instance.update(session, dest);
    }
}
