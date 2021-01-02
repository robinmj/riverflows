package io.fabric.sdk.android;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

public class Fabric {
    private static Crashlytics instance;

    public static void with(Application a, Crashlytics o) {
        Fabric.instance = o;
    }

    public static Crashlytics getInstance() {
        return Fabric.instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }
}
