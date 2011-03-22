package com.riverflows.test;

import java.io.IOException;
import java.io.InputStream;

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

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import com.riverflows.wsclient.MockUsgsCsvHttpClient;

/**
 * Mock HTTP client that instead returns static files from the application assets
 * @author robin
 *
 */
public abstract class AssetHttpClientWrapper extends MockUsgsCsvHttpClient {

	private static final Log LOG = LogFactory.getLog(AssetHttpClientWrapper.class);
	
	protected AssetManager assetManager;
	protected String sourceDirectory;
	
	protected AssetHttpClientWrapper(AssetManager assetManager, String sourceDirectory) {
		this.assetManager = assetManager;
		this.sourceDirectory = sourceDirectory;
	}
	
	protected InputStream getStream(String fileKey) throws IOException {
		return this.assetManager.open(fileKey);
	}
	
	@Override
	public HttpResponse doGet(HttpGet getCmd) throws ClientProtocolException,
			IOException {
		String fileName = getFileNameFromUrl(getCmd.getURI().toString());
		
		try {
			HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
		    		  new ProtocolVersion("HTTP", 1, 1), 200, ""));
			response.setStatusCode(200);
			
			if(LOG.isInfoEnabled()) {
				String[] files = assetManager.list(sourceDirectory.substring(0, sourceDirectory.length() - 1));
				for(String file:files) {
					LOG.info(file);
				}
			}
			
			AssetFileDescriptor assetFile = assetManager.openFd(sourceDirectory + fileName + ".mp3");
			
			response.setEntity(new InputStreamEntity(assetFile.createInputStream(), assetFile.getLength()));
			
			return response;
		} catch(IOException ioe) {
			LOG.error("problem accessing file: " + sourceDirectory + fileName, ioe);
			HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
		    		  new ProtocolVersion("HTTP", 1, 1), 404, ""));
			response.setStatusCode(404);
			response.setEntity(new StringEntity("<html><head><title>No Such File</title></head><body>No Such File</body></html>"));
			
			return response;
			
		}
	}
	
	protected abstract String getFileNameFromUrl(String url);
}
