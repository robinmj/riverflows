package com.riverflows;

import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

import com.google.analytics.tracking.android.GoogleAnalytics;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is automatically loaded by Robolectric in place of com.riverflows.App
 * Created by robin on 11/11/14.
 */
public class TestApp extends App {

    @Override
    public void onCreate() {
        //don't call super.onCreate()- instead do any necessary configuration here

        Logger.getLogger("").setLevel(Level.ALL);

        GoogleAnalytics myInstance = GoogleAnalytics.getInstance(this);
        myInstance.setAppOptOut(true);

        Log.d(App.TAG, "TestApp.onCreate exit");
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        //prevent GoogleAnalyticsService from binding
        return false;
    }

}
