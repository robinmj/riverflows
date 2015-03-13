package com.riverflows;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Site;
import com.riverflows.data.UserAccount;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.factory.DestinationFacetFactory;
import com.riverflows.factory.SiteDataFactory;
import com.riverflows.factory.SiteFactory;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.clickOn;

/**
 * Created by robin on 11/14/14.
 */

@RunWith(RobolectricGradleTestRunner.class)
public class ViewDestinationTest {

    ActivityController<ViewDestination> activityController;
    CheckBox favoriteBtn;
    DestinationFragment destinationFragment;
    MockWsClient wsClient = new MockWsClient();

    @Before
    public void setup() {
        RoboGuice.overrideApplicationInjector(Robolectric.application, wsClient, new RobinSession());
    }

    public ViewDestination createViewDestination(Intent i) throws Exception {
        this.activityController= Robolectric.buildActivity(ViewDestination.class);

        this.activityController.withIntent(i).create().start().resume().visible();

        ViewDestination activity = activityController.get();

        loadExaminedViews(activity);

        return activity;
    }

    public void loadExaminedViews(ViewDestination activity) {

        assertThat(activity.getSupportFragmentManager().getFragments().size(), equalTo(1));

        this.destinationFragment = (DestinationFragment)activity.getSupportFragmentManager().getFragments().get(0);
        this.favoriteBtn = (CheckBox)this.destinationFragment.getView().findViewById(R.id.favorite_btn);
    }

    private ViewDestination simulateConfigurationChange(ViewDestination activity) {
        Bundle bundle = new Bundle();
        this.activityController.saveInstanceState(bundle).pause().stop().destroy();
        this.activityController = Robolectric.buildActivity(ViewDestination.class).withIntent(activity.getIntent());
        this.activityController.create(bundle).start().restoreInstanceState(bundle).resume().visible();
        return this.activityController.get();
    }

    @Test
    public void shouldLoadHydrograph() throws Exception {

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        Site clearCreek = clearCreekKayak.getDestination().getSite();

        when(wsClient.dsControllerMock.getSiteData(argThat(SiteFactory.matches(clearCreek)),
                argThat(equalTo(clearCreek.getSupportedVariables())),
                eq(false)))
                .thenReturn(SiteDataFactory.getClearCreekData());

        Intent i = new Intent(Robolectric.application, ViewDestination.class);

        i.putExtra(ViewDestination.KEY_DESTINATION_FACET, clearCreekKayak);

        ViewDestination activity = createViewDestination(i);

        this.destinationFragment.setZeroYMin(true);

        assertThat(this.destinationFragment.getData(), notNullValue());
        assertThat(this.destinationFragment.errorMsg, nullValue());

        activity = simulateConfigurationChange(activity);
        loadExaminedViews(activity);

        assertThat(this.destinationFragment.getData(), notNullValue());
        assertThat(this.destinationFragment.errorMsg, nullValue());
        assertThat(this.destinationFragment.zeroYMin, equalTo(true));
    }

    @Test
    public void shouldSaveFavorite() throws Exception {

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        Site clearCreek = clearCreekKayak.getDestination().getSite();

        Intent i = new Intent(Robolectric.application, ViewDestination.class);

        i.putExtra(ViewDestination.KEY_DESTINATION_FACET, clearCreekKayak);

        ViewDestination activity = createViewDestination(i);

        assertThat("precondition",
                !FavoritesDaoImpl.isFavorite(Robolectric.application, 23));

        assertThat(favoriteBtn.isChecked(), equalTo(false));

        clickOn(favoriteBtn);//add favorite

        assertThat("created favorite", FavoritesDaoImpl.isFavorite(Robolectric.application, 23));
        assertThat(favoriteBtn.isChecked(), equalTo(true));

        clickOn(favoriteBtn);//remove favorite
        assertThat("removed favorite", !FavoritesDaoImpl.isFavorite(Robolectric.application, 23));

        InOrder inOrder = inOrder(wsClient.destinationFacetsMock);

        verify(wsClient.dsControllerMock).getSiteData(argThat(SiteFactory.matches(clearCreek)),
                argThat(equalTo(clearCreek.getSupportedVariables())),
                eq(false));

        inOrder.verify(wsClient.destinationFacetsMock).saveFavorite(any(WsSession.class), eq(new Integer(23)));
        inOrder.verify(wsClient.destinationFacetsMock).removeFavorite(any(WsSession.class), eq(new Integer(23)));
    }
}
