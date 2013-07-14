package com.riverflows;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import com.riverflows.data.ValueConverter;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.DataSourceController;

public class Favorites extends ListActivity {
	
	private static final String TAG = Home.TAG;
	
	public static final String FAVORITES_PATH = "/favorites/";
	
	public static final int REQUEST_EDIT_FAVORITE = 1;
	public static final int REQUEST_REORDER_FAVORITES = 2;
	
	public static final int DIALOG_ID_LOADING = 1;
	public static final int DIALOG_ID_LOADING_ERROR = 2;
	public static final int DIALOG_ID_MASTER_LOADING = 3;
	public static final int DIALOG_ID_MASTER_LOADING_ERROR = 4;
	public static final int DIALOG_ID_UPGRADE_FAVORITES = 5;
	
	LoadSitesTask loadTask = null;
	
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
                    Site s = current.getSite();

                    //modify a copy of the site since it may be used in more than one place
                    Site customNamedSite = new Site(s.getSiteId(), favorite.getName(), s.getLongitude(), s.getLatitude(), s.getState(), s.getSupportedVariables());

					current.setSite(customNamedSite);
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
}
