package com.riverflows;

import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TabHost;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.riverflows.db.CachingHttpClientWrapper;
import com.riverflows.db.DatasetsDaoImpl;
import com.riverflows.db.DbMaintenance;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.RiverGaugesDb;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.CDECDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.USACEDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.WsSessionManager.Session;

public class Home extends TabActivity {
	
	public static final String TAG = "RiverFlows";

	public static final String PREFS_FILE = "com.riverflows.prefs";
	public static final String PREF_TEMP_UNIT = "tempUnit";

	static {
		// Work around pre-Froyo bugs in HTTP connection reuse.
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	/**
	 * 20 minutes
	 */
	public static final long CACHE_TTL = 20 * 60 * 1000;
	
	private InitSession initSession = new InitSession(this, REQUEST_CHOOSE_ACCOUNT, REQUEST_HANDLE_RECOVERABLE_AUTH_EXC, false, false);
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    SharedPreferences settings = getPreferences(0);
        boolean widgetAdShown = settings.getBoolean("widgetAdShown", false);
        if(!widgetAdShown) {
        	startActivity(new Intent(this,WidgetAd.class));
        	Editor prefsEditor = settings.edit();
        	prefsEditor.putBoolean("widgetAdShown", true);
        	prefsEditor.commit();
        }

		DataSourceController.setHttpClientWrapper(new CachingHttpClientWrapper(
				getApplicationContext(), getCacheDir(), CACHE_TTL, "text/plain"));
		DataSourceController.getDataSource("AHPS").setHttpClientWrapper(new CachingHttpClientWrapper(
				getApplicationContext(), getCacheDir(), CACHE_TTL, "text/xml"));
		DataSourceController.initCache(getCacheDir());
	    
	    Logger.getLogger("").setLevel(Level.WARNING);

		//disable Google Analytics when in debug mode
		GoogleAnalytics myInstance = GoogleAnalytics.getInstance(this);
		myInstance.setAppOptOut((getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != ApplicationInfo.FLAG_DEBUGGABLE);
	    
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.main);

	    Resources res = getResources();
	    TabHost tabHost = getTabHost();
	    TabHost.TabSpec spec;
	    Intent intent;

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, StateSelect.class);
	    spec = tabHost.newTabSpec("browse").setIndicator("Browse by State",
	                      res.getDrawable(R.drawable.ic_tab_browse))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, Favorites.class);
	    spec = tabHost.newTabSpec("favorites").setIndicator("Favorites",
	                      res.getDrawable(R.drawable.ic_tab_favorites))
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    try {
	    	DbMaintenance.upgradeIfNecessary(getApplicationContext());
		} catch(SQLiteException sqle) {
			showDialog(DIALOG_ID_MIGRATION_ERROR);
			return;
		}
	    
	    if(FavoritesDaoImpl.hasFavorites(getCurrentActivity())) {
	    	tabHost.setCurrentTab(1);
	    } else {
	    	tabHost.setCurrentTab(0);
	    }

		//attempt to set up session for accessing web services, but don't display a login screen
		initSession.execute();
	}

	@Override
	protected void onStart() {
		super.onStart();

	    EasyTracker.getInstance().activityStart(this);
	}
	
	static final int REQUEST_CHOOSE_ACCOUNT = 281546;
	static final int REQUEST_HANDLE_RECOVERABLE_AUTH_EXC = 95436;

	private class InitSession extends ApiCallTask<String, Integer, String> {
		
		public InitSession(Activity activity, int requestCode, int recoveryRequestCode, boolean loginRequired, boolean secondTry) {
			super(activity, requestCode, recoveryRequestCode, loginRequired, secondTry);
		}
		
		public InitSession(InitSession oldTask) {
			super(oldTask);
		}

		@Override
		protected String doApiCall(Session session, String... params) {
			return null;
		}
		
		@Override
		protected void onNoUIRequired(String result) {
		}
		
		@Override
		protected ApiCallTask<String, Integer, String> clone()
				throws CloneNotSupportedException {
			return new InitSession(this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

	    EasyTracker.getInstance().activityStop(this);
	}
	
	protected void onActivityResult(final int requestCode, final int resultCode,
	         final Intent data) {
	     initSession.authorizeCallback(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	    
		//do in background to avoid hanging the main thread if deleteExpiredDatasets() takes some time.
		new Thread() {
			@Override
			public void run() {
			    DatasetsDaoImpl.deleteExpiredDatasets(getApplicationContext(), getCacheDir());
			    
			    RiverGaugesDb.closeHelper();
			}
		}.start();
	}
	
	public static Integer getAgencyIconResId(String siteAgency) {
		if(UsgsCsvDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.usgs;
        } else if(AHPSXmlDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.ahps;
        } else if(CODWRDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.codwr;
        } else if(CDECDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.cdec;
        } else if(USACEDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.usace;
        }
		return null;
	}
	
	public static Intent getWidgetUpdateIntent() {
		//refers to com.riverflows.widget.Provider.ACTION_UPDATE_WIDGET
		Intent i = new Intent("com.riverflows.widget.UPDATE");
		i.setClassName("com.riverflows.widget", "com.riverflows.widget.Provider");
		return i;
	}
	
	public static final int DIALOG_ID_MIGRATION_ERROR = 1;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_ID_MIGRATION_ERROR:
			AlertDialog alert = new AlertDialog.Builder(this).create();
			alert.setMessage("Sorry- An error occurred while updating your favorites database. You will have to uninstall and reinstall RiverFlows to fix this.");
			return alert;
		}
		return null;
	}
}
