package com.riverflows;

import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.analytics.tracking.android.GoogleAnalytics;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.fabric.sdk.android.Fabric;
import roboguice.RoboGuice;

/**
 * This is automatically loaded by Robolectric in place of com.riverflows.App
 * Created by robin on 11/11/14.
 */
public class TestApp extends App {

    @Override
    public void onCreate() {
        //don't call super.onCreate()- instead do any necessary configuration here

        Logger.getLogger("").setLevel(Level.ALL);

        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(true).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);

        RoboGuice.setUseAnnotationDatabases(false);

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
