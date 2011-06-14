package com.riverflows.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import android.content.Context;
import android.util.Log;

import com.riverflows.data.CachedDataset;
import com.riverflows.wsclient.HttpClientWrapper;

public class CachingHttpClientWrapper implements HttpClientWrapper {
	private static final String TAG = CachingHttpClientWrapper.class.getSimpleName();
	
	private static final Random filenameGenerator = new Random(System.currentTimeMillis());
	
	private File cacheDir;
	private long lifetimeMs;
	private Context dbContext;
	private String contentType;
	
	public CachingHttpClientWrapper(Context dbContext, File cacheDir, long lifetimeMs, String contentType) {
		this.cacheDir = cacheDir;
		this.lifetimeMs = lifetimeMs;
		this.dbContext = dbContext;
		this.contentType = contentType;
	}
	
	@Override
	public HttpResponse doGet(HttpGet getCmd, boolean hardRefresh) throws ClientProtocolException,
			IOException {

		HttpResponse response;
		
		File cacheFile = null;
		
		String requestUrl = getCmd.getURI().toString();
		
		CachedDataset cacheEntry = DatasetsDaoImpl.getDataset(dbContext, requestUrl);
		
		if(cacheEntry != null) {
			cacheFile = new File(cacheDir, cacheEntry.getCacheFileName());
			
			if(!hardRefresh) {
				//try to build a response from a cache entry

				if(cacheEntry.getTimestamp().after(new Date(System.currentTimeMillis() - lifetimeMs))) {
					if(cacheFile.exists()) {
						Log.i(TAG,"cache hit");
						
						response = new BasicHttpResponse(new BasicStatusLine(
					    		  new ProtocolVersion("HTTP", 1, 1), 200, ""));
						response.setStatusCode(200);
						response.setEntity(new InputStreamEntity(new FileInputStream(cacheFile), cacheFile.length()));
						
						if(contentType != null) {
							response.setHeader("Content-Type", contentType);
						}
						
						return response;
					} else {
						Log.e(TAG, "could not find cache file: " + cacheFile);
					}
				} else {
					Log.d(TAG, "expired cache entry");
				}
			}
		} else {
			Log.d(TAG, "cache miss");
		}

		//no cached response found- make a new request
		HttpClient client = new DefaultHttpClient();
		response = client.execute(getCmd);

		String url = getCmd.getURI().toString();
		
		if(cacheFile == null) {
			cacheFile = new File(cacheDir, "" + filenameGenerator.nextLong());
			
			DatasetsDaoImpl.saveDataset(dbContext, url, cacheFile.getName());
			
			response.addHeader(PN_CACHE_FILE, cacheFile.getAbsolutePath());
		} else {
			DatasetsDaoImpl.updateDatasetTimestamp(dbContext, url);
		}
		
		Log.i(TAG, "caching " + url + " to " + cacheFile.getAbsolutePath());
		
		return response;
	}

}
