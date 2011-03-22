package com.riverflows.test;

import android.content.res.AssetManager;

import com.riverflows.wsclient.UsgsCsvDataSource;

public class AssetUsgsCsvHttpClient extends AssetHttpClientWrapper {
	
	public AssetUsgsCsvHttpClient(AssetManager assetManager) {
		super(assetManager, "usgs/csv/");
	}
	
	@Override
	protected String getFileNameFromUrl(String url) {
		
		if(!url.startsWith(UsgsCsvDataSource.SITE_DATA_URL)) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + url);
		}
		
		return url.substring(UsgsCsvDataSource.SITE_DATA_URL.length());
	}
}
