package com.riverflows;

import com.google.inject.AbstractModule;
import com.riverflows.data.UserAccount;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

/**
 * Created by robin on 3/12/15.
 */
public class RobinSession extends AbstractModule {

    public WsSessionManager wsSessionManager = new WsSessionManager();

    public RobinSession() {

        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        WsSession session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        this.wsSessionManager.setSession(session);
    }

    @Override
    protected void configure() {
        bind(WsSessionManager.class).toInstance(wsSessionManager);
    }
}
