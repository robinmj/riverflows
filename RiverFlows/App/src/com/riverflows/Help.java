package com.riverflows;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;

public class Help extends Activity {
	public static final String EXTRA_TOPIC = "topic";
	
	public static final String DATA_PATH_PREFIX = "help";
	
	public static final String BASE_URI = "riverflows://" + DATA_PATH_PREFIX + "/";
	
	private WebView webview;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    
	    String topicPath = "/index.html";
	    
		if(getIntent().getData() == null) {
			String extraTopicPath = getIntent().getStringExtra(EXTRA_TOPIC);
		    if(extraTopicPath != null) {
		    	topicPath = extraTopicPath;
		    }
		} else if(Intent.ACTION_VIEW.equals(getIntent().getAction())){
		    String dataTopicPath = getIntent().getData().getPath();
		    if(dataTopicPath.trim().length() > 0) {
		    	topicPath = dataTopicPath;
		    }
		}
		
		webview = new WebView(this);
		webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setSupportZoom(true);
		setContentView(webview);
		webview.loadUrl("file:///android_asset/" + DATA_PATH_PREFIX + topicPath);
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
