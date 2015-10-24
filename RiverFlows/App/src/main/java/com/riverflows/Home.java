package com.riverflows;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.google.analytics.tracking.android.EasyTracker;
import com.riverflows.data.UserAccount;
import com.riverflows.db.DatasetsDaoImpl;
import com.riverflows.db.DbMaintenance;
import com.riverflows.db.RiverGaugesDb;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.CDECDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.USACEDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.WsSession;

import roboguice.activity.RoboActionBarActivity;

public class Home extends RoboActionBarActivity implements ActionBar.TabListener {

	public static final int TAB_FAVORITES = 0;
	public static final int TAB_SITES = 1;

    public static final int REQUEST_CREATE_ACCOUNT = 3247;

    //TODO move this into App
	public static final String TAG = "RiverFlows";

    //TODO move these into App
	public static final String PREFS_FILE = "com.riverflows.prefs";
	public static final String PREF_TEMP_UNIT = "tempUnit";
    public static final String PREF_FACET_TYPES = "currentFacetTypes";
    public static final String PREF_FAVORITE_INTRO = "favoriteIntro";

	public static final String ACTION_FAVORITES_CHANGED = "com.riverflows.FAVORITES_CHANGED";

	public static final int DIALOG_ID_MASTER_LOADING = 3;
	public static final int DIALOG_ID_MASTER_LOADING_ERROR = 4;
	public static final int DIALOG_ID_MIGRATION_ERROR = 7;

	private Favorites favorites = new Favorites();
	private StateSelect states = new StateSelect();

	private Fragment currentFragment = favorites;
	private volatile int currentTabId = TAB_FAVORITES;

    private SignIn signin;
	
	public void onCreate(Bundle savedInstanceState) {

	    super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final ActionBar ab = getSupportActionBar();

        ab.hide();
	    /*
	    SharedPreferences settings = getPreferences(0);
        boolean widgetAdShown = settings.getBoolean("widgetAdShown", false);
        if(!widgetAdShown) {
        	startActivity(new Intent(this,WidgetAd.class));
        	Editor prefsEditor = settings.edit();
        	prefsEditor.putBoolean("widgetAdShown", true);
        	prefsEditor.commit();
        }*/

		addTab(ab, TAB_FAVORITES, "Favorites");
		addTab(ab, TAB_SITES, "Sites");

		ab.setTitle(getResources().getString(R.string.app_name));
        ab.setDisplayShowHomeEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            ab.setIcon(getResources().getDrawableForDensity(R.drawable.launcher, Math.round(getResources().getDisplayMetrics().scaledDensity * DisplayMetrics.DENSITY_LOW)));
        }
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		if(savedInstanceState != null) {

			//switch to the tab that the user was at last
			currentTabId = savedInstanceState.getInt("tabId", TAB_FAVORITES);

			Log.v(TAG, "reopening tab " + currentTabId);
		} else {
			currentTabId = TAB_FAVORITES;
		}

	    try {
	    	DbMaintenance.upgradeIfNecessary(getApplicationContext());
		} catch(SQLiteException sqle) {
			Log.w(TAG, "", sqle);

			showDialog(DIALOG_ID_MIGRATION_ERROR);
			return;
		}

        findViewById(R.id.initial_progress).setVisibility(View.GONE);

        getSupportActionBar().show();

        getSupportActionBar().setSelectedNavigationItem(currentTabId);
	}

	@Override
	protected void onStart() {
		super.onStart();

	    EasyTracker.getInstance().activityStart(this);
	}
	
	static final int REQUEST_CHOOSE_ACCOUNT = 2154;
	static final int REQUEST_HANDLE_RECOVERABLE_AUTH_EXC = 5436;

    public void signIn() {
        signin = new SignIn();
        signin.execute();
    }

    private class SignIn extends ApiCallTask<String, Integer, UserAccount> {

        private SignInDialogFragment signInDialog;

        public SignIn(){
            super(Home.this, Home.REQUEST_CHOOSE_ACCOUNT, Home.REQUEST_HANDLE_RECOVERABLE_AUTH_EXC, true, false);

            signInDialog = new SignInDialogFragment();
            signInDialog.show(Home.this.getSupportFragmentManager(), "signin");
        }

        public SignIn(SignIn oldTask) {
            super(oldTask);
            signInDialog = new SignInDialogFragment();
            signInDialog.show(Home.this.getSupportFragmentManager(), "signin");
        }

        @Override
        protected UserAccount doApiCall(WsSession session, String... params) {
            return session.userAccount;
        }

        @Override
        protected void onComplete() {
            SignInDialogFragment signInDialog = this.signInDialog;

            if(signInDialog != null) {
                signInDialog.dismiss();
                this.signInDialog = null;
            }
        }

        @Override
        protected void onNoUIRequired(UserAccount userAccount) {

            if(exception != null) {
                Log.e(Home.TAG, "", exception);
            }

            if(userAccount == null) {
                return;
            }

            if(userAccount.getFacetTypes() == 0) {
                //set up this user's account
                startActivityForResult(new Intent(Home.this, AccountSettings.class), REQUEST_CREATE_ACCOUNT);
                return;
            }

            try {
                Favorites favoritesFrag = (Favorites) Home.this.currentFragment;
                if(favoritesFrag != null) {
                    favoritesFrag.loadSites(false);
                }
            } catch(ClassCastException cce) {
                //not viewing favorites
            }
        }

        @Override
        protected ApiCallTask<String, Integer, UserAccount> clone() throws CloneNotSupportedException {
            return new SignIn(this);
        }
    }

    public static class SignInDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog signingInDialog = new ProgressDialog(getActivity());
            signingInDialog.setMessage("Signing In...");
            signingInDialog.setIndeterminate(true);
            signingInDialog.setCancelable(true);
            return signingInDialog;
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
        Log.d(App.TAG, "Home.onActivityResult(" + requestCode + "," + resultCode);

        switch(requestCode) {
            case REQUEST_CHOOSE_ACCOUNT:
            case REQUEST_HANDLE_RECOVERABLE_AUTH_EXC:
                signin.authorizeCallback(requestCode, resultCode, data);
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
			alert.setMessage("Sorry- An error occurred while updating your favorites database. You will have to uninstall and reinstall riverflows.net to fix this.");
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
