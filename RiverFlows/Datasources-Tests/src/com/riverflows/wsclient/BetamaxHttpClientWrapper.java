package com.riverflows.wsclient;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

import co.freeside.betamax.Recorder;
import co.freeside.betamax.httpclient.BetamaxHttpClient;

/**
 * Created by robin on 3/30/15.
 */
public class BetamaxHttpClientWrapper implements HttpClientWrapper {

    private BetamaxHttpClient client;

    public BetamaxHttpClientWrapper(Recorder recorder) {
        this.client = new BetamaxHttpClient(recorder);
    }

    @Override
    public HttpResponse doGet(HttpGet getCmd, boolean hardRefresh) throws IOException {
        return this.client.execute(getCmd);
    }
}
