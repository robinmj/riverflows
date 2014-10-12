package com.riverflows.wsclient;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

public interface HttpClientWrapper {
	
	public static final String PN_CACHE_FILE = "cacheFile";
	
	public HttpResponse doGet(HttpGet getCmd, boolean hardRefresh) throws ClientProtocolException, IOException;
}
