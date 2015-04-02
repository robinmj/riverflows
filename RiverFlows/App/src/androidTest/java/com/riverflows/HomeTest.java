package com.riverflows;

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by robin on 3/12/15.
 */

@RunWith(RobolectricGradleTestRunner.class)
public class HomeTest {
    private MockWsClient wsClient = new MockWsClient();
    private MockSessionManager mockSessionManager = new MockSessionManager();

    @Before
    public void setup() {
        RoboGuice.overrideApplicationInjector(Robolectric.application, wsClient, mockSessionManager);
    }

    public Home createHome(Intent i) throws Exception {
        ActivityController<Home> activityController= Robolectric.buildActivity(Home.class);

        activityController.withIntent(i).create().start().resume().visible();

        Home activity = activityController.get();

        return activity;
    }

    @Test
    public void shouldSelectFavoritesTab() throws Exception {
        Intent i = new Intent(Robolectric.application, Home.class);

        Home h = createHome(i);

        assertThat(h.getSupportActionBar().getSelectedTab().getText().toString(), equalTo("Favorites"));

    }
}
