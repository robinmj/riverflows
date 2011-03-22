package com.riverflows.test;

import android.content.res.AssetManager;

import com.riverflows.wsclient.CODWRDataSource;

public class AssetCoDwrCsvHttpClient extends AssetHttpClientWrapper {
	
	/**
	 * tail end of the URL that needs to be chopped off
	 */
	private static final String SUFFIX = "&START=00/00/00&END=00/00/00";
	
	public AssetCoDwrCsvHttpClient(AssetManager assetManager) {
		super(assetManager, "codwr/");
	}
	
	@Override
	protected String getFileNameFromUrl(String url) {
		if(!url.startsWith(CODWRDataSource.SITE_DATA_URL)) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + url);
		}
		
		return url.substring(CODWRDataSource.SITE_DATA_URL.length(), url.length() - SUFFIX.length());
	}
}
