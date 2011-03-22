package com.riverflows.test;

import android.content.res.AssetManager;

import com.riverflows.wsclient.AHPSXmlDataSource;

public class AssetAhpsXmlHttpClient extends AssetHttpClientWrapper {
	
	public AssetAhpsXmlHttpClient(AssetManager assetManager) {
		super(assetManager, "ahps/");
	}
	
	@Override
	protected String getFileNameFromUrl(String url) {
		
		if(!url.startsWith(AHPSXmlDataSource.SITE_DATA_URL)) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + url);
		}
		
		return url.substring(AHPSXmlDataSource.SITE_DATA_URL.length());
	}

}
