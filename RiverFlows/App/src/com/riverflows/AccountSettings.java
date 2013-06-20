package com.riverflows;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.riverflows.data.UserAccount;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UnexpectedResultException;
import com.riverflows.wsclient.UserAccounts;
import com.riverflows.wsclient.WsSessionManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by robin on 6/4/13.
 */
public class AccountSettings extends SherlockActivity {

	private static final int REQUEST_SAVE = 642048;
	private static final int REQUEST_LOGIN_FOR_SAVE = 32078;

	private String[] facetNames;
	private int[] facetValues;
	private CheckBox[] facetCheckBoxes;

	private SaveTask saveTask;

	private UserAccount currentUser;

	private ActionMode mMode;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currentUser = WsSessionManager.getSession().userAccount;

		setContentView(R.layout.account_settings);

		facetNames = getResources().getStringArray(R.array.facet_type_names);
		facetValues = getResources().getIntArray(R.array.facet_type_values);
		facetCheckBoxes = new CheckBox[facetValues.length];

		LinearLayout mainLayout = (LinearLayout)findViewById(R.id.main);

		TextView usernameField = (TextView)findViewById(R.id.username);
		usernameField.setText(currentUser.getEmail());

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

		mMode = startActionMode(new EditActionMode());
	}

	private int getPx(int dp) {
		Resources r = getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	public void save() {
		setSupportProgressBarIndeterminate(true);

		UserAccount account = new UserAccount(WsSessionManager.getSession().userAccount);

		int facetTypes = 0;
		for(int a = 0; a < facetCheckBoxes.length; a++) {

			if(facetCheckBoxes[a].isChecked()) {
				facetTypes += (Integer)facetCheckBoxes[a].getTag();
			}
		}

		account.setFacetTypes(facetTypes);

		saveTask = new SaveTask(false);
		saveTask.execute(account);

	}

	public void cancel() {
		finish();
	}

	private class SaveTask extends ApiCallTask<UserAccount,Integer,String> {

		public SaveTask(boolean secondTry) {
			super(AccountSettings.this, REQUEST_SAVE, REQUEST_LOGIN_FOR_SAVE, true, secondTry);
		}

		@Override
		protected String doApiCall(WsSessionManager.Session session, UserAccount... params) throws Exception {

			HttpPut putCmd = new HttpPut(WsSessionManager.WS_BASE_URL + "/account/update.json?auth_token=" + session.authToken);
			HttpClient client = new DataSourceController.SSLHttpClient();

//			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
//			String userString = UserAccounts.userAsJson(params[0]).toString();
//
//			nameValuePairs.add(new BasicNameValuePair("user", userString));
//			nameValuePairs.add(new BasicNameValuePair("auth_token", session.authToken));
//
//			putCmd.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			JSONObject entity = new JSONObject();

			//entity.put("auth_token", session.authToken);
			entity.put("account", UserAccounts.userAsJson(params[0]));

			putCmd.setEntity(new StringEntity(entity.toString()));

			putCmd.addHeader("Content-Type", "application/json");
			putCmd.addHeader("Accept", "application/json");

			HttpResponse httpResponse = client.execute(putCmd);

			Log.d(Home.TAG, putCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

			if(httpResponse.getStatusLine().getStatusCode() != 200) {
				throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
			}

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

			getSupportMenuInflater().inflate(R.menu.edit_account_action_mode, menu);

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