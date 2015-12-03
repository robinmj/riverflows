package com.riverflows.wsclient;

import com.riverflows.data.UserAccount;

import junit.framework.TestCase;

/**
 * Created by robin on 10/30/14.
 */
public class UserAccountsTest extends TestCase {
    UserAccount account = new UserAccount();
    WsSession session = null;

    @Override
    protected void setUp() throws Exception {
        this.account.setEmail("robin.m.j@gmail.com");
        this.session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);
    }

    public void getUserAccount() throws Exception {

        UserAccount account = UserAccounts.instance.get(this.session);

        assertEquals(2, account.getId().intValue());
        assertEquals("Robin", account.getFirstName());
        assertEquals("Johnson", account.getLastName());
        assertEquals("robin.m.j@gmail.com",account.getEmail());
        assertEquals("robin.m.j", account.getNickname());
        assertEquals(18, account.getFacetTypes());
        assertNull(account.getLatitude());
        assertNull(account.getLongitude());
    }
}
