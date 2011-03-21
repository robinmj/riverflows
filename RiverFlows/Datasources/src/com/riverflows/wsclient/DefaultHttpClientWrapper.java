package com.riverflows.wsclient;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class DefaultHttpClientWrapper implements HttpClientWrapper {

	@Override
	public HttpResponse doGet(HttpGet getCmd) throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(getCmd);
		return response;
	}

}
