package com.riverflows.test;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;

import com.riverflows.wsclient.MockUsgsCsvHttpClient;

public abstract class AssetHttpClientWrapper extends MockUsgsCsvHttpClient {
	
	protected AssetManager assetManager;
	
	protected AssetHttpClientWrapper(AssetManager assetManager) {
		this.assetManager = assetManager;
	}
	
	protected InputStream getStream(String fileKey) throws IOException {
		return this.assetManager.open(fileKey);
	}
	
}
