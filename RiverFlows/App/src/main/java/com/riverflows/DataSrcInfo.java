package com.riverflows;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class DataSrcInfo extends Activity {
	
	public static final String KEY_INFO = "info";
	
	private WebView webview;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle("Data Source Info");

		Bundle extras = getIntent().getExtras();
		
        String info = (String)extras.get(KEY_INFO);
		
		webview = new WebView(this);
		setContentView(webview);
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        //use loadDataWithBaseURL as workaround for http://code.google.com/p/android/issues/detail?id=1733
		webview.loadDataWithBaseURL(null, info, "text/html", "utf-8", null);
	}
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.standard_menu, menu);
        
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case android.R.id.home:
	    	startActivityIfNeeded(new Intent(this, Home.class), -1);
	    	return true;
	    case R.id.mi_about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
	        return true;
	    case R.id.mi_reload:
	    	webview.reload();
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}
