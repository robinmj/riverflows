package com.riverflows;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.TextView;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.factory.DestinationFacetFactory;
import com.riverflows.factory.SiteDataFactory;
import com.riverflows.factory.SiteFactory;
import com.riverflows.wsclient.UsgsCsvDataSource;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.util.ActivityController;

import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by robin on 11/14/14.
 */

@RunWith(RobolectricGradleTestRunner.class)
public class ViewSiteTest {

    ActivityController<ViewSite> activityController;
    CheckBox favoriteBtn;
    SiteFragment siteFragment;
    MockWsClient wsClient = new MockWsClient();

    public ViewSite createViewSite(Intent i) throws Exception {
        this.activityController= Robolectric.buildActivity(ViewSite.class);

        this.activityController.withIntent(i).setup();

        ViewSite activity = activityController.get();

        loadExaminedViews(activity);

        return activity;
    }

    public void loadExaminedViews(ViewSite activity) {

        assertThat(activity.getSupportFragmentManager().getFragments().size(), equalTo(1));

        this.siteFragment = (SiteFragment)activity.getSupportFragmentManager().getFragments().get(0);
        this.favoriteBtn = (CheckBox)this.siteFragment.getView().findViewById(R.id.favorite_btn);
    }

    private ViewSite simulateConfigurationChange(ViewSite activity) {
        Bundle bundle = new Bundle();
        this.activityController.saveInstanceState(bundle).pause().stop().destroy();
        this.activityController = Robolectric.buildActivity(ViewSite.class).withIntent(activity.getIntent());
        this.activityController.create(bundle).start().restoreInstanceState(bundle).resume().visible();
        return this.activityController.get();
    }

    @Test
    public void shouldLoadHydrograph() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        Site clearCreek = clearCreekKayak.getDestination().getSite();

        when(wsClient.dsControllerMock.getSiteData(argThat(SiteFactory.matches(clearCreek)),
                argThat(equalTo(clearCreek.getSupportedVariables())),
                eq(false)))
                .thenReturn(SiteDataFactory.getClearCreekData());

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, clearCreek);

        ViewSite activity = createViewSite(i);

        this.siteFragment.setZeroYMin(true);

        assertThat(this.siteFragment.getData(), notNullValue());
        assertThat(this.siteFragment.errorMsg, nullValue());

        activity = simulateConfigurationChange(activity);
        loadExaminedViews(activity);

        assertThat(this.siteFragment.getData(), notNullValue());
        assertThat(this.siteFragment.errorMsg, nullValue());
        assertThat(this.siteFragment.zeroYMin, equalTo(true));

        assertThat(((TextView) activity.findViewById(R.id.title)).getText().toString(),
                equalTo(clearCreek.getSupportedVariables()[1].getName()));

        assertThat(this.siteFragment.getVariable(), equalTo(clearCreek.getSupportedVariables()[1]));
    }

    @Test
    public void shouldLoadHydrographWithVariable() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        Site clearCreek = clearCreekKayak.getDestination().getSite();

        when(wsClient.dsControllerMock.getSiteData(argThat(SiteFactory.matches(clearCreek)),
                argThat(equalTo(clearCreek.getSupportedVariables())),
                eq(false)))
                .thenReturn(SiteDataFactory.getClearCreekData());

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, clearCreek);
        i.putExtra(ViewSite.KEY_VARIABLE, clearCreek.getSupportedVariables()[0]);

        ViewSite activity = createViewSite(i);

        this.siteFragment.setZeroYMin(true);

        assertThat(this.siteFragment.getData(), notNullValue());
        assertThat(this.siteFragment.errorMsg, nullValue());

        activity = simulateConfigurationChange(activity);
        loadExaminedViews(activity);

        assertThat(this.siteFragment.getData(), notNullValue());
        assertThat(this.siteFragment.errorMsg, nullValue());
        assertThat(this.siteFragment.zeroYMin, equalTo(true));

        assertThat(((TextView) activity.findViewById(R.id.title)).getText().toString(),
                equalTo(clearCreek.getSupportedVariables()[0].getName()));
    }

    @Test
    public void shouldHandleException() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());

        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        Site clearCreek = clearCreekKayak.getDestination().getSite();

        when(wsClient.dsControllerMock.getSiteData(argThat(SiteFactory.matches(clearCreek)),
                argThat(equalTo(clearCreek.getSupportedVariables())),
                eq(false)))
                .thenThrow(new RuntimeException("Misc Error"));

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, clearCreek);
        i.putExtra(ViewSite.KEY_VARIABLE, clearCreek.getSupportedVariables()[0]);

        ViewSite activity = createViewSite(i);

        assertThat(this.siteFragment.getData(), nullValue());
        assertThat(this.siteFragment.errorMsg, equalTo("Error loading data from " + clearCreek + "; Misc Error"));

        activity = simulateConfigurationChange(activity);
        loadExaminedViews(activity);

        assertThat(this.siteFragment.getData(), nullValue());
        assertThat(this.siteFragment.errorMsg, equalTo("Error loading data from " + clearCreek + "; Misc Error"));

        assertThat(((TextView) activity.findViewById(R.id.title)).getText().toString(),
                equalTo(clearCreek.getSupportedVariables()[0].getName()));
    }

    @Test
    public void shouldSaveFavorite() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());

        Site fountainCreek = SiteFactory.getFountainCreek();
        Variable var = UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS;

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, fountainCreek);
        i.putExtra(ViewSite.KEY_VARIABLE, var);

        ViewSite activity = createViewSite(i);

        assertThat("precondition",
                !FavoritesDaoImpl.isFavorite(RuntimeEnvironment.application, fountainCreek.getSiteId(), var.getId()));

        assertThat(favoriteBtn.isChecked(), equalTo(false));

        Menu menu = shadowOf(activity).getOptionsMenu();
        assertThat("edit favorite function not accessible", !menu.findItem(R.id.mi_edit_favorite).isVisible());
        assertThat("edit destination function not accessible", !menu.findItem(R.id.mi_edit_destination).isVisible());
        assertThat("create  destination function is accessible", menu.findItem(R.id.mi_create_destination).isVisible());

        favoriteBtn.performClick();//add favorite

        assertThat("created favorite", FavoritesDaoImpl.isFavorite(RuntimeEnvironment.application, fountainCreek.getSiteId(), var.getId()));
        assertThat(favoriteBtn.isChecked(), equalTo(true));
        assertThat("edit favorite function should be accessible", menu.findItem(R.id.mi_edit_favorite).isVisible());
        assertThat("edit destination function should not be accessible", !menu.findItem(R.id.mi_edit_destination).isVisible());
        assertThat("create destination function should be accessible", menu.findItem(R.id.mi_create_destination).isVisible());

        favoriteBtn.performClick();//remove favorite
        assertThat("removed favorite", !FavoritesDaoImpl.isFavorite(RuntimeEnvironment.application, fountainCreek.getSiteId(), var.getId()));

        verify(wsClient.dsControllerMock).getSiteData(argThat(SiteFactory.matches(fountainCreek)),
                argThat(equalTo(fountainCreek.getSupportedVariables())),
                eq(false));

        assertThat("edit favorite function should not be accessible", !menu.findItem(R.id.mi_edit_favorite).isVisible());
        assertThat("edit destination function should not be accessible", !menu.findItem(R.id.mi_edit_destination).isVisible());
    }

    @Test
    public void shouldDisplayCorrectFavoriteButtonState() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());

        Site fountainCreek = SiteFactory.getFountainCreek();
        Variable var = UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS;

        FavoritesDaoImpl.createFavorite(RuntimeEnvironment.application,
                new Favorite(fountainCreek, UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS.getId()));

        when(wsClient.dsControllerMock.getSiteData(argThat(SiteFactory.matches(fountainCreek)),
                argThat(equalTo(fountainCreek.getSupportedVariables())),
                eq(false)))
                .thenReturn(SiteDataFactory.getFountainCreekData());

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, fountainCreek);
        i.putExtra(ViewSite.KEY_VARIABLE, var);

        ViewSite activity = createViewSite(i);

        Menu menu = shadowOf(activity).getOptionsMenu();

        assertThat(favoriteBtn.isChecked(), equalTo(true));
        assertThat("edit favorite function should be accessible", menu.findItem(R.id.mi_edit_favorite).isVisible());

        this.siteFragment.setVariable(UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT);

        assertThat(favoriteBtn.isChecked(), equalTo(false));
        assertThat("edit favorite function not accessible", !menu.findItem(R.id.mi_edit_favorite).isVisible());

        activity = simulateConfigurationChange(activity);
        loadExaminedViews(activity);

        //should retain state after configuration change
        assertThat(favoriteBtn.isChecked(), equalTo(false));
        assertThat("edit favorite function not accessible", !menu.findItem(R.id.mi_edit_favorite).isVisible());
    }

    @Test
    public void shouldPreventEditingRawFavForDestination() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());

        DestinationFacet facet = DestinationFacetFactory.getFountainCreekKayak();

        Site fountainCreek = facet.getDestination().getSite();

        FavoritesDaoImpl.createFavorite(RuntimeEnvironment.application,
                new Favorite(facet));

        when(wsClient.dsControllerMock.getSiteData(argThat(SiteFactory.matches(fountainCreek)),
                argThat(equalTo(fountainCreek.getSupportedVariables())),
                eq(false)))
                .thenReturn(SiteDataFactory.getFountainCreekData());

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, fountainCreek);
        i.putExtra(ViewSite.KEY_VARIABLE, facet.getVariable());

        ViewSite activity = createViewSite(i);

        Menu menu = shadowOf(activity).getOptionsMenu();

        assertThat(favoriteBtn.isChecked(), equalTo(true));
        assertThat("edit favorite function should not be accessible", !menu.findItem(R.id.mi_edit_favorite).isVisible());
    }

    @Test
    public void shouldDisplayCorrectStateAfterVariableChange() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());

        Site fountainCreek = SiteFactory.getFountainCreek();
        Variable var = UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS;

        FavoritesDaoImpl.createFavorite(RuntimeEnvironment.application,
                new Favorite(fountainCreek, UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT.getId()));

        when(wsClient.dsControllerMock.getSiteData(argThat(SiteFactory.matches(fountainCreek)),
                argThat(equalTo(fountainCreek.getSupportedVariables())),
                eq(false)))
                .thenReturn(SiteDataFactory.getFountainCreekData());

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, fountainCreek);
        i.putExtra(ViewSite.KEY_VARIABLE, var);

        ViewSite activity = createViewSite(i);

        assertThat(favoriteBtn.isChecked(), equalTo(false));

        Menu menu = shadowOf(activity).getOptionsMenu();
        assertThat("edit favorite function not accessible", !menu.findItem(R.id.mi_edit_favorite).isVisible());

        this.siteFragment.setVariable(UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT);

        assertThat(favoriteBtn.isChecked(), equalTo(true));

        assertThat("edit favorite function should be accessible", menu.findItem(R.id.mi_edit_favorite).isVisible());
    }

    @Test
    public void shouldDisplayCorrectStateAfterVariableChangeAndFailedResponse() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());

        Site fountainCreek = SiteFactory.getFountainCreek();
        Variable var = UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS;

        FavoritesDaoImpl.createFavorite(RuntimeEnvironment.application,
                new Favorite(fountainCreek, UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT.getId()));

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, fountainCreek);
        i.putExtra(ViewSite.KEY_VARIABLE, var);

        ViewSite activity = createViewSite(i);

        assertThat(favoriteBtn.isChecked(), equalTo(false));

        Menu menu = shadowOf(activity).getOptionsMenu();
        assertThat("edit favorite function not accessible", !menu.findItem(R.id.mi_edit_favorite).isVisible());

        this.siteFragment.setVariable(UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT);

        assertThat(favoriteBtn.isChecked(), equalTo(true));

        assertThat("edit favorite function should be accessible", menu.findItem(R.id.mi_edit_favorite).isVisible());
    }

    @Test
    public void shouldSaveFavoriteWhenNotLoggedIn() throws Exception {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient);

        Site fountainCreek = SiteFactory.getFountainCreek();
        Variable var = UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS;

        Intent i = new Intent(RuntimeEnvironment.application, ViewDestination.class);

        i.putExtra(ViewSite.KEY_SITE, fountainCreek);
        i.putExtra(ViewSite.KEY_VARIABLE, var);

        ViewSite activity = createViewSite(i);

        assertThat("precondition",
                !FavoritesDaoImpl.isFavorite(RuntimeEnvironment.application, fountainCreek.getSiteId(), var.getId()));

        assertThat(favoriteBtn.isChecked(), equalTo(false));

        favoriteBtn.performClick();//add favorite

        assertThat("created favorite", FavoritesDaoImpl.isFavorite(RuntimeEnvironment.application, fountainCreek.getSiteId(), var.getId()));
        assertThat(favoriteBtn.isChecked(), equalTo(true));

        favoriteBtn.performClick();//remove favorite
        assertThat("removed favorite", !FavoritesDaoImpl.isFavorite(RuntimeEnvironment.application, fountainCreek.getSiteId(), var.getId()));

        verify(wsClient.dsControllerMock).getSiteData(argThat(SiteFactory.matches(fountainCreek)),
                argThat(equalTo(fountainCreek.getSupportedVariables())),
                eq(false));
    }

    @After
    public void cleanup() {
        RobolectricGradleTestRunner.resetDbSingleton();
    }
}
