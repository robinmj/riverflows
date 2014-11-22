package com.riverflows;

import android.content.Intent;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Page;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.factory.DestinationFacetFactory;
import com.riverflows.factory.SiteDataFactory;
import com.riverflows.factory.WsSessionFactory;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;

/**
 * Created by robin on 11/18/14.
 */
@RunWith(RobolectricTestRunner.class)
public class RiverSelectTest {

    MockWsClient wsClient = new MockWsClient();

    @Before
    public void setup() {
        RoboGuice.overrideApplicationInjector(Robolectric.application, wsClient);
    }


    private RiverSelect createRiverSelect(Intent i) throws Exception {
        ActivityController<RiverSelect> activityController= Robolectric.buildActivity(RiverSelect.class);

        activityController.withIntent(i).create().start().resume().visible();

        RiverSelect activity = activityController.get();

        return activity;
    }

    @Test
    public void shouldLoadSitesIfNotLoggedIn() throws Exception {
        WsSessionManager.setSession(null);

        Map<SiteId, SiteData> mockData = new HashMap<SiteId, SiteData>();

        SiteData clearCreekData = SiteDataFactory.getClearCreekData();

        mockData.put(clearCreekData.getSite().getSiteId(), clearCreekData);

        SiteData fountainCreekData = SiteDataFactory.getFountainCreekData();

        mockData.put(fountainCreekData.getSite().getSiteId(), fountainCreekData);

        when(wsClient.dsControllerMock.getSiteData(USState.CO, false)).thenReturn(mockData);

        Intent i = new Intent(Robolectric.application, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.CO);

        RiverSelect activity = createRiverSelect(i);

        assertThat(activity.getListView().getAdapter().getCount(), equalTo(2));
    }

    @Test
    public void shouldLoadSitesAndDestinations() throws Exception {

        WsSessionManager.setSession(WsSessionFactory.getRobinSession());

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

        Intent i = new Intent(Robolectric.application, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.CO);

        RiverSelect activity = createRiverSelect(i);

        MapItemAdapter mapItemAdapter = (MapItemAdapter)activity.getListView().getAdapter();

        assertThat(mapItemAdapter.getCount(), equalTo(3));
        assertThat(mapItemAdapter.getItem(0).getSite().getName(), equalTo(clearCreekData.getSite().getName()));
        assertThat(mapItemAdapter.getItem(1).destinationFacet.getDestination().getName(), equalTo(clearCreekKayak.getDestination().getName()));
        assertThat(mapItemAdapter.getItem(2).getSite().getName(), equalTo(fountainCreekData.getSite().getName()));
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
}
