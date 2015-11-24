package com.riverflows;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.HttpHostConnectException;

import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.inject.Inject;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.MapItem;
import com.riverflows.data.Page;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.DestinationFacets;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import roboguice.RoboGuice;

public class RiverSelect extends MapItemList {

    @Inject
    private DataSourceController dataSourceController;

    @Inject
    private DestinationFacets destinationFacets;

	private static final String TAG = Home.TAG;
	
	public static final String KEY_STATE = "state";

	private USState state = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        RoboGuice.getInjector(RiverSelect.this).injectMembers(this);

		Bundle extras = getIntent().getExtras();
		
		if(extras == null)
			return;

		state = (USState)extras.get(KEY_STATE);
		
		if(state != null) {
            getActionBar().setTitle(state.getText() + " Gauge Sites");
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		EasyTracker.getInstance().activityStart(this);
		EasyTracker.getTracker().setCustomDimension(1, "" + state);
	}
	
	@Override
	protected void onStop() {
		super.onStart();
		
		EasyTracker.getInstance().activityStop(this);
	}
	
	@Override
	protected LoadSitesTask createLoadStationsTask() {
		return new LoadSitesTask() {
			
			@Override
			protected List<MapItem> doInBackground(Integer... params) {
				boolean hardRefresh = false;
				
				if(params.length > 0) {
					if(params[0].equals(HARD_REFRESH)) {
						hardRefresh = true;
					}
				}

				ArrayList<MapItem> items = null;

                List<Favorite> favorites = FavoritesDaoImpl.getFavorites(RiverSelect.this, null, null);

                //calling FavoritesDaoImpl.isFavorite() for every MapItem is too slow, so check against
                // HashSets instead
                HashSet<Integer> favoriteDestinationFacetIds = new HashSet<Integer>(favorites.size());
                HashSet<SiteId> favoriteSiteIds = new HashSet<SiteId>(favorites.size());

                for(Favorite favorite : favorites) {
                    if(favorite.getDestinationFacet() == null) {
                        favoriteSiteIds.add(favorite.getSite().getSiteId());
                    } else {
                        favoriteDestinationFacetIds.add(favorite.getDestinationFacet().getId());
                    }
                }

				//cache miss or hard refresh
				//TODO toast notification of each site loaded?
				try {
					Map<SiteId,SiteData> siteDataMap = RiverSelect.this.dataSourceController.getSiteData(state, hardRefresh);

					items = new ArrayList<MapItem>(siteDataMap.size());

                    for(SiteData data : siteDataMap.values()) {
                        items.add(new MapItem(data, null, favoriteSiteIds.contains(data.getSite().getSiteId())));
                    }

                    WsSession session = RiverSelect.this.wsSessionManager.getSession(RiverSelect.this);

                    if(session != null) {
                        HashMap<String, List<String>> filterParams = new HashMap<String, List<String>>();
                        filterParams.put("state", Collections.singletonList(RiverSelect.this.state.getAbbrev()));
                        filterParams.put("facet_types", Collections.singletonList(
                                Integer.toString(
                                        ((App)getApplication()).getCurrentFacetTypes(session.userAccount))));

                        Page<DestinationFacet> destinations = RiverSelect.this.destinationFacets.get(session, filterParams, null, null);

                        for(DestinationFacet destination : destinations) {
                            items.add(new MapItem(destination, favoriteDestinationFacetIds.contains(destination.getId())));
                        }
                    }

					long startTime = System.currentTimeMillis();
					Collections.sort(items, MapItem.SORT_BY_NAME);
					if(Log.isLoggable(TAG, Log.VERBOSE)) {
						Log.v(TAG, "sorted in " + (System.currentTimeMillis() - startTime));
					}
				} catch (UnknownHostException uhe) {
					setLoadErrorMsg("no network access");
					if(items != null && items.size() > 0) {
						//re-use cached site info
						return items;
					}
					return null;
				} catch(HttpHostConnectException hhce) {
					setLoadErrorMsg("could not reach RiverFlows server");
					Log.e(TAG, "",hhce);
					Crashlytics.logException(hhce);
					return null;
				} catch(SocketException se) {
					setLoadErrorMsg("could not connect to RiverFlows server");
					Log.e(TAG, "",se);
					return null;
				} catch(Exception e) {
					Crashlytics.logException(e);
					setLoadErrorMsg(e.getMessage());
					Log.e(TAG, "", e);
					return null;
				}

				if(items == null) {
					setLoadErrorMsg("Failed to load sites for unknown reason");
				}
				
		        return items;
			}
		};
	}
}
