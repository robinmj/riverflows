package com.riverflows.wsclient;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.riverflows.Home;

import java.io.IOException;

/**
 * Handles the user flow for obtaining a WsSession object
 * Created by robin on 9/12/14.
 */
public class WsSessionUIHelper {

	protected final FragmentActivity activity;
	protected final LoaderManager.LoaderCallbacks<?> loaderCallbacks;
	protected final int requestCode;
	protected final int recoveryRequestCode;
	private boolean secondTry = false;
	private boolean loginRequired = false;
	private final int loaderId;

	private String accountName = null;
	private boolean sendToLoginScreen = false;

	public WsSessionUIHelper(FragmentActivity activity, final int loaderId, LoaderManager.LoaderCallbacks<?> loaderCallbacks, int requestCode, int recoveryRequestCode, boolean secondTry) {
		this.activity = activity;
		this.requestCode = requestCode;
		this.recoveryRequestCode = recoveryRequestCode;
		this.secondTry = secondTry;
		this.loaderCallbacks = loaderCallbacks;
		this.loaderId = loaderId;
	}

	public WsSession initSession() throws Exception {
		WsSession currentSession = WsSessionManager.getSession(this.activity);

		if(!(currentSession == null || currentSession.authToken == null ||
				currentSession.isExpired() || (currentSession.accountName == null && this.loginRequired))) {
			//session already initialized
			return currentSession;
		}

		if(accountName == null) {

			if(this.loginRequired || !WsSessionManager.wasPromptedToLogin()) {
				this.sendToLoginScreen = true;
				return null;
			}

			//login anonymously
			currentSession = WsSessionManager.getWsAuthToken("anonymous", null, null);
			WsSessionManager.setPromptedToLogin();
		} else {
			currentSession = WsSessionManager.loginWithGoogleOAuth2(this.activity, accountName);
		}

		WsSessionManager.notifyAccountSessionChange(currentSession, null);

		return currentSession;
	}

	protected void onNetworkError() {}

	public void authorizeCallback(final int requestCode, final int resultCode,
								  final Intent data, final Bundle loaderArgs) {
		if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) {

			Log.d(Home.TAG, "authentication succeeded");

			reExecute(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), loaderArgs);
		} else if (requestCode == this.recoveryRequestCode) {

			Log.d(Home.TAG, "recovery authentication succeeded");

			reExecute(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), loaderArgs);
		}
	}

	private void reExecute(final String accountName, final Bundle loaderArgs) {

		Log.d(Home.TAG, "account name: " + accountName);

		SharedPreferences prefs = this.activity.getSharedPreferences(WsSessionManager.PREFS_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(WsSessionManager.PREF_ACCOUNT_NAME, accountName);
		editor.commit();

		this.activity.getSupportLoaderManager().restartLoader(loaderId, loaderArgs, this.loaderCallbacks);
	}

	public void handleCompletion(final Exception exception, final Bundle loaderArgs) {
		if(exception != null) {
			if(exception instanceof UserRecoverableAuthException) {
				activity.startActivityForResult(((UserRecoverableAuthException)exception).getIntent(), recoveryRequestCode);
				return;
			}

			if(exception instanceof IOException && exception.getMessage().equals("NetworkError")) {
				onNetworkError();
			}

		}

		if(sendToLoginScreen && !secondTry) {

//			Intent loginIntent = new Intent(activity, Login.class);
//
//			activity.startActivityForResult(loginIntent, requestCode);


			//TODO make sure google play services is available


			if(accountName == null) {

				SharedPreferences settings = this.activity.getSharedPreferences(WsSessionManager.PREFS_FILE_NAME, Context.MODE_PRIVATE);

				accountName = settings.getString(WsSessionManager.PREF_ACCOUNT_NAME, null);

				if(accountName == null) {

					Intent launchChooser = AccountPicker.newChooseAccountIntent(null,
							null,
							new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
							false,
							null,
							null,
							null,
							null);

					this.activity.startActivityForResult(launchChooser, requestCode);

					return;
				} else {

					Log.d(Home.TAG, "found username stored in prefs: " + accountName);
					reExecute(accountName, loaderArgs);
				}
			}
		}
	}
}
