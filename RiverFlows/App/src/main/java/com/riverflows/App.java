package com.riverflows;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.riverflows.db.CachingHttpClientWrapper;
import com.riverflows.wsclient.DataSourceController;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by robin on 9/29/14.
 */
public class App extends Application {

    /** tag for logging */
    public static final String TAG = "RiverFlows";

    /**
     * 20 minutes
     */
    public static final long CACHE_TTL = 20 * 60 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            DataSourceController.useKeyStore(getResources().getAssets().open("trusted.keystore"));
        } catch (IOException ioe) {
            throw new RuntimeException("could not find keystore", ioe);
        }

        DataSourceController.setHttpClientWrapper(new CachingHttpClientWrapper(
                getApplicationContext(), getCacheDir(), CACHE_TTL, "text/plain"));
        DataSourceController.getDataSource("AHPS").setHttpClientWrapper(new CachingHttpClientWrapper(
                getApplicationContext(), getCacheDir(), CACHE_TTL, "text/xml"));
        DataSourceController.initCache(getCacheDir());

        //disable Google Analytics when in debug mode
        GoogleAnalytics myInstance = GoogleAnalytics.getInstance(this);
        myInstance.setAppOptOut((getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != ApplicationInfo.FLAG_DEBUGGABLE);

        // Work around pre-Froyo bugs in HTTP connection reuse.
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }

        Logger.getLogger("").setLevel(Level.ALL);

        Log.d(TAG, "App.onCreate exit");
    }
}
