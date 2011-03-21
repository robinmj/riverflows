package com.riverflows.wsclient;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

public interface HttpClientWrapper {
	public HttpResponse doGet(HttpGet getCmd) throws ClientProtocolException, IOException;
}
