package com.riverflows;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Page;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.UserAccount;
import com.riverflows.factory.DestinationFacetFactory;
import com.riverflows.factory.SiteDataFactory;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

        h.signIn();

        assertThat(shadowOf(h).getNextStartedActivity().getAction(), equalTo("com.google.android.gms.common.account.CHOOSE_ACCOUNT"));

    }

    @Test
    public void testSetupAccount() throws Exception {

        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        account.setFacetTypes(0);
        mockSessionManager.session = new WsSession("robin.m.j", account, "", System.currentTimeMillis() + 10 * 60 * 1000);

        RoboGuice.overrideApplicationInjector(Robolectric.application, wsClient, mockSessionManager);

        Intent i = new Intent(Robolectric.application, Home.class);

        Home h = createHome(i);

        SharedPreferences prefs = h.getSharedPreferences(WsSessionManager.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(WsSessionManager.PREF_ACCOUNT_NAME, "robin.m.j");
        editor.commit();

        h.signIn();

        Intent expectedIntent = new Intent(h, AccountSettings.class);
        assertThat(shadowOf(h).getNextStartedActivity(), equalTo(expectedIntent));

    }

    @Test
    public void testPostLogin() throws Exception {

        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");
        account.setFacetTypes(4);
        mockSessionManager.session = new WsSession("robin.m.j", account, "", System.currentTimeMillis() + 10 * 60 * 1000);

        RoboGuice.overrideApplicationInjector(Robolectric.application, wsClient, mockSessionManager);

        ArrayList<DestinationFacet> mockResults = new ArrayList<DestinationFacet>();
        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        mockResults.add(clearCreekKayak);
        when(wsClient.destinationFacetsMock.getFavorites(any(WsSession.class))).thenReturn(mockResults);

        List<FavoriteData> mockData = new ArrayList<FavoriteData>();
        Favorite clearCreekFav = new Favorite(clearCreekKayak);
        SiteData clearCreekData = SiteDataFactory.getClearCreekData();
        FavoriteData clearCreekFavData = new FavoriteData(clearCreekFav,clearCreekData, CODWRDataSource.VTYPE_STREAMFLOW_CFS);
        mockData.add(clearCreekFavData);
        when(wsClient.dsControllerMock.getFavoriteData(anyList(), anyBoolean())).thenReturn(mockData);

        Intent i = new Intent(Robolectric.application, Home.class);

        Home h = createHome(i);

        SharedPreferences prefs = h.getSharedPreferences(WsSessionManager.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(WsSessionManager.PREF_ACCOUNT_NAME, "robin.m.j");
        editor.commit();

        Favorites favorites = null;

        for(Fragment f : h.getSupportFragmentManager().getFragments()) {
            if(f instanceof Favorites) {
                favorites = (Favorites)f;
            }
        }

        assertThat(favorites.getListView().getVisibility(), equalTo(View.VISIBLE));
        assertThat(favorites.getListView().getEmptyView().getVisibility(), equalTo(View.GONE));

    }
}
