package com.riverflows;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.UserAccount;
import com.riverflows.data.ValueConverter;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.DestinationFacets;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Favorites extends SherlockListFragment {

	private static final String TAG = Home.TAG;

	public static final String FAVORITES_PATH = "/favorites/";

	public static final String PREF_MIGRATE_DESTINATIONS_NOTICE_SHOWN = "pref_share_favorites_notice_shown";

	public static final int REQUEST_EDIT_FAVORITE = 1;
	public static final int REQUEST_REORDER_FAVORITES = 2;
	public static final int REQUEST_CREATE_ACCOUNT = 83247;
	public static final int REQUEST_GET_FAVORITES = 15319;
	public static final int REQUEST_GET_FAVORITES_RECOVER = 4193;

	LoadFavoritesTask loadTask = null;

	private SignIn signin;

	private String tempUnit = null;

	private Date v2MigrationDate = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		return inflater.inflate(R.layout.favorites, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ListView lv = getListView();

		hideInstructions();

		TextView favoriteSubtext = (TextView)getView().findViewById(R.id.favorite_instructions_subheader);

		SpannableString subtext = new SpannableString("If you select your favorite gauge sites, they will appear here.");

		subtext.setSpan(new URLSpan("riverflows://help/favorites.html"), 7, 39, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		favoriteSubtext.setText(subtext);
		favoriteSubtext.setMovementMethod(LinkMovementMethod.getInstance());

		SharedPreferences settings = getActivity().getSharedPreferences(Home.PREFS_FILE, Activity.MODE_PRIVATE);
    	tempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);
		long destinationMigrationDate = settings.getLong("destination_migration", -1);
		if(destinationMigrationDate != -1) {
			v2MigrationDate = new Date(destinationMigrationDate);
		}

		lv.setTextFilterEnabled(true);
		//this.loadTask = getLastNonConfigurationInstance();

		if(this.loadTask != null) {
			if(!this.loadTask.running) {
				if(this.loadTask.getGauges() == null || this.loadTask.getErrorMsg() != null) {
					loadSites(false);
				} else {
					displayFavorites();
				}
			} else {
				//if the loadTask is running, just wait until it finishes
				showDialog(Home.DIALOG_ID_LOADING);
			}
		} else {
			loadSites(false);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		//discard the cached list items after 2 hours
		if(this.loadTask.isStale()) {
			loadSites(true);
			return;
		}

		//make sure list of favorites hasn't been modified
//		LoadSitesTask currentLoadTask = this.loadTask;
//		if(currentLoadTask.running) {
			//not much we can do here- modifying the task's results will just cause thread contention problems
//			return;
//		}

		List<Favorite> currentFavs = FavoritesDaoImpl.getFavorites(getActivity().getApplicationContext(), null, null);

		if(!this.loadTask.isRunning() &&
				(this.loadTask.favorites == null || (currentFavs.size() != this.loadTask.favorites.size()))) {
			//a favorite has been removed- reload the list
			// TODO it would be snappier if we just deleted item(s) from the currently displayed list
			loadSites(false);
			return;
		}

		//there's still a possibility that the favorites list is the same length because
		// a favorite was added while another was deleted
		for(Favorite currentFav: currentFavs) {

			Date lastLoadTime = this.loadTask.getLoadTime();

			if(lastLoadTime != null && currentFav.getCreationDate().after(lastLoadTime)) {
				//a favorite has been added- reload the list
				loadSites(false);
				return;
			}
		}

		//temperature conversion has changed
		SharedPreferences settings = getActivity().getSharedPreferences(Home.PREFS_FILE, Activity.MODE_PRIVATE);
    	String newTempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);

    	if(newTempUnit != tempUnit && (newTempUnit == null || !newTempUnit.equals(tempUnit))) {
			tempUnit = newTempUnit;
			loadSites(false);
			return;
    	}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(getActivity(), ViewChart.class);
		FavoriteData selectedFavorite = null;
		
		if(this.loadTask == null || this.loadTask.getGauges() == null) {
			return;
		}
		
		for(FavoriteData currentData: this.loadTask.getGauges()) {
			if(FavoriteAdapter.getItemId(currentData) == id){
                selectedFavorite = currentData;
				break;
			}
		}
		
		if(selectedFavorite == null) {
			Log.w(TAG,"no such data: " + id);
			return;
		}
		
        i.putExtra(ViewChart.KEY_SITE, selectedFavorite.getFavorite().getSite());
        i.putExtra(ViewChart.KEY_VARIABLE, selectedFavorite.getVariable());
        startActivity(i);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mi_about:
			Intent i = new Intent(getActivity(), About.class);
			startActivity(i);
	        return true;
	    case R.id.mi_reload:
	    	loadSites(true);
	    	return true;
	    case R.id.mi_reorder:
	    	Intent i_reorder = new Intent(getActivity(), ReorderFavorites.class);
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
			WsSessionManager.logOut(getActivity());
			return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	/**
	 * @param hardRefresh if true, discard persisted site data as well
	 */
	public void loadSites(boolean hardRefresh) {
		showDialog(Home.DIALOG_ID_LOADING);

		List<FavoriteData> currentGauges = null;
		
		if(this.loadTask != null) {
			currentGauges = this.loadTask.getGauges();
			this.loadTask.cancel(true);
		}

		getListView().getEmptyView().setVisibility(View.INVISIBLE);

		this.loadTask = new LoadFavoritesTask(getActivity(), false, false, hardRefresh);

		//preserve existing gauges in case load fails
		this.loadTask.setGauges(currentGauges);

		if(hardRefresh) {
			this.loadTask.execute(HARD_REFRESH);
		} else {
			this.loadTask.execute();
		}
	}

	public void displayFavorites() {
		Activity activity = getActivity();

		if(activity == null) {
			return;
		}

		if(this.loadTask.getGauges() != null) {

//			if(this.loadTask.getGauges().size() > 0) {
//				SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, MODE_PRIVATE);
//				boolean showDestinationsNotice = !settings.getBoolean(PREF_MIGRATE_DESTINATIONS_NOTICE_SHOWN, false);
//
//				if (showDestinationsNotice) {
//					startActivity(new Intent(this, MigrateToDestinations.class));
//				}
//			
			
			setListAdapter(new FavoriteAdapter(activity, this.loadTask.getGauges()));
			registerForContextMenu(getListView());

			/*
			NOT READY FOR PRIME TIME
			if(v2MigrationDate == null && this.loadTask.getGauges().size() > 0) {
				Intent i = new Intent(this, DestinationOnboard.class);
				startActivity(i);
			}
			 */
		}
		hideProgress();
		if(this.loadTask.getGauges() == null || this.loadTask.getErrorMsg() != null) {
			showError();
		} else if(this.loadTask.getGauges().size() == 0) {
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

	public class LoadFavoritesTask extends ApiCallTask<Integer, Integer, List<Favorite>> {
		public List<Favorite> favorites = null;
		private boolean hardRefresh = false;
		private LoadSitesTask loadSitesTask = null;
		private boolean running = false;
		private List<FavoriteData> previousGauges = null;
		private String errorMsg = null;

		private LoadFavoritesTask(LoadFavoritesTask task) {
			super(task);
		}

		public LoadFavoritesTask(Activity activity, boolean loginRequired, boolean secondTry, boolean hardRefresh) {
			super(activity, REQUEST_GET_FAVORITES, REQUEST_GET_FAVORITES_RECOVER, loginRequired, secondTry);
			this.hardRefresh = hardRefresh;
		}

		@Override
		protected List<Favorite> doApiCall(WsSession session, Integer... params) throws Exception {
			this.running = true;

			List<Favorite> result = FavoritesDaoImpl.getFavorites(getActivity(), null, null);

			List<DestinationFacet> destinationFacets = DestinationFacets.instance.getFavorites(session);

			HashSet<Integer> localDestFacetIds = new HashSet<Integer>(result.size());

			//TODO do all this with a special SQLite call
			for(int a = 0; a < result.size(); a++) {
				
				Favorite currentFav = result.get(a);

				if(currentFav.getDestinationFacet() == null) {
					//TODO send favorite to remote server

					EasyTracker.getTracker().sendException("favorite is not synced", new Exception(), false);
					Log.e(getClass().getName(), "favorite is not synced: " + currentFav.getId());
				} else {
					localDestFacetIds.add(currentFav.getDestinationFacet().getId());

					for(int b = 0; b < destinationFacets.size(); b++) {

						//if modification date of remote favorite is later than that of local favorite
						if(currentFav.getDestinationFacet().getId().equals(destinationFacets.get(b).getId())
						&& (destinationFacets.get(b).getModificationDate().after(currentFav.getDestinationFacet().getModificationDate())
						|| destinationFacets.get(b).getDestination().getModificationDate().after(currentFav.getDestinationFacet().getDestination().getModificationDate()))) {
							//update local favorite

							currentFav.setDestinationFacet(destinationFacets.get(b));

							FavoritesDaoImpl.updateFavorite(Favorites.this.getActivity(), currentFav);
							break;
						}
					}
				}
			}

			for(int a = 0; a < destinationFacets.size(); a++) {
				DestinationFacet remoteDestFacet = destinationFacets.get(a);

				if(!localDestFacetIds.contains(remoteDestFacet.getId())) {

					Favorite newFav = new Favorite(remoteDestFacet.getDestination().getSite(), remoteDestFacet.getVariable().getId());

					newFav.setDestinationFacet(remoteDestFacet);
					newFav.setCreationDate(new Date());

					//save new favorite locally
					FavoritesDaoImpl.createFavorite(Favorites.this.getActivity(), newFav);
					result.add(newFav);
				}
			}

			this.running = false;
			return result;
		}

		public List<FavoriteData> getGauges() {
			if(this.loadSitesTask == null) {
				return null;
			}

			if(this.loadSitesTask.gauges == null) {
				return this.previousGauges;
			}

			return this.loadSitesTask.gauges;
		}

		public void setGauges(List<FavoriteData> gauges) {
			this.previousGauges = gauges;
		}

		public boolean isStale() {
			LoadSitesTask snapshot = this.loadSitesTask;
			if(snapshot == null) {
				return false;
			}

			return (System.currentTimeMillis() - snapshot.loadTime.getTime()) > (2 * 60 * 60 * 1000);
		}

		public Date getLoadTime() {
			if(this.loadSitesTask == null) {
				return null;
			}

			return this.loadSitesTask.loadTime;
		}

		public String getErrorMsg() {
			if(this.loadSitesTask == null) {
				return null;
			}

			return this.loadSitesTask.errorMsg;
		}

		public boolean isRunning() {
			if(this.running) {
				return true;
			}

			LoadSitesTask snapshot = this.loadSitesTask;
			if(snapshot != null) {
				return snapshot.running;
			}

			return false;
		}

		@Override
		protected void onNetworkError() {
			hideProgress();

			if(this.exception != null) {
				this.errorMsg = this.exception.toString() + ": " + exception.getMessage();
				Log.w(Home.TAG,"failed to get remote favorites: ", exception);
			}

			showError();
		}

		@Override
		protected void onNoUIRequired(List<Favorite> favorites) {
			hideProgress();

			if(this.exception != null) {
				this.errorMsg = this.exception.toString() + ": " + exception.getMessage();
				Log.e(Home.TAG,"failed to get remote favorites: ", exception);
				EasyTracker.getTracker().sendException("loadFavorites", exception, false);
				showError();
				return;
			}

			this.loadSitesTask = new LoadSitesTask(favorites);

			//preserve existing gauges in case load fails

			if(this.hardRefresh) {
				this.loadSitesTask.execute(HARD_REFRESH);
			} else {
				this.loadSitesTask.execute();
			}
		}

		@Override
		protected ApiCallTask<Integer, Integer, List<Favorite>> clone() throws CloneNotSupportedException {
			return new LoadFavoritesTask(this);
		}
	}

	public class LoadSitesTask extends AsyncTask<Integer, Integer, List<FavoriteData>> {

		protected static final int STATUS_UPGRADING_FAVORITES = -1;

		public final Date loadTime = new Date();
		public List<FavoriteData> gauges = null;
		public List<Favorite> favorites = null;
		
		public boolean running = false;

		public String errorMsg = null;

		public LoadSitesTask(List<Favorite> favorites) {
			this.favorites = favorites;
		}

		protected void setLoadErrorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
		}

		@Override
		protected List<FavoriteData> doInBackground(Integer... params) {
			running = true;
			try {

				if(favorites.size() == 0) {
					return Collections.emptyList();
				}

				boolean hardRefresh = (params.length > 0 && params[0].equals(HARD_REFRESH));

				Map<SiteId,SiteData> allSiteDataMap = new HashMap<SiteId,SiteData>();
				
				List<FavoriteData> favoriteData = DataSourceController.getSiteData(favorites, hardRefresh);

		    	Map<CommonVariable, CommonVariable> unitConversionMap = CommonVariable.temperatureConversionMap(tempUnit);
				
				for(FavoriteData currentData: favoriteData) {
					
					//convert °C to °F if that setting is enabled
					Map<CommonVariable,Series> datasets = currentData.getSiteData().getDatasets();
					for(Series dataset: datasets.values()) {
						ValueConverter.convertIfNecessary(unitConversionMap, dataset);
					}
				}
				
				return favoriteData;
			} catch (UnknownHostException uhe) {
				setLoadErrorMsg("no network access");
			} catch(Exception e) {
				setLoadErrorMsg(e.getMessage());
				Log.e(getClass().getName(), "",e);
				EasyTracker.getTracker().sendException("getFavorites", e, false);
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<FavoriteData> result) {
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
						removeDialog(Home.DIALOG_ID_LOADING);
						showDialog(Home.DIALOG_ID_UPGRADE_FAVORITES);
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
						removeDialog(Home.DIALOG_ID_UPGRADE_FAVORITES);
						showDialog(Home.DIALOG_ID_LOADING);
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

	public void hideProgress() {
		removeDialog(Home.DIALOG_ID_LOADING);
	}
	public void showError() {
		showDialog(Home.DIALOG_ID_LOADING_ERROR);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if(loadTask != null && this.loadTask.getGauges() != null && this.loadTask.getGauges().size() > 0) {
			MenuItem reorderFavorites = menu.findItem(R.id.mi_reorder);
			reorderFavorites.setVisible(true);
		}

		MenuItem signin = menu.findItem(R.id.mi_sign_in);
		MenuItem signout = menu.findItem(R.id.mi_sign_out);

		if(WsSessionManager.getSession(getActivity()) != null) {
			signin.setVisible(false);
			signout.setVisible(true);
		} else {
			signin.setVisible(true);
			signout.setVisible(false);
		}
		
		super.onPrepareOptionsMenu(menu);
	}
	
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
	    super.onCreateOptionsMenu(menu, inflater);
	    
	    inflater.inflate(R.menu.favorites_menu, menu);
	    
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		FavoriteAdapter adapter = (FavoriteAdapter)((ListView)v).getAdapter();
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

		FavoriteData favoriteData = adapter.getItem(info.position);
		
		if(favoriteData == null) {
			return;
		}

		Variable variable = favoriteData.getSeries().getVariable();
		
		android.view.MenuItem view = menu.add("View");
		view.setOnMenuItemClickListener(new ViewFavoriteListener(favoriteData.getFavorite().getSite(), variable));

		android.view.MenuItem edit = menu.add("Edit");
		edit.setOnMenuItemClickListener(new EditFavoriteListener(favoriteData.getFavorite().getSite(), variable));

		android.view.MenuItem delete = menu.add("Delete");
		delete.setOnMenuItemClickListener(new DeleteFavoriteListener(favoriteData.getFavorite().getSite(), variable));
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == Home.REQUEST_CHOOSE_ACCOUNT || requestCode == Home.REQUEST_HANDLE_RECOVERABLE_AUTH_EXC) {
			signin.authorizeCallback(requestCode, resultCode, data);
			return;
		}

		if(requestCode == REQUEST_GET_FAVORITES || requestCode == REQUEST_GET_FAVORITES_RECOVER) {
			this.loadTask.authorizeCallback(requestCode, resultCode, data);
			return;
		}
		
		//update the view, if necessary
		
		switch(requestCode) {
		case REQUEST_EDIT_FAVORITE:
			if(resultCode == Activity.RESULT_OK) {
				getActivity().sendBroadcast(Home.getWidgetUpdateIntent());
				
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
		        			Favorite newFavorite = FavoritesDaoImpl.getFavorite(getActivity(), favoriteId);
		        			
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
		        			
		        			for(FavoriteData favoriteData: loadTask.getGauges()) {
                                if(newFavorite.getId().equals(favoriteData.getFavorite().getId())) {
                                    if(newName == null) {
                                        //revert to original name of the site
                                        newName = newFavorite.getSite().getName();
                                    }

                                    //update the favorite that is displayed so we don't have to
                                    // reload anything
                                    oldFavorite.setName(newName);

                                    ((FavoriteAdapter)getListAdapter()).notifyDataSetChanged();
                                    return;
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
			if(resultCode == Activity.RESULT_OK) {
				getActivity().sendBroadcast(Home.getWidgetUpdateIntent());
				
				if(this.loadTask == null || this.loadTask.getGauges() == null) {
	        		//favorites haven't even loaded yet- return
	        		return;
	        	}
				
				List<Favorite> newFavorites = FavoritesDaoImpl.getFavorites(getActivity(), null, null);
				
				List<FavoriteData> newSiteData = new ArrayList<FavoriteData>(newFavorites.size());
				
				//reorder loadTask.gauges based on the new favorites order
				for(Favorite newFav: newFavorites) {
					for(FavoriteData favoriteData: loadTask.getGauges()) {
						if(newFav.getId().equals(favoriteData.getFavorite().getId())) {
                            newSiteData.add(favoriteData);
                            //we can get away with this without a ConcurrentModificationException
                            // because it is immediately followed by a break statement
                            loadTask.getGauges().remove(favoriteData);
                            break;
        				}
					}
				}
				
				loadTask.favorites = newFavorites;
				loadTask.getGauges().addAll(newSiteData);
				((FavoriteAdapter)getListAdapter()).notifyDataSetChanged();
			}
		}
	}
	
	private class ViewFavoriteListener implements android.view.MenuItem.OnMenuItemClickListener {
		
		private Site site;
		private Variable variable;
		
		public ViewFavoriteListener(Site site, Variable variable) {
			super();
			this.site = site;
			this.variable = variable;
		}

		@Override
		public boolean onMenuItemClick(android.view.MenuItem item) {
			Intent i = new Intent(Favorites.this.getActivity(), ViewChart.class);
	        i.putExtra(ViewChart.KEY_SITE, site);
	        i.putExtra(ViewChart.KEY_VARIABLE, variable);
	        startActivity(i);
			return true;
		}
	};
	
	private class EditFavoriteListener implements android.view.MenuItem.OnMenuItemClickListener {
		
		private Site site;
		private Variable variable;
		
		public EditFavoriteListener(Site site, Variable variable) {
			super();
			this.site = site;
			this.variable = variable;
		}

		@Override
		public boolean onMenuItemClick(android.view.MenuItem item) {
			
			Intent i = new Intent(Favorites.this.getActivity(), EditFavorite.class);

	        i.putExtra(EditFavorite.KEY_SITE_ID, site.getSiteId());
	        i.putExtra(EditFavorite.KEY_VARIABLE_ID, variable.getId());
	        startActivityForResult(i, REQUEST_EDIT_FAVORITE);
			return true;
		}
	}
	
	private class DeleteFavoriteListener implements android.view.MenuItem.OnMenuItemClickListener {
		
		private Site site;
		private Variable variable;
		
		public DeleteFavoriteListener(Site site, Variable variable) {
			super();
			this.site = site;
			this.variable = variable;
		}

		@Override
		public boolean onMenuItemClick(android.view.MenuItem item) {
			
			FavoritesDaoImpl.deleteFavorite(Favorites.this.getActivity(), site.getSiteId(), variable);
			
			LoadFavoritesTask loadTask = Favorites.this.loadTask;
			
			if(loadTask == null) {
				return true;
			}
			
			if(loadTask.isRunning()) {
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
			
			Iterator<FavoriteData> gaugesI = loadTask.getGauges().iterator();
			
			while(gaugesI.hasNext()) {
				FavoriteData gauge = gaugesI.next();
				if(!gauge.getSiteData().getSite().getSiteId().equals(site.getSiteId())) {
					continue;
				}

				if(gauge.getSiteData().getDatasets().containsKey(variable.getCommonVariable())) {
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
			super(Favorites.this.getActivity(), Home.REQUEST_CHOOSE_ACCOUNT, Home.REQUEST_HANDLE_RECOVERABLE_AUTH_EXC, true, false);
			showDialog(Home.DIALOG_ID_SIGNING_IN);
		}

		public SignIn(SignIn oldTask) {
			super(oldTask);
			showDialog(Home.DIALOG_ID_SIGNING_IN);
		}

		@Override
		protected UserAccount doApiCall(WsSession session, String... params) {
			return session.userAccount;
		}

		@Override
		protected void onNoUIRequired(UserAccount userAccount) {

			removeDialog(Home.DIALOG_ID_SIGNING_IN);

			if(exception != null) {
				Log.e(Home.TAG, "", exception);
			}

			if(userAccount == null) {
				return;
			}

			if(userAccount.getFacetTypes() == 0) {
				//set up this user's account
				startActivityForResult(new Intent(Favorites.this.getActivity(), AccountSettings.class), REQUEST_CREATE_ACCOUNT);
			}
		}

		@Override
		protected ApiCallTask<String, Integer, UserAccount> clone() throws CloneNotSupportedException {
			return new SignIn(this);
		}
	}

	private void showDialog(int id) {
		Activity activity = getActivity();
		if(activity != null) {
			activity.showDialog(id);
		}
	}

	private void removeDialog(int id) {
		Activity activity = getActivity();
		if(activity != null) {
			activity.removeDialog(id);
		}
	}
}
