package com.riverflows;

import com.riverflows.data.UserAccount;
import com.riverflows.wsclient.HttpClientFactory;
import com.riverflows.wsclient.WebModel;
import com.riverflows.wsclient.WsSession;

import junit.framework.TestCase;

import org.apache.http.client.HttpClient;
import org.junit.Rule;

import co.freeside.betamax.Recorder;
import co.freeside.betamax.httpclient.BetamaxHttpClient;

/**
 * Created by robin on 11/23/14.
 */
public class WebModelTestCase extends TestCase {

    @Rule
    public Recorder recorder = new Recorder();

    public WsSession session = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        session = new WsSession("robin.m.j", account, "T9HLJkUvA7JwELEeHjsu", System.currentTimeMillis() + 10 * 60 * 1000);

        WebModel.setHttpClientFactory(new HttpClientFactory() {
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
}
