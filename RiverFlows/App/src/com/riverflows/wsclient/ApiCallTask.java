package com.riverflows.wsclient;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.riverflows.Home;
import com.riverflows.wsclient.WsSessionManager.Session;
import com.subalpine.DeferredExceptionAsyncTask;

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
	 * @param accountName
	 */
	public ApiCallTask(ApiCallTask<Params, Progress, Result> oldTask) {
		this.activity = oldTask.activity;
		this.requestCode = oldTask.requestCode;
		this.recoveryRequestCode = oldTask.recoveryRequestCode;
		this.loginRequired = oldTask.loginRequired;
		this.secondTry = true;
	}
	
	private Session initSession() throws Exception {
		Session currentSession = WsSessionManager.getSession();
		
		if(!(currentSession == null || currentSession.authToken == null ||
				currentSession.isExpired() || (currentSession.accountName == null && this.loginRequired))) {
			//session already initialized
			return currentSession;
		}
		
		if(this.loginRequired || !WsSessionManager.wasPromptedToLogin()) {
			this.sendToLoginScreen = true;
			return null;
		}
		
		if(accountName == null) {
			//login anonymously
			currentSession = WsSessionManager.getWsAuthToken("anonymous", null, null);
			WsSessionManager.setPromptedToLogin();
		} else {
			currentSession = WsSessionManager.loginWithGoogleOAuth2(this.activity, accountName, recoveryRequestCode);
		}
		
        WsSessionManager.notifyAccountSessionChange(currentSession, null);
		
		return currentSession;
		
		//check for facebook access token- if one exists, use it to get a request token and start a session
//		Facebook facebook = FacebookHelper.getFacebook(context);
//		
//		if(facebook.isSessionValid()) {
//			//this shouldn't happen very often since it's unlikely that the refresh token will be invalid
//			// while a valid facebook token is present
//			Log.d(TAG, "found saved facebook token");
//			try {
//				currentSession = associateFacebook(this.context, facebook.getAccessToken(), facebook.getAccessExpires(), currentSession);
//			} finally {
//				//this will result in a duplicate call to the session listeners (not ideal), but it will
//				// happen only on very rare occasions (see above)
//		        notifyAccountSessionChange(currentSession, null);
//			}
//		}
	}
	
	protected final Result tryInBackground(Params... params) throws Exception {
		//save parameters for reExecute method
		this.params = params;
		
		Session session = initSession();
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
	protected abstract Result doApiCall(Session session, Params...params);
	
	public boolean isSecondTry() {
		return this.secondTry;
	}
	
	protected final void onPostExecute(Result result) {
		if(sendToLoginScreen && !secondTry) {

//			Intent loginIntent = new Intent(activity, Login.class);
//			
//			activity.startActivityForResult(loginIntent, requestCode);
			

			//TODO make sure google play services is available

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
        
		try {
	        ApiCallTask<Params, Progress, Result> newTask = clone();
	        newTask.accountName = accountName;
	        newTask.execute(this.params);
		} catch(CloneNotSupportedException cnse) {
			throw new RuntimeException(cnse);
		}
	}
	
	/**
	 * Called if the user was logged in without the need for
	 * a redirect to the login screen, or this is the second try
	 * @param result
	 */
	protected abstract void onNoUIRequired(Result result);
	
	@Override
	protected ApiCallTask<Params, Progress, Result> clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("ApiCallTask subclasses must override clone(), utilizing the copy constructor");
	}
}
