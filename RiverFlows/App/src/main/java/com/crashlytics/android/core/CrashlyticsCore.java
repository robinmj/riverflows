package com.crashlytics.android.core;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.util.HashMap;
import java.util.Map;

public class CrashlyticsCore {
    private CrashlyticsCore(){}

    private final Map<String,String> stringMeta = new HashMap<>();

    public static class Builder {
        public Builder() {}

        public CrashlyticsCore.Builder disabled(boolean isDisabled) {
            return this;
        }

        public CrashlyticsCore build(){
            return new CrashlyticsCore();
        }
    }

    public void setString(String name, String s) {
        synchronized (stringMeta) {
            stringMeta.put(name, s);
        }
    }

    public void log(String msg) {
        this.log(Log.DEBUG, "riverflows", msg);
    }

    public void log(int logLevel, String tag, String msg) {
        Log.println(logLevel, tag, msg);
    }

    public void log(Throwable t) {
        synchronized (stringMeta) {
            for (Map.Entry<String, String> entry: stringMeta.entrySet()) {
                Log.e("riverflows", "" + entry.getKey() + ":" + entry.getValue());
            }
            stringMeta.clear();
        }
        Log.wtf("riverflows", t);
    }

    public static void logException(Throwable t) {
        Crashlytics.logException(t);
    }
}
