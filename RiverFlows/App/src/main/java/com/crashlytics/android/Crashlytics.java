package com.crashlytics.android;

import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;

public class Crashlytics {

    public final CrashlyticsCore core;

    private Crashlytics(CrashlyticsCore core) {
        this.core = core;
    }

    public static class Builder {
        public Builder() {}

        private CrashlyticsCore core;

        public Crashlytics.Builder core(CrashlyticsCore c) {
            this.core = c;
            return this;
        }

        public Crashlytics build() {
            return new Crashlytics(this.core);
        }
    }

    public static Crashlytics getInstance() {
        return Fabric.getInstance();
    }

    private static CrashlyticsCore getCore() {
        Crashlytics instance = Crashlytics.getInstance();
        if (instance == null) {
            return null;
        }

        return instance.core;
    }

    public static void logException(Throwable t) {
        final CrashlyticsCore core = getCore();

        if (core == null) {
            return;
        }

        core.log(t);
    }

    public static void setString(String name, String s) {
        final CrashlyticsCore core = getCore();

        if (core == null) {
            return;
        }

        core.setString(name, s);
    }
}
