package com.riverflows;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.appcompat.BuildConfig;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.riverflows.data.UserAccount;
import com.riverflows.db.CachingHttpClientWrapper;
import com.riverflows.wsclient.DataSourceController;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.fabric.sdk.android.Fabric;

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
        // Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);

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
//        GoogleAnalytics myInstance = GoogleAnalytics.getInstance(this);
//        myInstance.setAppOptOut((getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE);

        // Work around pre-Froyo bugs in HTTP connection reuse.
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }

        Logger.getLogger("").setLevel(Level.ALL);

        Log.d(TAG, "App.onCreate exit");
    }

    public int getCurrentFacetTypes(UserAccount account) {
        SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, Activity.MODE_PRIVATE);
        int facetTypes = settings.getInt(Home.PREF_FACET_TYPES, -1);

        if(facetTypes == -1) {
            if(account == null || account.getFacetTypes() == 0) {
                facetTypes = 2;
            } else {
                facetTypes = account.getFacetTypes();
            }

            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Home.PREF_FACET_TYPES, facetTypes);
        }

        return facetTypes;
    }
}
