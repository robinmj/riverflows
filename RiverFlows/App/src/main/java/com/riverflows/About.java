package com.riverflows;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class About extends ActionBarActivity {
	private WebView webview;

    private Handler handler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		webview = new WebView(this);
		setContentView(webview);
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);

        this.handler = new Handler();

        webview.addJavascriptInterface(new VersionProvider(), "versionProvider");
        
		webview.loadUrl("file:///android_asset/about.html");
	}

	public class VersionProvider {
        @JavascriptInterface
		/**
		 * Callback to request the version number from within about.html
		 */
		public void initializeVersion() {
	        About.this.handler.post(new Runnable(){
	        	public void run() {
	        		try {
	        			PackageInfo info = About.this.getPackageManager().getPackageInfo(getPackageName(), 0);
	        			About.this.webview.loadUrl("javascript:setVersionNum('" + info.versionName + "')");
	        		} catch (NameNotFoundException e) {
	        			Log.e(getClass().getSimpleName(), "huh?",e);
	        		}
	        	}
	        });
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case android.R.id.home:
	    	finish();
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}
