package com.riverflows;

import android.content.Intent;
import android.widget.EditText;

import com.riverflows.data.UserAccount;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import roboguice.RoboGuice;

/**
 * Created by robin on 3/12/15.
 */

@RunWith(RobolectricGradleTestRunner.class)
public class HomeTest {
    private MockWsClient wsClient = new MockWsClient();

    @Before
    public void setup() {
        RoboGuice.overrideApplicationInjector(Robolectric.application, wsClient);
    }

    public Home createHome(Intent i) throws Exception {
        ActivityController<Home> activityController= Robolectric.buildActivity(Home.class);

        activityController.withIntent(i).create().start().resume().visible();

        Home activity = activityController.get();

        return activity;
    }

    @Test
    public void shouldStartSession() {

    }
}
