package com.riverflows;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.riverflows.data.UserAccount;
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
import com.riverflows.wsclient.WsSession;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Home extends ActionBarActivity implements ActionBar.TabListener {

	public static final int TAB_FAVORITES = 0;
	public static final int TAB_SITES = 1;

    //TODO move this into App
	public static final String TAG = "RiverFlows";

    //TODO move these into App
	public static final String PREFS_FILE = "com.riverflows.prefs";
	public static final String PREF_TEMP_UNIT = "tempUnit";
    public static final String PREF_FACET_TYPES = "currentFacetTypes";

	public static final String ACTION_FAVORITES_CHANGED = "com.riverflows.FAVORITES_CHANGED";

	public static final int DIALOG_ID_MASTER_LOADING = 3;
	public static final int DIALOG_ID_MASTER_LOADING_ERROR = 4;
	public static final int DIALOG_ID_MIGRATION_ERROR = 7;

	private Favorites favorites = new Favorites();
	private StateSelect states = new StateSelect();

	private Fragment currentFragment = favorites;
	private volatile int currentTabId = TAB_FAVORITES;
	
	private InitSession initSession = new InitSession(this, REQUEST_CHOOSE_ACCOUNT, REQUEST_HANDLE_RECOVERABLE_AUTH_EXC, false, false);
	
	public void onCreate(Bundle savedInstanceState) {

	    super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final ActionBar ab = getSupportActionBar();

        ab.hide();
	    
	    SharedPreferences settings = getPreferences(0);
        boolean widgetAdShown = settings.getBoolean("widgetAdShown", false);
        if(!widgetAdShown) {
        	startActivity(new Intent(this,WidgetAd.class));
        	Editor prefsEditor = settings.edit();
        	prefsEditor.putBoolean("widgetAdShown", true);
        	prefsEditor.commit();
        }

		addTab(ab, TAB_FAVORITES, "Favorites");
		addTab(ab, TAB_SITES, "Sites");

		ab.setDisplayShowTitleEnabled(false);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		if(savedInstanceState != null) {

			//switch to the tab that the user was at last
			currentTabId = savedInstanceState.getInt("tabId", TAB_FAVORITES);

			Log.v(TAG, "reopening tab " + currentTabId);
		} else {
			if(FavoritesDaoImpl.hasFavorites(getApplicationContext())) {
				currentTabId = TAB_FAVORITES;
			} else {
				currentTabId = TAB_SITES;
			}
		}

	    try {
	    	DbMaintenance.upgradeIfNecessary(getApplicationContext());
		} catch(SQLiteException sqle) {
			Log.w(TAG, "", sqle);

			showDialog(DIALOG_ID_MIGRATION_ERROR);
			return;
		}

		//attempt to set up session for accessing web services, but don't display a login screen
		initSession.execute();
	}

	@Override
	protected void onStart() {
		super.onStart();

	    EasyTracker.getInstance().activityStart(this);
	}
	
	static final int REQUEST_CHOOSE_ACCOUNT = 2154;
	static final int REQUEST_HANDLE_RECOVERABLE_AUTH_EXC = 5436;

	private class InitSession extends ApiCallTask<String, Integer, String> {
		
		public InitSession(Activity activity, int requestCode, int recoveryRequestCode, boolean loginRequired, boolean secondTry) {
			super(activity, requestCode, recoveryRequestCode, loginRequired, secondTry);
		}
		
		public InitSession(InitSession oldTask) {
			super(oldTask);
		}

		@Override
		protected String doApiCall(WsSession session, String... params) {
			return null;
		}
		
		@Override
		protected void onNoUIRequired(String result) {

			findViewById(R.id.initial_progress).setVisibility(View.GONE);

			getSupportActionBar().show();

			getSupportActionBar().setSelectedNavigationItem(currentTabId);
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
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_CHOOSE_ACCOUNT:
            case REQUEST_HANDLE_RECOVERABLE_AUTH_EXC:
                initSession.authorizeCallback(requestCode, resultCode, data);
                return;
        }
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

	private class ErrorMsgDialog extends AlertDialog {

		public ErrorMsgDialog(Context context, String msg) {
			super(context);
			setMessage(msg);
		}

	}
	
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

	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
		tabSelected(((Integer)tab.getTag()).intValue(), ft);
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
		//((RefreshableFragment)this.currentFragment).refresh(false);
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
	}

	private void tabSelected(int id, FragmentTransaction ft) {
		currentTabId = id;

		Fragment nextFrag = null;
		switch (id) {
			case TAB_FAVORITES:
				nextFrag = this.favorites;
				break;
			case TAB_SITES:
				nextFrag = this.states;
				break;
		}

		Log.d(TAG, "tabSelected " + id);

		this.currentFragment = nextFrag;
		ft.replace(R.id.root, nextFrag);
	}

	private ActionBar.Tab addTab(ActionBar ab, int key, String title) {

		ActionBar.Tab t = ab.newTab();
		t.setTag(key);
		t.setText(title);
		t.setTabListener(this);
		ab.addTab(t);
		return t;
	}
}
