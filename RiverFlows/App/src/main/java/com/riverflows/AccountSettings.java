package com.riverflows;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.inject.Inject;
import com.riverflows.data.UserAccount;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.UserAccounts;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import roboguice.activity.RoboActionBarActivity;

/**
 * Created by robin on 6/4/13.
 */
public class AccountSettings extends RoboActionBarActivity {

	private static final int REQUEST_SAVE = 642048;
	private static final int REQUEST_LOGIN_FOR_SAVE = 32078;

    @Inject
    private WsSessionManager wsSessionManager;

	private String[] facetNames;
	private int[] facetValues;
	private CheckBox[] facetCheckBoxes;

	private SaveTask saveTask;

	private UserAccount currentUser;

	private ActionMode mMode;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currentUser = new UserAccount(this.wsSessionManager.getSession(this).userAccount);

		setContentView(R.layout.account_settings);

		facetNames = getResources().getStringArray(R.array.facet_type_names);
		facetValues = getResources().getIntArray(R.array.facet_type_values);
		facetCheckBoxes = new CheckBox[facetValues.length];

		LinearLayout mainLayout = (LinearLayout)findViewById(R.id.main);

		TextView usernameField = (TextView)findViewById(R.id.username);
		usernameField.setText(currentUser.getEmail());

		if(savedInstanceState != null) {
			int savedTypes = savedInstanceState.getInt("facet_types", -1);

			//restore checkbox state
			if(savedTypes != -1) {
				currentUser.setFacetTypes(savedTypes);
			}
		}

		int marginLeftPx = getPx(10);
		int marginTopPx = getPx(2);

		for(int a = 0; a < facetNames.length; a++) {
			facetCheckBoxes[a] = new CheckBox(this);
			facetCheckBoxes[a].setText(facetNames[a]);
			facetCheckBoxes[a].setTag(facetValues[a]);

			if((currentUser.getFacetTypes() & facetValues[a]) > 0 ) {
				facetCheckBoxes[a].setChecked(true);
			}

			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

			layoutParams.setMargins(marginLeftPx, marginTopPx, marginLeftPx, marginTopPx);
			facetCheckBoxes[a].setLayoutParams(layoutParams);

			mainLayout.addView(facetCheckBoxes[a]);
		}

		mMode = startSupportActionMode(new EditActionMode());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//save checkbox state
		outState.putInt("facet_types", updateUserAccount().getFacetTypes());
	}

	private int getPx(int dp) {
		Resources r = getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	private UserAccount updateUserAccount() {

		int facetTypes = 0;
		for(int a = 0; a < facetCheckBoxes.length; a++) {

			if(facetCheckBoxes[a].isChecked()) {
				facetTypes += (Integer)facetCheckBoxes[a].getTag();
			}
		}

		currentUser.setFacetTypes(facetTypes);

		return currentUser;
	}

	public void save() {
		setSupportProgressBarIndeterminate(true);

		saveTask = new SaveTask(false);
		saveTask.execute(updateUserAccount());

	}

	public void cancel() {
		finish();
	}

	private class SaveTask extends ApiCallTask<UserAccount,Integer,String> {

		public SaveTask(boolean secondTry) {
			super(AccountSettings.this, REQUEST_SAVE, REQUEST_LOGIN_FOR_SAVE, true, secondTry);
		}

		@Override
		protected String doApiCall(WsSession session, UserAccount... params) throws Exception {

			UserAccounts.updateUserAccount(session, params[0]);

			WsSession newSession = new WsSession(session.accountName, params[0], session.authToken, session.accessTokenExpires);

			this.wsSessionManager.notifyAccountSessionChange(newSession, null);

			return null;
		}

		@Override
		protected void onNoUIRequired(String s) {
			setSupportProgressBarIndeterminate(false);

			if(exception != null) {
				Toast.makeText(AccountSettings.this, exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				return;
			}

			Toast.makeText(AccountSettings.this, "Account Saved", Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	private final class EditActionMode implements ActionMode.Callback {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            getMenuInflater().inflate(R.menu.edit_account_action_mode, menu);

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Log.v(Home.TAG, "clicked " + item.getItemId());
			switch(item.getItemId()) {
				case R.id.ai_cancel:
					cancel();
					break;
//				case R.id.ai_save:
//					save();
//					break;
			}
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			Log.v(Home.TAG, "onDestroyActionMode()");
			save();
		}
	}

}