package com.riverflows.wsclient;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.riverflows.Home;
import com.subalpine.DeferredExceptionAsyncTask;

import java.io.IOException;

public abstract class ApiCallTask<Params, Progress, Result> extends
		DeferredExceptionAsyncTask<Params, Progress, Result> {

	protected final Activity activity;
	protected final int requestCode;
	protected final int recoveryRequestCode;
	private boolean secondTry = false;
	private boolean loginRequired = false;
	
	private Params[] params = null;
	private String accountName = null;
	private boolean sendToLoginScreen = false;
	
	public ApiCallTask(Activity activity, int requestCode, int recoveryRequestCode, boolean loginRequired, boolean secondTry) {
		this.activity = activity;
		this.requestCode = requestCode;
		this.recoveryRequestCode = recoveryRequestCode;
		this.loginRequired = loginRequired;
		this.secondTry = secondTry;
	}
	
	/**
	 * For re-running the task after authentication has succeeded
	 * @param oldTask
	 */
	public ApiCallTask(ApiCallTask<Params, Progress, Result> oldTask) {
		this.activity = oldTask.activity;
		this.requestCode = oldTask.requestCode;
		this.recoveryRequestCode = oldTask.recoveryRequestCode;
		this.loginRequired = oldTask.loginRequired;
		this.secondTry = true;
	}
	
	private WsSession initSession() throws Exception {
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
	
	protected final Result tryInBackground(Params... params) throws Exception {
		//save parameters for reExecute method
		this.params = params;
		
		WsSession session = initSession();
		if(session == null) {
			return null;
		}
		
		return doApiCall(session, params);
	};
	
	/**
	 * @param session will never be null, nor will session.accessToken and session.accessTokenExpires
	 * @param params
	 * @return
	 */
	protected abstract Result doApiCall(WsSession session, Params...params) throws Exception;
	
	public boolean isSecondTry() {
		return this.secondTry;
	}
	
	protected final void onPostExecute(Result result) {
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
							new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
							false,
							null,
							null,
							null,
							null);

					this.activity.startActivityForResult(launchChooser, requestCode);

					return;
				}
			}
		}
		onNoUIRequired(result);
	};
	
	public void authorizeCallback(final int requestCode, final int resultCode,
	         final Intent data) {
	     if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) {
	         
	         Log.d(Home.TAG, "authentication succeeded");
	         
	         reExecute(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
	     } else if (requestCode == this.recoveryRequestCode) {
	         
	         Log.d(Home.TAG, "recovery authentication succeeded");
	         
	         reExecute(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
	     }
	}
	
	private void reExecute(String accountName) {
        
        Log.d(Home.TAG, "account name: " + accountName);

		SharedPreferences prefs = this.activity.getSharedPreferences(WsSessionManager.PREFS_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(WsSessionManager.PREF_ACCOUNT_NAME, accountName);
		editor.commit();
        
		try {
	        ApiCallTask<Params, Progress, Result> newTask = clone();
	        newTask.accountName = accountName;
	        newTask.execute(this.params);
		} catch(CloneNotSupportedException cnse) {
			throw new RuntimeException(cnse);
		}
	}

	protected void onNetworkError() {}
	
	/**
	 * Called on the UI thread after the user has successfully authenticated and {#doApiCall()} has
     * been executed. If an IOException was thrown, {#onNetworkError()} will be called instead
	 * @param result
	 */
	protected abstract void onNoUIRequired(Result result);
	
	@Override
	protected ApiCallTask<Params, Progress, Result> clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("ApiCallTask subclasses must override clone(), utilizing the copy constructor");
	}
}
