package com.riverflows.wsclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

public class MockAHPSHttpClient implements HttpClientWrapper {
	
	private static final Log LOG = LogFactory.getLog(MockAHPSHttpClient.class);
	
	public static File sourceDir = new File("testdata/ahps/");
	
	@Override
	public HttpResponse doGet(HttpGet getCmd, boolean hardRefresh) throws ClientProtocolException,
			IOException {
		String requestUrl = getCmd.getURI().toString();
		
		if(!requestUrl.startsWith(AHPSXmlDataSource.SITE_DATA_URL)) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + requestUrl);
		}
		
		requestUrl = requestUrl.substring(AHPSXmlDataSource.SITE_DATA_URL.length());
		
		File responseFile = new File(sourceDir, requestUrl);
		
		if(!responseFile.exists()) {
			LOG.error("no such file: " + responseFile);
			HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
		    		  new ProtocolVersion("HTTP", 1, 1), 404, ""));
			response.setStatusCode(404);
			response.setEntity(new StringEntity("<html><head><title>No Such File</title></head><body>No Such File</body></html>"));
			
			return response;
		}
		
		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
	    		  new ProtocolVersion("HTTP", 1, 1), 200, ""));
		response.setStatusCode(200);
		
		response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));
		
		return response;
	}
}
