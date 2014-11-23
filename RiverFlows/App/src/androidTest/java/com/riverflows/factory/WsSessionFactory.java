package com.riverflows.factory;

import com.riverflows.wsclient.WsSession;

/**
 * Created by robin on 11/18/14.
 */
public class WsSessionFactory {

    private static WsSession robinSession;

    public static WsSession getRobinSession() {
        if(robinSession != null) {
            return robinSession;
        }

        robinSession = new WsSession("robin.m.j", UserAccountFactory.getRobin(), "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        return robinSession;
    }
}
