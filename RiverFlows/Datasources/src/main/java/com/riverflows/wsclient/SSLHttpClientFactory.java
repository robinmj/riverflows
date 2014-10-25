package com.riverflows.wsclient;

import org.apache.http.client.HttpClient;

/**
 * Created by robin on 10/18/14.
 */
public class SSLHttpClientFactory implements HttpClientFactory {
    @Override
    public HttpClient getHttpClient() {
        return new DataSourceController.SSLHttpClient();
    }
}
