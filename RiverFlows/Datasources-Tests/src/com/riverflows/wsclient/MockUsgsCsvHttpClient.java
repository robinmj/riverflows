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

public class MockUsgsCsvHttpClient implements HttpClientWrapper {
	
	private static final Log LOG = LogFactory.getLog(MockUsgsCsvHttpClient.class);
	
	public static File sourceDir = new File("testdata/usgs/csv/");
	
	@Override
	public HttpResponse doGet(HttpGet getCmd, boolean hardRefresh) throws ClientProtocolException,
			IOException {
		String requestUrl = getCmd.getURI().toString();
		
		if(!requestUrl.startsWith(UsgsCsvDataSource.SITE_DATA_URL)) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + requestUrl);
		}
		
		requestUrl = requestUrl.substring(UsgsCsvDataSource.SITE_DATA_URL.length());
		
		/*
		Pattern queryParamsPattern = Pattern.compile("sites=(.*)&parameterCd=(.*)(&?.*)");
		Matcher m = queryParamsPattern.matcher(requestUrl);
		if(!m.matches()) {
			throw new IllegalArgumentException("bad query string parameters: " + requestUrl);
		}
		
		String[] siteIds = m.group(1).split(",");
		String[] parameterIds = m.group(2).split(",");
		boolean singleReading = true;
		if(m.groupCount() > 3) {
			String period = m.group(3);
			assert period.equals("&period=P7D");
			singleReading = false;
		}*/
		
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
