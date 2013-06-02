package com.subalpine;

import android.os.AsyncTask;

public abstract class DeferredExceptionAsyncTask<Params, Progress, Result> extends
		AsyncTask<Params, Progress, Result> {
	
	protected Exception exception;
	
	protected final Result doInBackground(Params... params) {
		try {
			return tryInBackground(params);
		} catch(Exception e) {
			this.exception = e;
		}
		return null;
	}
	
	protected abstract Result tryInBackground(Params... params) throws Exception;
}
