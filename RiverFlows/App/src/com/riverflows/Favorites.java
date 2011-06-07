package com.riverflows;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.HttpHostConnectException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.ListView;

import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.SitesDaoImpl;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UsgsCsvDataSource;

public class Favorites extends ListActivity {
	
	private static final String TAG = Favorites.class.getSimpleName();
	
	public static final int DIALOG_ID_LOADING = 1;
	public static final int DIALOG_ID_LOADING_ERROR = 2;
	public static final int DIALOG_ID_MASTER_LOADING = 3;
	public static final int DIALOG_ID_MASTER_LOADING_ERROR = 4;
	public static final int DIALOG_ID_UPGRADE_FAVORITES = 5;
	
	LoadSitesTask loadTask = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		this.loadTask = getLastNonConfigurationInstance();
		
		if(this.loadTask != null) {
			if(!this.loadTask.running) {
				if(this.loadTask.gauges == null) {
					loadSites(false);
				} else {
					displayFavorites();
				}
			} else {
				//if the loadTask is running, just wait until it finishes
				showDialog(DIALOG_ID_LOADING);
			}
		} else {
			loadSites(false);
		}
		
		setTitle("Favorites");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		//discard the cached list items after 2 hours
		if((System.currentTimeMillis() - this.loadTask.loadTime.getTime()) > (2 * 60 * 60 * 1000)
				|| FavoritesDaoImpl.hasNewFavorites(getApplicationContext(), getLastLoadTime())) {
			loadSites(true);
		}
	}
	
	@Override
    public LoadSitesTask onRetainNonConfigurationInstance() {
        return this.loadTask;
    }
    
    @Override
    public LoadSitesTask getLastNonConfigurationInstance() {
    	return (LoadSitesTask)super.getLastNonConfigurationInstance();
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, ViewChart.class);
		Site selectedStation = null;
		Variable selectedVariable = null;
		
		if(this.loadTask == null || this.loadTask.gauges == null) {
			return;
		}
		
		for(SiteData currentData: this.loadTask.gauges) {
			if(SiteAdapter.getItemId(currentData) == id){
				selectedStation = currentData.getSite();
				
				Series data = DataSourceController.getPreferredSeries(currentData);
				if(data != null) {
					selectedVariable = data.getVariable();
				}
				break;
			}
		}
		
		if(selectedStation == null) {
			Log.w(TAG,"no such station: " + id);
			return;
		}
		
        i.putExtra(ViewChart.KEY_SITE, selectedStation);
        i.putExtra(ViewChart.KEY_VARIABLE, selectedVariable);
        startActivity(i);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mi_home:
	    	startActivityIfNeeded(new Intent(this, Home.class), -1);
	    	return true;
	    case R.id.mi_about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
	        return true;
	    case R.id.mi_reload:
	    	loadSites(true);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_ID_LOADING:
			ProgressDialog dialog = new ProgressDialog(this);
	        dialog.setMessage("Loading Sites...");
	        dialog.setIndeterminate(true);
	        dialog.setCancelable(true);
	        return dialog;
		case DIALOG_ID_LOADING_ERROR:
			ErrorMsgDialog errorDialog = new ErrorMsgDialog(this, loadTask.errorMsg);
			return errorDialog;
		case DIALOG_ID_MASTER_LOADING:
			ProgressDialog masterDialog = new ProgressDialog(this);
			masterDialog.setMessage("Downloading Master Site List...");
			masterDialog.setIndeterminate(true);
			masterDialog.setCancelable(true);
	        return masterDialog;
		case DIALOG_ID_MASTER_LOADING_ERROR:
			ErrorMsgDialog masterErrorDialog = new ErrorMsgDialog(this, loadTask.errorMsg);
			return masterErrorDialog;
		case DIALOG_ID_UPGRADE_FAVORITES:
			ProgressDialog favoritesDialog = new ProgressDialog(this);
			favoritesDialog.setMessage("Upgrading Favorites\nthis may take a few minutes");
			favoritesDialog.setIndeterminate(true);
			favoritesDialog.setCancelable(true);
	        return favoritesDialog;
		}
		return null;
	}
	
	private class ErrorMsgDialog extends AlertDialog {

		public ErrorMsgDialog(Context context, String msg) {
			super(context);
			setMessage(msg);
		}
		
	}
	
	/**
	 * @param hardRefresh if true, discard persisted site data as well
	 */
	public void loadSites(boolean hardRefresh) {
		showDialog(DIALOG_ID_LOADING);

		List<SiteData> currentGauges = null;
		
		if(this.loadTask != null) {
			currentGauges = this.loadTask.gauges;
		}
		
		this.loadTask = new LoadSitesTask();
		
		//preserve existing gauges in case load fails
		this.loadTask.gauges = currentGauges;
		
		if(hardRefresh) {
			this.loadTask.execute(HARD_REFRESH);
		} else {
			this.loadTask.execute();
		}
	}
	
	public void displayFavorites() {
		if(this.loadTask.gauges != null) {
			setListAdapter(new SiteAdapter(getApplicationContext(), this.loadTask.gauges));
		}
		removeDialog(DIALOG_ID_LOADING);
		if(this.loadTask.gauges == null || this.loadTask.errorMsg != null) {
			try {
				showDialog(DIALOG_ID_LOADING_ERROR);
			} catch(BadTokenException bte) {
				if(Log.isLoggable(TAG, Log.INFO)) {
					Log.i(TAG, "can't display dialog; activity no longer active");
				}
			}
		}
	}

	/** parameter for LoadSitesTask */
	public static final Integer HARD_REFRESH = new Integer(1);
	
	public class LoadSitesTask extends AsyncTask<Integer, Integer, List<SiteData>> {
		
		protected static final int STATUS_UPGRADING_FAVORITES = -1;

		public final Date loadTime = new Date();
		public List<SiteData> gauges = null;
		
		public boolean running = false;
		
		public String errorMsg = null;
		
		protected void setLoadErrorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
		}
		
		@Override
		protected List<SiteData> doInBackground(Integer... params) {
			running = true;
			try {
				List<Favorite> favorites = FavoritesDaoImpl.getFavorites(getApplicationContext());
				
				if(favorites.size() == 0) {
					return Collections.emptyList();
				}
				
				//migrate any favorites without a variable set
				checkForOldFavorites(favorites);
				

				boolean hardRefresh = (params.length > 0 && params[0].equals(HARD_REFRESH));
				
				Map<SiteId,SiteData> allSiteDataMap = new HashMap<SiteId,SiteData>();
				
				Map<SiteId,SiteData> siteDataMap = DataSourceController.getSiteData(favorites, hardRefresh);
				
				for(SiteData currentData: siteDataMap.values()) {
					allSiteDataMap.put(currentData.getSite().getSiteId(), currentData);
				}
				
				return expandDatasets(favorites, allSiteDataMap);
			} catch (UnknownHostException uhe) {
				setLoadErrorMsg("no network access");
			} catch(Exception e) {
				setLoadErrorMsg(e.getMessage());
				Log.e(getClass().getName(), "",e);
			}
			return null;
		}
		
		private List<SiteData> expandDatasets(List<Favorite> favorites, Map<SiteId,SiteData> siteDataMap) {
			ArrayList<SiteData> expandedDatasets = new ArrayList<SiteData>(favorites.size());
			
			//build a list of SiteData objects corresponding to the list of favorites
			for(Favorite favorite: favorites) {
				SiteData current = siteDataMap.get(favorite.getSite().getSiteId());
				
				Variable favoriteVar = DataSourceController.getVariable(favorite.getSite().getAgency(), favorite.getVariable());
				
				if(favoriteVar == null) {
					throw new NullPointerException("could not find variable: " + favorite.getSite().getAgency() + " " + favorite.getVariable());
				}
				
				if(current == null) {
					//failed to get data for this site- create a placeholder item
					current = new SiteData();
					current.setSite(favorite.getSite());
					
					Series nullSeries = new Series();
					nullSeries.setVariable(favoriteVar);
					
					Reading placeHolderReading = new Reading();
					placeHolderReading.setDate(new Date());
					placeHolderReading.setQualifiers("Datasource Down");
					
					nullSeries.setReadings(Collections.singletonList(placeHolderReading));
					nullSeries.setSourceUrl("");
					
					current.getDatasets().put(favoriteVar.getCommonVariable(), nullSeries);
					
					expandedDatasets.add(current);
					continue;
				}
				
				if(current.getDatasets().size() <= 1) {
					expandedDatasets.add(current);
					continue;
				}
				
				//use the dataset for this favorite's variable
				Series dataset = current.getDatasets().get(favoriteVar.getCommonVariable());
				SiteData expandedDataset = new SiteData();
				expandedDataset.setSite(current.getSite());
				expandedDataset.getDatasets().put(favoriteVar.getCommonVariable(), dataset);
				expandedDatasets.add(expandedDataset);
			}
			return expandedDatasets;
		}
		
		private void checkForOldFavorites(List<Favorite> favorites) {
			
			if(favorites.size() > 0) {
				
				HashSet<USState> favoriteStates = new HashSet<USState>();
	
				//check for favorites which still don't have a variable set
				
				Iterator<Favorite> favoritesI = favorites.iterator();
				
				boolean progressStartPublished = false;
				
				while(favoritesI.hasNext()) {
					Favorite f = favoritesI.next();
					if(f.getVariable() == null) {
						if(!progressStartPublished) {
							publishProgress(-1, 0);
							progressStartPublished = true;
						}
						
						USState favoriteState = f.getSite().getState();
						
						if(!favoriteStates.contains(favoriteState)) {
							//download the list of all sites in this favorite's state in order to get its supported variables
							try {
								Map<SiteId, SiteData> sitesMap = DataSourceController.getSiteData(favoriteState);
								List<SiteData> sites = new ArrayList<SiteData>(sitesMap.values());
								SitesDaoImpl.saveSites(getApplicationContext(), favoriteState, sites);
								favoriteStates.add(favoriteState);
							} catch(UnknownHostException ioe) {
								setLoadErrorMsg("Could not upgrade favorites list format: No network access");
								favorites.clear();
								publishProgress(-1, 100);
								return;
							} catch(HttpHostConnectException hhce) {
								setLoadErrorMsg("Could not upgrade favorites list format: Network error - please try again later");
								favorites.clear();
								publishProgress(-1, 100);
								return;
							} catch(Exception ioe) {
								//don't know what else to do- just delete the favorite
								FavoritesDaoImpl.deleteFavorite(getApplicationContext(), f.getSite().getSiteId(), null);
								favoritesI.remove();
								continue;
							}
						}
						
						//get updated set of supported variables from the sites table
						List<SiteData> siteDataList = SitesDaoImpl.getSites(getApplicationContext(), Collections.singletonList(f.getSite().getSiteId()));
						
						//delete the old favorite
						FavoritesDaoImpl.deleteFavorite(getApplicationContext(), f.getSite().getSiteId(), null);

						if(siteDataList.size() == 0) {
							//The user's sites table probably isn't fully initialized for whatever reason- hopefully it
							// will be next time this code is reached.
							Log.w(getClass().getSimpleName(), "failed to find favorite site: " + f.getSite().getId());
							favoritesI.remove();
							continue;
						}
						SiteData favoriteSiteData = siteDataList.get(0);
						
						Variable[] siteVars = favoriteSiteData.getSite().getSupportedVariables();
						
						//f.site is now stale since we've downloaded a fresh sitelist- update it
						f.setSite(favoriteSiteData.getSite());
						
						if(siteVars == null || siteVars.length == 0) {
							Log.e(getClass().getSimpleName(), "Site has no supported variables! " + favoriteSiteData.getSite().getSiteId());
							favoritesI.remove();
							continue;
						}
						
						Variable favoriteVar = null;
						
						//prefer streamflow over gauge height
						String[] oldSupportedVarIds = new String[]{UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS.getId(), UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT.getId()};
						
						//try to use one of the two variables that was supported in a previous version of riverflows
						for(int a = 0; a < oldSupportedVarIds.length; a++) {
							for(int b = 0; b < siteVars.length; b++) {
								if(siteVars[b].getId().equals(oldSupportedVarIds[a])) {
									favoriteVar = siteVars[b];
									break;
								}
							}
							if(favoriteVar != null) {
								break;
							}
						}
						
						if(favoriteVar == null) {
							//shouldn't happen, but we can just use the first supported variable
							favoriteVar = siteVars[0];
						}
						
						//set the favorite variable using the site's first supported variable,
						// save the updated favorite
						f.setVariable(favoriteVar.getId());
						FavoritesDaoImpl.createFavorite(getApplicationContext(), f);
					}
				}
				publishProgress(-1, 100);
			}
		}
		
		@Override
		protected void onPostExecute(List<SiteData> result) {
			super.onPostExecute(result);
			this.gauges = result;
			Favorites.this.displayFavorites();
			running = false;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			if(values == null || values.length == 0) {
				return;
			}
			if(values.length == 2 && values[0] == STATUS_UPGRADING_FAVORITES) {
				if(values[1] == 0) {
					removeDialog(DIALOG_ID_LOADING);
					showDialog(DIALOG_ID_UPGRADE_FAVORITES);
				}
				if(values[1] == 100) {
					removeDialog(DIALOG_ID_UPGRADE_FAVORITES);
					showDialog(DIALOG_ID_LOADING);
				}
			}
		}
	}
	
	protected Date getLastLoadTime() {
		if(this.loadTask == null) {
			return null;
		}
		
		return this.loadTask.loadTime;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.standard_menu, menu);
	    
	    //disable home menu item, which is not needed here
	    menu.findItem(R.id.mi_home).setVisible(false);
	    return true;
	}
}
