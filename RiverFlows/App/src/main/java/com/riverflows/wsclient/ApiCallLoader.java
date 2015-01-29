package com.riverflows.wsclient;

import android.app.Activity;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.riverflows.App;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by robin on 7/19/14.
 */
public abstract class ApiCallLoader<Result> extends AsyncTaskLoader<Result> {

	protected Exception exception = null;

	protected final WsSessionUIHelper uiHelper;

    protected final AtomicBoolean wasAborted = new AtomicBoolean(false);

	protected ApiCallLoader(Activity activity, WsSessionUIHelper uiHelper) {
		super(activity);

		this.uiHelper = uiHelper;
	}

	@Override
	public Result loadInBackground() {
        Log.v(App.TAG, getClass().getSimpleName() + ".loadInBackground()");
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
            Log.i(App.TAG, "tryInBackground() aborted");
            wasAborted.set(true);
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
