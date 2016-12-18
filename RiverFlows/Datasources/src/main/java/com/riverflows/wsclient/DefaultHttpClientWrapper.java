package com.riverflows.wsclient;

import com.riverflows.data.WrappedHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DefaultHttpClientWrapper implements HttpClientWrapper {

	@Override
	public WrappedHttpResponse doGet(String requestUrl, boolean hardRefresh) throws IOException {
		URL url = new URL(requestUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(15000);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		conn.connect();
		InputStream responseStream = conn.getInputStream();

		return new WrappedHttpResponse(responseStream, null, conn.getResponseCode(), conn.getResponseMessage());
	}

}
