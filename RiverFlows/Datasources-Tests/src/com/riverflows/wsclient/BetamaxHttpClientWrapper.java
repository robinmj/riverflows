package com.riverflows.wsclient;

import com.riverflows.data.WrappedHttpResponse;

import org.apache.http.HttpResponse;
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
    public WrappedHttpResponse doGet(String requestUrl, boolean hardRefresh) throws IOException {
        HttpGet getCmd = new HttpGet(requestUrl);
        HttpResponse response = this.client.execute(getCmd);

        return new WrappedHttpResponse(response.getEntity().getContent(),
                null,
                response.getStatusLine().getStatusCode(),
                response.getStatusLine().getReasonPhrase());
    }
}
