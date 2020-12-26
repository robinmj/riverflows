package com.riverflows.db;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.riverflows.Home;
import com.riverflows.data.CachedDataset;
import com.riverflows.data.WrappedHttpResponse;
import com.riverflows.wsclient.HttpClientWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Random;

public class CachingHttpClientWrapper implements HttpClientWrapper {
	private static final String TAG = Home.TAG;
	
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
	public WrappedHttpResponse doGet(String requestUrl, boolean hardRefresh) throws IOException {

		InputStream responseStream;
		
		File cacheFile = null;

		boolean databaseError = false;
		
		CachedDataset cacheEntry = null;
		try {
			// crashlytics #27
			cacheEntry = DatasetsDaoImpl.getDataset(dbContext, requestUrl);
		} catch (IllegalStateException ise) {
			Crashlytics.getInstance().core.logException(ise);
			databaseError = true;
		}
		
		if(cacheEntry != null) {
			cacheFile = new File(cacheDir, cacheEntry.getCacheFileName());
			
			if(!hardRefresh) {
				//try to build a response from a cache entry

				if(cacheEntry.getTimestamp().after(new Date(System.currentTimeMillis() - lifetimeMs))) {
					if(cacheFile.exists()) {
						Log.i(TAG,"cache hit");
						
						return new WrappedHttpResponse(new FileInputStream(cacheFile), null, 200, null);
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

		URL url = new URL(requestUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		String existingAgent = System.getProperty("http.agent");
		conn.setRequestProperty("User-Agent","riverflows.net/2.0 " + existingAgent);
		// Start the query
		conn.connect();
		responseStream = conn.getInputStream();

		if (databaseError) {
			//don't even try to use cache

			return new WrappedHttpResponse(responseStream, null, conn.getResponseCode(), conn.getResponseMessage());
		}
		
		if(cacheFile == null) {
			cacheFile = new File(cacheDir, "" + filenameGenerator.nextLong());
			
			DatasetsDaoImpl.saveDataset(dbContext, requestUrl, cacheFile.getName());
		} else {
			DatasetsDaoImpl.updateDatasetTimestamp(dbContext, requestUrl, cacheEntry.getCacheFileName());
		}
		
		Log.i(TAG, "caching " + requestUrl + " to " + cacheFile.getAbsolutePath());

		return new WrappedHttpResponse(responseStream, cacheFile, conn.getResponseCode(), conn.getResponseMessage());
	}
}
