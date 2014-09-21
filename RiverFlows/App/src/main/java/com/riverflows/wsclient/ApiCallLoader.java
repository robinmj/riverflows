package com.riverflows.wsclient;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.riverflows.Home;

import java.io.IOException;

/**
 * Created by robin on 7/19/14.
 */
public abstract class ApiCallLoader<Result> extends AsyncTaskLoader<Result> {

	protected Exception exception = null;

	protected final WsSessionUIHelper uiHelper;

	protected ApiCallLoader(Activity activity, WsSessionUIHelper uiHelper) {
		super(activity);

		this.uiHelper = uiHelper;
	}

	@Override
	public Result loadInBackground() {
		try {
			return tryInBackground();
		} catch(Exception e) {
			this.exception = e;
		}
		return null;
	}

	public Exception getException() {
		return exception;
	}

	protected final Result tryInBackground() throws Exception {
		//save parameters for reExecute method

		WsSession session = uiHelper.initSession();
		if(session == null) {
			return null;
		}

		return doApiCall(session);
	};

	/**
	 * @param session will never be null, nor will session.accessToken and session.accessTokenExpires
	 * @return
	 */
	protected abstract Result doApiCall(WsSession session) throws Exception;
}
