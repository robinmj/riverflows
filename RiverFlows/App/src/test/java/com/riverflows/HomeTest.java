package com.riverflows;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.widget.TextView;

import com.riverflows.wsclient.WsSessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.robolectric.Robolectric.shadowOf;

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

        ActivityController<Home> c = activityController.withIntent(i);

        System.out.println("activityController-" + c);

        c.create().start().resume().visible();

        Home activity = activityController.get();

        return activity;
    }

    @Test
    public void shouldSelectFavoritesTab() throws Exception {
        Intent i = new Intent(Robolectric.application, Home.class);

        Home h = createHome(i);

        assertThat(h.getSupportActionBar().getSelectedTab().getText().toString(), equalTo("Favorites"));

    }

    @Test
    public void testChooseAccount() throws Exception {
        Intent i = new Intent(Robolectric.application, Home.class);

        Home h = createHome(i);

        TextView signInInstView = (TextView)h.findViewById(R.id.register_sign_in_instructions);
        ClickableSpan[] links = ((SpannableString)signInInstView.getText()).getSpans(0, 50, ClickableSpan.class);
        links[0].onClick(signInInstView);

        assertThat(shadowOf(h).getNextStartedActivity().getAction(), equalTo("com.google.android.gms.common.account.CHOOSE_ACCOUNT"));

    }

    @Test
    public void testSetupAccount() throws Exception {
        Intent i = new Intent(Robolectric.application, Home.class);

        Home h = createHome(i);

        SharedPreferences prefs = h.getSharedPreferences(WsSessionManager.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(WsSessionManager.PREF_ACCOUNT_NAME, "robin.m.j");
        editor.commit();

        TextView signInInstView = (TextView)h.findViewById(R.id.register_sign_in_instructions);
        ClickableSpan[] links = ((SpannableString)signInInstView.getText()).getSpans(0, 50, ClickableSpan.class);
        links[0].onClick(signInInstView);

        Intent expectedIntent = new Intent(h, AccountSettings.class);
        assertThat(shadowOf(h).getNextStartedActivity(), equalTo(expectedIntent));

    }
}
