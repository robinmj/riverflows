package com.riverflows;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.db.SitesDaoImpl;
import com.riverflows.wsclient.DataSourceController;

public class RiverSelect extends SiteList {
	
	private static final String TAG = RiverSelect.class.getSimpleName();
	
	public static final String KEY_STATE = "state";

	private USState state = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		Bundle extras = getIntent().getExtras();
		
		if(extras == null)
			return;
		state = (USState)extras.get(KEY_STATE);
		
		if(state != null) {
			setTitle(state.getText() + " Gauge Sites");
		}
		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected LoadSitesTask createLoadStationsTask() {
		return new LoadSitesTask() {
			
			@Override
			protected List<SiteData> doInBackground(Integer... params) {
				boolean hardRefresh = false;
				
				if(params.length > 0) {
					if(params[0].equals(HARD_REFRESH)) {
						hardRefresh = true;
					}
				}
				
				//2 weeks ago
				Date staleDate = new Date(System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000));
				
				List<SiteData> sites = SitesDaoImpl.getSitesInState(getApplicationContext(), state, staleDate);
				
				boolean reloadRecentReadings = false;
				
				if(hardRefresh || sites == null || sites.size() == 0) {
					reloadRecentReadings = true;
				} else {
//					for(int a = 0; a < sites.size(); a++) {
//						if(SitesDaoImpl.hasStaleReading(sites.get(a))) {
//							reloadRecentReadings = true;
//							break;
//						}
//					}
				}
				
				if(reloadRecentReadings) {
					
					//cache miss or hard refresh
			        //TODO toast notification of each site loaded?
					try {
						Map<SiteId,SiteData> siteDataMap = DataSourceController.getSiteData(state);
						
						sites = new ArrayList<SiteData>(siteDataMap.values());
						
						long startTime = System.currentTimeMillis();
						Collections.sort(sites, SiteData.SORT_BY_SITE_NAME);
				        if(Log.isLoggable(TAG, Log.VERBOSE)) {
				        	Log.v(TAG, "sorted in " + (System.currentTimeMillis() - startTime));
				        }
					} catch (UnknownHostException uhe) {
						setLoadErrorMsg("no network access");
						if(sites != null && sites.size() > 0) {
							//re-use cached site info
							return sites;
						}
						return null;
					} catch(Exception e) {
						setLoadErrorMsg(e.getMessage());
						Log.e(TAG, "",e);
						return null;
					}
					
					try {
						SitesDaoImpl.saveSites(getApplicationContext(), state, sites);
					} catch(Exception e) {
						Log.e(TAG, "failed to cache sites for state: " + state, e);
					}
				}
		        
		        return sites;
			}
			
			@Override
			protected void onPostExecute(List<SiteData> result) {
				super.onPostExecute(result);

				if(result != null ) {
					Toast.makeText(getApplicationContext(),  R.string.list_filter_tip, Toast.LENGTH_LONG).show();
				}
			}
		};
	}
}
