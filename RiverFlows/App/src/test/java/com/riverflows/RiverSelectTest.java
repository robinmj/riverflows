package com.riverflows;

import android.content.Context;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Page;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.factory.DestinationFacetFactory;
import com.riverflows.factory.SiteDataFactory;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.WsSession;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.fakes.RoboSubMenu;
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by robin on 11/18/14.
 */
@RunWith(RobolectricGradleTestRunner.class)
public class RiverSelectTest {

    MockWsClient wsClient = new MockWsClient();

    EditText filter_field = null;
    ListView listView = null;

    RobinSession robinSession = new RobinSession();

    @Before
    public void setup() {
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, robinSession);
    }


    private RiverSelect createRiverSelect(Intent i) throws Exception {
        ActivityController<RiverSelect> activityController= Robolectric.buildActivity(RiverSelect.class);

        activityController.withIntent(i).create().start().resume().visible();

        RiverSelect activity = activityController.get();

        filter_field = (EditText)activity.findViewById(R.id.site_filter_field);

        listView = (ListView)activity.findViewById(android.R.id.list);

        return activity;
    }

    @Test
    public void shouldLoadSitesIfNotLoggedIn() throws Exception {
        this.robinSession.wsSessionManager.setSession(null);

        Map<SiteId, SiteData> mockData = new HashMap<SiteId, SiteData>();

        SiteData clearCreekData = SiteDataFactory.getClearCreekData();

        mockData.put(clearCreekData.getSite().getSiteId(), clearCreekData);

        SiteData fountainCreekData = SiteDataFactory.getFountainCreekData();

        mockData.put(fountainCreekData.getSite().getSiteId(), fountainCreekData);

        when(wsClient.dsControllerMock.getSiteData(USState.CO, false)).thenReturn(mockData);

        Intent i = new Intent(RuntimeEnvironment.application, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.CO);

        RiverSelect activity = createRiverSelect(i);

        assertThat(activity.getListView().getAdapter().getCount(), equalTo(2));
    }

    @Test
    public void shouldLoadSitesAndDestinations() throws Exception {

        Map<SiteId, SiteData> mockData = new HashMap<SiteId, SiteData>();
        SiteData clearCreekData = SiteDataFactory.getClearCreekData();
        mockData.put(clearCreekData.getSite().getSiteId(), clearCreekData);
        SiteData fountainCreekData = SiteDataFactory.getFountainCreekData();
        mockData.put(fountainCreekData.getSite().getSiteId(), fountainCreekData);
        when(wsClient.dsControllerMock.getSiteData(USState.CO, false)).thenReturn(mockData);

        ArrayList<DestinationFacet> pageElements = new ArrayList<DestinationFacet>();
        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        pageElements.add(clearCreekKayak);
        Page<DestinationFacet> mockResults = new Page<DestinationFacet>(pageElements, 1);

        HashMap<String, List<String>> filterParams = new HashMap<String, List<String>>();
        filterParams.put("state", Collections.singletonList(USState.CO.getAbbrev()));
        filterParams.put("facet_types", Collections.singletonList("2"));

        when(wsClient.destinationFacetsMock.get(any(WsSession.class), argThat(new MapListMatcher(filterParams)), isNull(Integer.class), isNull(Integer.class))).thenReturn(mockResults);

        Intent i = new Intent(RuntimeEnvironment.application, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.CO);

        RiverSelect activity = createRiverSelect(i);

        MapItemAdapter mapItemAdapter = (MapItemAdapter)activity.getListView().getAdapter();

        assertThat(mapItemAdapter.getCount(), equalTo(3));
        assertThat(mapItemAdapter.getItem(0).getSite().getName(), equalTo(clearCreekData.getSite().getName()));
        assertThat(mapItemAdapter.getItem(1).getSite().getName(), equalTo(fountainCreekData.getSite().getName()));
        assertThat(mapItemAdapter.getItem(2).destinationFacet.getDestination().getName(), equalTo(clearCreekKayak.getDestination().getName()));

        filter_field.setText("Terrible");

        assertThat(mapItemAdapter.getCount(), equalTo(1));
        assertThat(mapItemAdapter.getItem(0).destinationFacet.getDestination().getName(), equalTo(clearCreekKayak.getDestination().getName()));
    }

    @Test
    public void shouldAssembleSiteContextMenuWhenSignedIn() throws Throwable {

        Map<SiteId, SiteData> mockData = new HashMap<SiteId, SiteData>();
        SiteData clearCreekData = SiteDataFactory.getClearCreekData();
        mockData.put(clearCreekData.getSite().getSiteId(), clearCreekData);
        SiteData fountainCreekData = SiteDataFactory.getFountainCreekData();
        mockData.put(fountainCreekData.getSite().getSiteId(), fountainCreekData);
        when(wsClient.dsControllerMock.getSiteData(USState.CO, false)).thenReturn(mockData);

        ArrayList<DestinationFacet> pageElements = new ArrayList<DestinationFacet>();
        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        pageElements.add(clearCreekKayak);
        Page<DestinationFacet> mockResults = new Page<DestinationFacet>(pageElements, 1);

        when(wsClient.destinationFacetsMock.get(any(WsSession.class), anyMap(), isNull(Integer.class), isNull(Integer.class))).thenReturn(mockResults);

        Intent i = new Intent(RuntimeEnvironment.application, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.CO);

        RiverSelect activity = createRiverSelect(i);

        ContextMenu mockMenu = mock(ContextMenu.class);

        Variable[] ccSupportedVars = clearCreekData.getSite().getSupportedVariables();

        SubMenu mockAddFavSubMenu = mock(SubMenu.class);

        when(mockMenu.addSubMenu(eq(ContextMenu.NONE), eq(ccSupportedVars.length), eq(ccSupportedVars.length), eq("Add To Favorites"))).thenReturn(mockAddFavSubMenu);

        int mi_id = 0;
        MenuItem streamflowItem = mock(MenuItem.class);
        when(mockMenu.add(eq(ContextMenu.NONE), eq(0), eq(0), eq("Streamflow, cfs"))).thenReturn(streamflowItem);

        MenuItem gaugeHeightItem = mock(MenuItem.class);
        when(mockMenu.add(eq(ContextMenu.NONE), eq(1), eq(1), eq("Gauge Height, ft"))).thenReturn(gaugeHeightItem);

        RoboMenuItem streamflowFavItem = new RoboMenuItem(mi_id++);
        when(mockAddFavSubMenu.add(eq(ContextMenu.NONE), eq(0), eq(0), eq("Streamflow, cfs"))).thenReturn(streamflowFavItem);

        RoboMenuItem gaugeHeightFavItem = new RoboMenuItem(mi_id++);
        when(mockAddFavSubMenu.add(eq(ContextMenu.NONE), eq(1), eq(1), eq("Gauge Height, ft"))).thenReturn(gaugeHeightFavItem);

        activity.onCreateContextMenu(mockMenu, activity.getListView(), new AdapterView.AdapterContextMenuInfo(activity.getListView(), 0, 0));

        verify(streamflowItem).setOnMenuItemClickListener(any(MenuItem.OnMenuItemClickListener.class));
        verify(gaugeHeightItem).setOnMenuItemClickListener(any(MenuItem.OnMenuItemClickListener.class));

        assertThat("add favorite var item is checkable", streamflowFavItem.isCheckable());
        assertThat("add favorite var item is not checked if it is not a favorite", !streamflowFavItem.isChecked());

        assertThat("add favorite var item is checkable", gaugeHeightFavItem.isCheckable());
        assertThat("add favorite var item is not checked if it is not a favorite", !gaugeHeightFavItem.isChecked());

    }

    @Test
    public void shouldAllowSiteVarToBeViewedFromContextMenu() throws Throwable {

        Map<SiteId, SiteData> mockData = new HashMap<SiteId, SiteData>();
        SiteData clearCreekData = SiteDataFactory.getClearCreekData();
        mockData.put(clearCreekData.getSite().getSiteId(), clearCreekData);
        SiteData fountainCreekData = SiteDataFactory.getFountainCreekData();
        mockData.put(fountainCreekData.getSite().getSiteId(), fountainCreekData);
        when(wsClient.dsControllerMock.getSiteData(USState.CO, false)).thenReturn(mockData);

        ArrayList<DestinationFacet> pageElements = new ArrayList<DestinationFacet>();
        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        pageElements.add(clearCreekKayak);
        Page<DestinationFacet> mockResults = new Page<DestinationFacet>(pageElements, 1);

        when(wsClient.destinationFacetsMock.get(any(WsSession.class), anyMap(), isNull(Integer.class), isNull(Integer.class))).thenReturn(mockResults);

        Intent i = new Intent(RuntimeEnvironment.application, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.CO);

        RiverSelect activity = createRiverSelect(i);

        ContextMenu mockMenu = mock(ContextMenu.class);

        SubMenu mockAddFavSubMenu = mock(SubMenu.class);

        when(mockMenu.addSubMenu(eq(ContextMenu.NONE), anyInt(), anyInt(), anyString())).thenReturn(mockAddFavSubMenu);

        int mi_id = 0;
        when(mockMenu.add(eq(ContextMenu.NONE), eq(0), anyInt(), anyString())).thenReturn(new RoboMenuItem(mi_id++));

        RoboMenuItem gaugeHeightItem = new RoboMenuItem(mi_id++);
        when(mockMenu.add(eq(ContextMenu.NONE), eq(1), eq(1), eq("Gauge Height, ft"))).thenReturn(gaugeHeightItem);

        when(mockAddFavSubMenu.add(eq(ContextMenu.NONE), anyInt(), anyInt(), anyString())).thenReturn(new RoboMenuItem(mi_id++));

        activity.onCreateContextMenu(mockMenu, activity.getListView(), new AdapterView.AdapterContextMenuInfo(activity.getListView(), 0, 0));

        gaugeHeightItem.click();

        //should go to ViewSite screen
        Intent expectedIntent = new Intent(activity, ViewSite.class);
        expectedIntent.putExtra(ViewSite.KEY_SITE, clearCreekData.getSite());
        expectedIntent.putExtra(ViewSite.KEY_VARIABLE, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT);
        assertThat(shadowOf(activity).getNextStartedActivity(), equalTo(expectedIntent));

    }

    @Test
    public void shouldAllowDestinationCreationWhenLoggedIn() throws Throwable {

        Map<SiteId, SiteData> mockData = new HashMap<SiteId, SiteData>();
        SiteData apalachicolaData = SiteDataFactory.getApalachicolaData();
        mockData.put(apalachicolaData.getSite().getSiteId(), apalachicolaData);
        when(wsClient.dsControllerMock.getSiteData(USState.FL, false)).thenReturn(mockData);

        when(wsClient.destinationFacetsMock.get(any(WsSession.class), anyMap(), isNull(Integer.class), isNull(Integer.class))).thenReturn(new Page<DestinationFacet>(new ArrayList<DestinationFacet>(), 0));

        Intent i = new Intent(RuntimeEnvironment.application, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.FL);

        RiverSelect activity = createRiverSelect(i);

        ContextMenu mockMenu = mock(ContextMenu.class);

        SubMenu mockAddDestSubMenu = mock(SubMenu.class);

        when(mockMenu.addSubMenu(eq(ContextMenu.NONE), eq(1), eq(1), eq("Create Destination"))).thenReturn(mockAddDestSubMenu);

        int mi_id = 0;
        when(mockMenu.add(eq(ContextMenu.NONE), anyInt(), anyInt(), anyString())).thenReturn(new RoboMenuItem(mi_id++));

        RoboMenuItem gaugeHeightDestItem = new RoboMenuItem(mi_id++);
        when(mockAddDestSubMenu.add(eq(ContextMenu.NONE), eq(0), eq(0), eq("Gauge Height, ft"))).thenReturn(gaugeHeightDestItem);

        activity.onCreateContextMenu(mockMenu, activity.getListView(), new AdapterView.AdapterContextMenuInfo(activity.getListView(), 0, 0));

        gaugeHeightDestItem.click();//create destination

        //should go to the EditDestination screen
        Intent expectedIntent = new Intent(activity, EditDestination.class);
        expectedIntent.putExtra(EditDestination.KEY_SITE, apalachicolaData.getSite());
        expectedIntent.putExtra(EditDestination.KEY_VARIABLE, UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT);
        assertThat(shadowOf(activity).getNextStartedActivity(), equalTo(expectedIntent));

    }

    @Test
    public void shouldAllowFavoriteCreationWhenRemoteSiteIdIsMissing() throws Throwable {

        Map<SiteId, SiteData> mockData = new HashMap<SiteId, SiteData>();
        SiteData clearCreekData = SiteDataFactory.getClearCreekData();
        mockData.put(clearCreekData.getSite().getSiteId(), clearCreekData);
        SiteData fountainCreekData = SiteDataFactory.getFountainCreekData();
        mockData.put(fountainCreekData.getSite().getSiteId(), fountainCreekData);
        when(wsClient.dsControllerMock.getSiteData(USState.CO, false)).thenReturn(mockData);

        ArrayList<DestinationFacet> pageElements = new ArrayList<DestinationFacet>();
        DestinationFacet clearCreekKayak = DestinationFacetFactory.getClearCreekKayak();
        pageElements.add(clearCreekKayak);
        Page<DestinationFacet> mockResults = new Page<DestinationFacet>(pageElements, 1);

        when(wsClient.destinationFacetsMock.get(any(WsSession.class), anyMap(), isNull(Integer.class), isNull(Integer.class))).thenReturn(mockResults);

        Intent i = new Intent(RuntimeEnvironment.application, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.CO);

        RiverSelect activity = createRiverSelect(i);

        ContextMenu mockMenu = mock(ContextMenu.class);

        Variable[] ccSupportedVars = clearCreekData.getSite().getSupportedVariables();

        SubMenu mockAddFavSubMenu = mock(SubMenu.class);

        when(mockMenu.addSubMenu(eq(ContextMenu.NONE), eq(ccSupportedVars.length), eq(ccSupportedVars.length), eq("Add To Favorites"))).thenReturn(mockAddFavSubMenu);

        int mi_id = 0;
        when(mockMenu.add(eq(ContextMenu.NONE), anyInt(), anyInt(), anyString())).thenReturn(new RoboMenuItem(mi_id++));

        when(mockAddFavSubMenu.add(eq(ContextMenu.NONE), eq(0), anyInt(), anyString())).thenReturn(new RoboMenuItem(mi_id++));

        RoboMenuItem gaugeHeightFavItem = new RoboMenuItem(mi_id++);
        when(mockAddFavSubMenu.add(eq(ContextMenu.NONE), eq(1), eq(1), eq("Gauge Height, ft"))).thenReturn(gaugeHeightFavItem);

        activity.onCreateContextMenu(mockMenu, activity.getListView(), new AdapterView.AdapterContextMenuInfo(activity.getListView(), 0, 0));

        gaugeHeightFavItem.click();//add favorite

        assertThat("created favorite", FavoritesDaoImpl.isFavorite(RuntimeEnvironment.application, clearCreekData.getSite().getSiteId(), CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT));
        assertThat("add favorite var item is checked after a favorite has been created", gaugeHeightFavItem.isChecked());

    }

    public class MapListMatcher extends BaseMatcher<Map<String,List<String>>> {
        private Map<String, List<String>> thisMap;

        public MapListMatcher(Map<String, List<String>> thisMap) {
            this.thisMap = thisMap;
        }

        @Override
        public boolean matches(Object item) {
            if(thisMap == null) {
                return item == null;
            }
            try {
                Map<String, List<String>> otherMap = (Map<String, List<String>>)item;

                if(thisMap.size() != otherMap.size()) {
                    return false;
                }

                for(Map.Entry<String,List<String>> entry : thisMap.entrySet()) {
                    if(!equalTo(entry.getValue()).matches(otherMap.get(entry.getKey()))) {
                        return false;
                    }
                }

                return true;
            } catch(ClassCastException cce) {
            }

            return false;
        }

        @Override
        public void describeTo(Description description) {
        }
    }

    @After
    public void cleanup() {
        RobolectricGradleTestRunner.resetDbSingleton();
    }
}
