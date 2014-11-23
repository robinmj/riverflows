package com.riverflows;

import android.app.Application;
import android.util.Log;

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

        Log.d(App.TAG, "TestApp.onCreate exit");
    }

}
