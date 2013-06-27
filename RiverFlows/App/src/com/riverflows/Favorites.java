package com.riverflows;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.UserAccount;
import com.riverflows.data.ValueConverter;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.SitesDaoImpl;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.WsSessionManager;

import org.apache.http.conn.HttpHostConnectException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Favorites extends ListActivity {
	
	private static final String TAG = Home.TAG;
	
	public static final String FAVORITES_PATH = "/favorites/";

	public static final String PREF_MIGRATE_DESTINATIONS_NOTICE_SHOWN = "pref_share_favorites_notice_shown";
	
	public static final int REQUEST_EDIT_FAVORITE = 1;
	public static final int REQUEST_REORDER_FAVORITES = 2;
	public static final int REQUEST_CREATE_ACCOUNT = 83247;

	public static final int DIALOG_ID_LOADING = 1;
	public static final int DIALOG_ID_LOADING_ERROR = 2;
	public static final int DIALOG_ID_MASTER_LOADING = 3;
	public static final int DIALOG_ID_MASTER_LOADING_ERROR = 4;
	public static final int DIALOG_ID_UPGRADE_FAVORITES = 5;
	public static final int DIALOG_ID_SIGNING_IN = 6;
	
	LoadSitesTask loadTask = null;

	private SignIn signin;

	private String tempUnit = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.favorites);

		ListView lv = getListView();
		
		hideInstructions();

		TextView favoriteSubtext = (TextView)findViewById(R.id.favorite_instructions_subheader);
		
		SpannableString subtext = new SpannableString("If you select your favorite gauge sites, they will appear here.");
		
		subtext.setSpan(new URLSpan("riverflows://help/favorites.html"), 7, 39, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		favoriteSubtext.setText(subtext);
		favoriteSubtext.setMovementMethod(LinkMovementMethod.getInstance());

		SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, MODE_PRIVATE);
    	tempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);
		
		lv.setTextFilterEnabled(true);
		this.loadTask = getLastNonConfigurationInstance();
		
		if(this.loadTask != null) {
			if(!this.loadTask.running) {
				if(this.loadTask.gauges == null || this.loadTask.errorMsg != null) {
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
		EasyTracker.getInstance().activityStart(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//discard the cached list items after 2 hours
		if((System.currentTimeMillis() - this.loadTask.loadTime.getTime()) > (2 * 60 * 60 * 1000)) {
			loadSites(true);
			return;
		}
		
		//make sure list of favorites hasn't been modified
//		LoadSitesTask currentLoadTask = this.loadTask;
//		if(currentLoadTask.running) {
			//not much we can do here- modifying the task's results will just cause thread contention problems
//			return;
//		}
		
		List<Favorite> currentFavs = FavoritesDaoImpl.getFavorites(this, null, null);
		
		if(!this.loadTask.running &&
				(this.loadTask.favorites == null || (currentFavs.size() != this.loadTask.favorites.size()))) {
			//a favorite has been removed- reload the list
			// TODO it would be snappier if we just deleted item(s) from the currently displayed list
			loadSites(false);
			return;
		}
		
		//there's still a possibility that the favorites list is the same length because
		// a favorite was added while another was deleted
		for(Favorite currentFav: currentFavs) {
			if(currentFav.getCreationDate().after(getLastLoadTime())) {
				//a favorite has been added- reload the list
				loadSites(false);
				return;
			}
		}
		
		//temperature conversion has changed
		SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, MODE_PRIVATE);
    	String newTempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);
    	
    	if(newTempUnit != tempUnit && (newTempUnit == null || !newTempUnit.equals(tempUnit))) {
			tempUnit = newTempUnit;
			loadSites(false);
			return;
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
	    case R.id.mi_about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
	        return true;
	    case R.id.mi_reload:
	    	loadSites(true);
	    	return true;
	    case R.id.mi_reorder:
	    	Intent i_reorder = new Intent(this, ReorderFavorites.class);
	    	startActivityForResult(i_reorder, REQUEST_REORDER_FAVORITES);
	    	return true;
	    case R.id.mi_help:
	    	Intent i_help = new Intent(Intent.ACTION_VIEW, Uri.parse(Help.BASE_URI + "favorites.html"));
	    	startActivity(i_help);
	    	return true;
		case R.id.mi_sign_in:
			signin = new SignIn();
			signin.execute();
			return true;
		case R.id.mi_sign_out:
			WsSessionManager.logOut(this);
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
		case DIALOG_ID_SIGNING_IN:
			ProgressDialog signingInDialog = new ProgressDialog(this);
			signingInDialog.setMessage("Signing In...");
			signingInDialog.setIndeterminate(true);
			signingInDialog.setCancelable(true);
			return signingInDialog;
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
			this.loadTask.cancel(true);
		}
		
		getListView().getEmptyView().setVisibility(View.INVISIBLE);
		
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

//			if(this.loadTask.gauges.size() > 0) {
//				SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, MODE_PRIVATE);
//				boolean showDestinationsNotice = !settings.getBoolean(PREF_MIGRATE_DESTINATIONS_NOTICE_SHOWN, false);
//
//				if (showDestinationsNotice) {
//					startActivity(new Intent(this, MigrateToDestinations.class));
//				}
//			}

			setListAdapter(new SiteAdapter(getApplicationContext(), this.loadTask.gauges));
			registerForContextMenu(getListView());
		}
		try {
			removeDialog(DIALOG_ID_LOADING);
		} catch(IllegalArgumentException iae) {
			if(Log.isLoggable(TAG, Log.INFO)) {
				Log.i(TAG, "can't remove dialog; activity no longer active");
			}
		}
		if(this.loadTask.gauges == null || this.loadTask.errorMsg != null) {
			try {
				showDialog(DIALOG_ID_LOADING_ERROR);
			} catch(BadTokenException bte) {
				if(Log.isLoggable(TAG, Log.INFO)) {
					Log.i(TAG, "can't display dialog; activity no longer active");
				}
			}
		} else if(this.loadTask.gauges.size() == 0) {
			showInstructions();
		}

		//needed for Android 3.0+
		//invalidateOptionsMenu();
	}
	
	private void showInstructions() {
		getListView().getEmptyView().setVisibility(View.VISIBLE);
		
		/*
		WebView instructions = (WebView)findViewById(R.id.favorite_instructions);
        instructions.setClickable(false);
		instructions.loadUrl("file:///android_asset/" + Help.DATA_PATH_PREFIX + "/favorites.html");
		*/
	}

	private void hideInstructions() {
		getListView().getEmptyView().setVisibility(View.INVISIBLE);
	}

	/** parameter for LoadSitesTask */
	public static final Integer HARD_REFRESH = new Integer(1);
	
	public class LoadSitesTask extends AsyncTask<Integer, Integer, List<SiteData>> {
		
		protected static final int STATUS_UPGRADING_FAVORITES = -1;

		public final Date loadTime = new Date();
		public List<SiteData> gauges = null;
		public List<Favorite> favorites = null;
		
		public boolean running = false;
		
		public String errorMsg = null;
		
		protected void setLoadErrorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
		}
		
		@Override
		protected List<SiteData> doInBackground(Integer... params) {
			running = true;
			try {
				this.favorites = FavoritesDaoImpl.getFavorites(getApplicationContext(), null, null);
				
				if(favorites.size() == 0) {
					return Collections.emptyList();
				}
				
				//migrate any favorites without a variable set
				checkForOldFavorites(favorites);
				

				boolean hardRefresh = (params.length > 0 && params[0].equals(HARD_REFRESH));
				
				Map<SiteId,SiteData> allSiteDataMap = new HashMap<SiteId,SiteData>();
				
				Map<SiteId,SiteData> siteDataMap = DataSourceController.getSiteData(favorites, hardRefresh);

		    	Map<CommonVariable, CommonVariable> unitConversionMap = CommonVariable.temperatureConversionMap(tempUnit);
				
				for(SiteData currentData: siteDataMap.values()) {
					
					//convert °C to °F if that setting is enabled
					Map<CommonVariable,Series> datasets = currentData.getDatasets();
					for(Series dataset: datasets.values()) {
						ValueConverter.convertIfNecessary(unitConversionMap, dataset);
					}
					
					allSiteDataMap.put(currentData.getSite().getSiteId(), currentData);
				}
				
				return expandDatasets(favorites, allSiteDataMap);
			} catch (UnknownHostException uhe) {
				setLoadErrorMsg("no network access");
			} catch(Exception e) {
				setLoadErrorMsg(e.getMessage());
				Log.e(getClass().getName(), "",e);
				EasyTracker.getTracker().sendException("getFavorites", e, false);
			}
			return null;
		}

		//TODO need a datatype that contains both the Favorite and SiteData so this is no longer necessary
		// this code is cut-n-pasted from the Favorites activity to the Favorites content provider
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

				//use custom name if one is defined
				if(favorite.getName() != null) {
					current.getSite().setName(favorite.getName());
				}
				
				if(current.getDatasets().size() <= 1) {
					expandedDatasets.add(current);
					continue;
				}
				
				//use the dataset for this favorite's variable
				Series dataset = current.getDatasets().get(favoriteVar.getCommonVariable());
				if(dataset == null) {
					//if no dataset is found, create one so we know which variable is associated with the favorite
					dataset = new Series();
					dataset.setReadings(new ArrayList<Reading>(0));
					dataset.setVariable(favoriteVar);
				}
				
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
								
								EasyTracker.getTracker().sendException("checkForOldFavorites", ioe, true);
								
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
			if(result != null) {
				this.gauges = result;
			}
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
					try {
						removeDialog(DIALOG_ID_LOADING);
						showDialog(DIALOG_ID_UPGRADE_FAVORITES);
					} catch(BadTokenException bte) {
						if(Log.isLoggable(TAG, Log.INFO)) {
							Log.i(TAG, "can't display dialog; activity no longer active");
						}
					} catch(IllegalArgumentException iae) {
						if(Log.isLoggable(TAG, Log.INFO)) {
							Log.i(TAG, "can't remove dialog; activity no longer active");
						}
					}
				}
				if(values[1] == 100) {
					try {
						removeDialog(DIALOG_ID_UPGRADE_FAVORITES);
						showDialog(DIALOG_ID_LOADING);
					} catch(BadTokenException bte) {
						if(Log.isLoggable(TAG, Log.INFO)) {
							Log.i(TAG, "can't display dialog; activity no longer active");
						}
					} catch(IllegalArgumentException iae) {
						if(Log.isLoggable(TAG, Log.INFO)) {
							Log.i(TAG, "can't remove dialog; activity no longer active");
						}
					}
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
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(loadTask != null && loadTask.gauges != null && loadTask.gauges.size() > 0) {
			MenuItem reorderFavorites = menu.findItem(R.id.mi_reorder);
			reorderFavorites.setVisible(true);
		}

		MenuItem signin = menu.findItem(R.id.mi_sign_in);
		MenuItem signout = menu.findItem(R.id.mi_sign_out);

		if(WsSessionManager.getSession() != null) {
			signin.setVisible(false);
			signout.setVisible(true);
		} else {
			signin.setVisible(true);
			signout.setVisible(false);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.favorites_menu, menu);
	    
	    return true;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		SiteAdapter adapter = (SiteAdapter)((ListView)v).getAdapter();
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

		SiteData siteData = adapter.getItem(info.position);
		
		if(siteData == null) {
			return;
		}
		
		Series data = DataSourceController.getPreferredSeries(siteData);
		if(data == null) {
			return;
		}
		Variable variable = data.getVariable();
		
		MenuItem view = menu.add("View");
		view.setOnMenuItemClickListener(new ViewFavoriteListener(siteData.getSite(), variable));
		
		MenuItem edit = menu.add("Edit");
		edit.setOnMenuItemClickListener(new EditFavoriteListener(siteData.getSite(), variable));
		
		MenuItem delete = menu.add("Delete");
		delete.setOnMenuItemClickListener(new DeleteFavoriteListener(siteData.getSite(), variable));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == Home.REQUEST_CHOOSE_ACCOUNT || requestCode == Home.REQUEST_HANDLE_RECOVERABLE_AUTH_EXC) {
			signin.authorizeCallback(requestCode, resultCode, data);
		}
		
		//update the view, if necessary
		
		switch(requestCode) {
		case REQUEST_EDIT_FAVORITE:
			if(resultCode == RESULT_OK) {
				sendBroadcast(Home.getWidgetUpdateIntent());
				
	        	if(loadTask == null || loadTask.favorites == null) {
	        		//favorites haven't even loaded yet- return
	        		return;
	        	}
	        	
	        	if(data == null) {
	        		//nothing was changed
	        		return;
	        	}
	        	
	        	String intentPath = data.getData().getSchemeSpecificPart();
	        	
        		int favoriteId = -1;
	        	
	        	try {
	        		favoriteId = Integer.parseInt(intentPath.substring(FAVORITES_PATH.length(), intentPath.length()));
	        	} catch(Exception e) {
	        		Log.e(TAG, "could not find favorite ID", e);
    				loadSites(false);
	        		return;
	        	}
	        	
	        	try {
		        	for(Favorite oldFavorite: loadTask.favorites) {
		        		if(oldFavorite.getId() == favoriteId) {
		        			Favorite newFavorite = FavoritesDaoImpl.getFavorite(this, favoriteId);
		        			
		        			if(!oldFavorite.getVariable().equals(newFavorite.getVariable())) {
		        				//variable has been changed- we need to reload
		        				// TODO for certain datasources, this shouldn't be necessary because data for all variables
		        				//  is retrieved along with the variable associated with the favorite.  However it will be
		        				//  difficult to make use of that extra data until this activity uses Favorite rather than SiteData
		        				//  objects as the core of its model.
		        				loadSites(false);
		        				return;
		        			}
		        			
		        			String newName = newFavorite.getName();
		        			
		        			for(SiteData favoriteData: loadTask.gauges) {
		        				if(favoriteData.getSite().getSiteId().equals(newFavorite.getSite().getSiteId())) {
		        					Variable var = DataSourceController.getVariable(newFavorite.getSite().getAgency(), newFavorite.getVariable());
		        					
		        					if(favoriteData.getDatasets().get(var.getCommonVariable()) != null) {
		        						if(newName == null) {
		        							//revert to original name of the site
		        							newName = newFavorite.getSite().getName();
		        						}
		        						favoriteData.getSite().setName(newName);
		        						((SiteAdapter)getListAdapter()).notifyDataSetChanged();
		    		        			
		    		        			oldFavorite.setName(newName);
		    		        			return;
		        					}
		        				}
		        			}
		        		}
		        	}
	        	} catch(Exception e) {
	        		Log.e(TAG, "error updating favorite", e);
    				loadSites(false);
	        	}
			}
    		return;
		case REQUEST_REORDER_FAVORITES:
			if(resultCode == RESULT_OK) {
				sendBroadcast(Home.getWidgetUpdateIntent());
				
				if(loadTask == null || loadTask.gauges == null) {
	        		//favorites haven't even loaded yet- return
	        		return;
	        	}
				
				List<Favorite> newFavorites = FavoritesDaoImpl.getFavorites(this, null, null);
				
				List<SiteData> newSiteData = new ArrayList<SiteData>(newFavorites.size());
				
				//reorder loadTask.gauges based on the new favorites order
				for(Favorite newFav: newFavorites) {
					for(SiteData siteData: loadTask.gauges) {
						if(siteData.getSite().getSiteId().equals(newFav.getSite().getSiteId())) {
        					Variable var = DataSourceController.getVariable(newFav.getSite().getAgency(), newFav.getVariable());
        					
        					if(siteData.getDatasets().get(var.getCommonVariable()) != null) {
        						newSiteData.add(siteData);
        						//we can get away with this without a ConcurrentModificationException
        						// because it is immediately followed by a break statement
        						loadTask.gauges.remove(siteData);
        						break;
        					}
        				}
					}
				}
				
				loadTask.favorites = newFavorites;
				loadTask.gauges.addAll(newSiteData);
				((SiteAdapter)getListAdapter()).notifyDataSetChanged();
			}
		}
	}
	
	private class ViewFavoriteListener implements MenuItem.OnMenuItemClickListener {
		
		private Site site;
		private Variable variable;
		
		public ViewFavoriteListener(Site site, Variable variable) {
			super();
			this.site = site;
			this.variable = variable;
		}

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			Intent i = new Intent(Favorites.this, ViewChart.class);
	        i.putExtra(ViewChart.KEY_SITE, site);
	        i.putExtra(ViewChart.KEY_VARIABLE, variable);
	        startActivity(i);
			return true;
		}
	};
	
	private class EditFavoriteListener implements MenuItem.OnMenuItemClickListener {
		
		private Site site;
		private Variable variable;
		
		public EditFavoriteListener(Site site, Variable variable) {
			super();
			this.site = site;
			this.variable = variable;
		}

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			
			Intent i = new Intent(Favorites.this, EditFavorite.class);

	        i.putExtra(EditFavorite.KEY_SITE_ID, site.getSiteId());
	        i.putExtra(EditFavorite.KEY_VARIABLE_ID, variable.getId());
	        startActivityForResult(i, REQUEST_EDIT_FAVORITE);
			return true;
		}
	}
	
	private class DeleteFavoriteListener implements MenuItem.OnMenuItemClickListener {
		
		private Site site;
		private Variable variable;
		
		public DeleteFavoriteListener(Site site, Variable variable) {
			super();
			this.site = site;
			this.variable = variable;
		}

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			
			FavoritesDaoImpl.deleteFavorite(Favorites.this, site.getSiteId(), variable);
			
			LoadSitesTask loadTask = Favorites.this.loadTask;
			
			if(loadTask == null) {
				return true;
			}
			
			if(loadTask.running) {
				//cancel and reload
				loadSites(false);
				
				return true;
			}
			
			Iterator<Favorite> favoritesI = loadTask.favorites.iterator();
			
			while(favoritesI.hasNext()) {
				Favorite fav = favoritesI.next();
				if(!fav.getSite().getSiteId().equals(site.getSiteId())) {
					continue;
				}
				
				if(fav.getVariable().equals(variable.getId())) {
					favoritesI.remove();
					break;
				}
			}
			
			Iterator<SiteData> gaugesI = loadTask.gauges.iterator();
			
			while(gaugesI.hasNext()) {
				SiteData gauge = gaugesI.next();
				if(!gauge.getSite().getSiteId().equals(site.getSiteId())) {
					continue;
				}

				if(gauge.getDatasets().containsKey(variable.getCommonVariable())) {
					gaugesI.remove();
					break;
				}
			}
			
			//there's still an unlikely possibility that Favorites.this.loadTask has been changed
			displayFavorites();
			
			return true;
		}
	}

	private class SignIn extends ApiCallTask<String, Integer, UserAccount> {
		public SignIn(){
			super(Favorites.this, Home.REQUEST_CHOOSE_ACCOUNT, Home.REQUEST_HANDLE_RECOVERABLE_AUTH_EXC, true, false);
			showDialog(DIALOG_ID_SIGNING_IN);
		}

		public SignIn(SignIn oldTask) {
			super(oldTask);
			showDialog(DIALOG_ID_SIGNING_IN);
		}

		@Override
		protected UserAccount doApiCall(WsSessionManager.Session session, String... params) {
			return session.userAccount;
		}

		@Override
		protected void onNoUIRequired(UserAccount userAccount) {

			removeDialog(DIALOG_ID_SIGNING_IN);

			if(exception != null) {
				Log.e(Home.TAG, "", exception);
			}

			if(userAccount == null) {
				return;
			}

			if(userAccount.getFacetTypes() == 0) {
				//set up this user's account
				startActivityForResult(new Intent(Favorites.this, AccountSettings.class), REQUEST_CREATE_ACCOUNT);
			}
		}

		@Override
		protected ApiCallTask<String, Integer, UserAccount> clone() throws CloneNotSupportedException {
			return new SignIn(this);
		}
	}
}
