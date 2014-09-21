package com.riverflows;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class About extends Activity {
	private WebView webview;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		webview = new WebView(this);
		setContentView(webview);
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        webview.addJavascriptInterface(new VersionProvider(), "versionProvider");
        
		webview.loadUrl("file:///android_asset/about.html");
	}
	
	public class VersionProvider {
		/**
		 * Callback to request the version number from within about.html
		 */
		public void initializeVersion() {
	        new Handler().post(new Runnable(){
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
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.standard_menu, menu);
        
        //disable irrelevant menu items
        menu.findItem(R.id.mi_about).setVisible(false);
        menu.findItem(R.id.mi_reload).setVisible(false);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mi_home:
	    	startActivityIfNeeded(new Intent(this, Home.class), -1);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}
