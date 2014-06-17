package com.riverflows;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
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
import com.riverflows.wsclient.WsSession;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Home extends SherlockFragmentActivity implements ActionBar.TabListener {

	public static final int TAB_FAVORITES = 0;
	public static final int TAB_SITES = 1;
	
	public static final String TAG = "RiverFlows";

	public static final String PREFS_FILE = "com.riverflows.prefs";
	public static final String PREF_TEMP_UNIT = "tempUnit";

	public static final int DIALOG_ID_LOADING = 1;
	public static final int DIALOG_ID_LOADING_ERROR = 2;
	public static final int DIALOG_ID_MASTER_LOADING = 3;
	public static final int DIALOG_ID_MASTER_LOADING_ERROR = 4;
	public static final int DIALOG_ID_UPGRADE_FAVORITES = 5;
	public static final int DIALOG_ID_SIGNING_IN = 6;
	public static final int DIALOG_ID_MIGRATION_ERROR = 7;

	private Favorites favorites = new Favorites();
	private StateSelect states = new StateSelect();

	private Fragment currentFragment = favorites;
	private volatile int currentTabId = TAB_FAVORITES;

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

		setContentView(R.layout.main);

		final ActionBar ab = getSupportActionBar();

		ab.hide();

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

		addTab(ab, TAB_FAVORITES, "Favorites");
		addTab(ab, TAB_SITES, "Sites");

		ArrayAdapter<?> navigationAdapter = ArrayAdapter
				.createFromResource(ab.getThemedContext(), R.array.sections,
						com.actionbarsherlock.R.layout.sherlock_spinner_item);
		navigationAdapter.setDropDownViewResource(com.actionbarsherlock.R.layout.sherlock_spinner_dropdown_item);

		ab.setListNavigationCallbacks(navigationAdapter,
				new ActionBar.OnNavigationListener() {
					public boolean onNavigationItemSelected(int itemPosition,
															long itemId) {

						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						tabSelected(itemPosition, ft);
						ft.commit();
						return true;
					}
				});

		ab.setDisplayShowTitleEnabled(false);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

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

	private class ErrorMsgDialog extends AlertDialog {

		public ErrorMsgDialog(Context context, String msg) {
			super(context);
			setMessage(msg);
		}

	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
			case Home.DIALOG_ID_LOADING:
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage("Loading Sites...");
				dialog.setIndeterminate(true);
				dialog.setCancelable(true);
				return dialog;
			case Home.DIALOG_ID_LOADING_ERROR:
				ErrorMsgDialog errorDialog = new ErrorMsgDialog(this, favorites.loadTask.getErrorMsg());
				return errorDialog;
			case Home.DIALOG_ID_MASTER_LOADING:
				ProgressDialog masterDialog = new ProgressDialog(this);
				masterDialog.setMessage("Downloading Master Site List...");
				masterDialog.setIndeterminate(true);
				masterDialog.setCancelable(true);
				return masterDialog;
			case Home.DIALOG_ID_MASTER_LOADING_ERROR:
				ErrorMsgDialog masterErrorDialog = new ErrorMsgDialog(this, favorites.loadTask.getErrorMsg());
				return masterErrorDialog;
			case Home.DIALOG_ID_UPGRADE_FAVORITES:
				ProgressDialog favoritesDialog = new ProgressDialog(this);
				favoritesDialog.setMessage("Upgrading Favorites\nthis may take a few minutes");
				favoritesDialog.setIndeterminate(true);
				favoritesDialog.setCancelable(true);
				return favoritesDialog;
			case Home.DIALOG_ID_SIGNING_IN:
				ProgressDialog signingInDialog = new ProgressDialog(this);
				signingInDialog.setMessage("Signing In...");
				signingInDialog.setIndeterminate(true);
				signingInDialog.setCancelable(true);
				return signingInDialog;
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
