package com.riverflows;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.inject.Inject;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.MapItem;
import com.riverflows.data.Site;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.ToggleFavoriteTask;
import com.riverflows.wsclient.WsSessionManager;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import roboguice.activity.RoboListActivity;

public abstract class MapItemList extends RoboListActivity {
	
	private static final String TAG = Home.TAG;
	
	public static final String PK_MASTER_SITE_LIST_LAST_LOAD_TIME = "masterSiteListLastLoadTime";

	private static final int REQUEST_CREATE_DESTINATION = 4359042;

	/**
	 * reload the master site list every 2 weeks
	 */
	public static final long MASTER_SITE_LIST_RELOAD_INTERVAL = 14 * 24 * 60 * 60 * 1000;
	
	private List<MapItem> items = null;
	public static final int DIALOG_ID_LOADING = 1;
	public static final int DIALOG_ID_LOADING_ERROR = 2;
	public static final int DIALOG_ID_MASTER_LOADING = 3;
	public static final int DIALOG_ID_MASTER_LOADING_ERROR = 4;
	public static final int DIALOG_ID_UPGRADE_FAVORITES = 5;

    @Inject
    protected WsSessionManager wsSessionManager;
	
	private LoadSitesTask loadTask = null;
	private String errorMsg = null;
	
	private TextWatcher filterFieldWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) { }

        @Override
        public void afterTextChanged(Editable s) {
        	if(getListAdapter() == null) {
        		return;
        	}
        	
            ((MapItemAdapter)getListAdapter()).getFilter().filter(s.toString());
        }
    };
	
	private OnFocusChangeListener filterFieldFocusListener = new OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			if(!hasFocus) {
				hideSoftKeyboard();
			}
		}
	};

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
    	hideSoftKeyboard();

        MapItem selectedItem = null;
		
		for(MapItem currentItem: items) {
			if(MapItemAdapter.getItemId(currentItem) == id){
                selectedItem = currentItem;
				break;
			}
		}
		
		if(selectedItem == null) {
			Log.w(TAG,"no such data: " + id);
			return;
		}

        if(selectedItem.destinationFacet != null) {
            Intent i = new Intent(this, ViewDestination.class);
            i.putExtra(ViewDestination.KEY_DESTINATION_FACET, selectedItem.destinationFacet);
            startActivity(i);
            return;
        }

        Intent i = new Intent(this, ViewSite.class);
        i.putExtra(ViewSite.KEY_SITE, selectedItem.getSite());
        i.putExtra(ViewSite.KEY_VARIABLE, selectedItem.getVariable());
        startActivity(i);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	hideSoftKeyboard();
	    // Handle item selection
	    switch (item.getItemId()) {
	    case android.R.id.home:
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.site_list);
		
		/* probably not necessary anymore now that we can reload on a state-by-state basis
		//check to see if the master list needs to be reloaded
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		long lastLoadTime = prefs.getLong(PK_MASTER_SITE_LIST_LAST_LOAD_TIME, 0);
		
		if(System.currentTimeMillis() - lastLoadTime > MASTER_SITE_LIST_RELOAD_INTERVAL) {
			showDialog(DIALOG_ID_MASTER_LOADING);
			LoadMasterSiteListTask masterSiteListTask = new LoadMasterSiteListTask();
			masterSiteListTask.execute();
		}*/

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		EditText siteFilterField = (EditText)findViewById(R.id.site_filter_field);
		siteFilterField.addTextChangedListener(filterFieldWatcher);
		siteFilterField.setOnFocusChangeListener(filterFieldFocusListener);

        //see onRetainNonConfigurationInstance()
    	final Object[] data = (Object[])getLastNonConfigurationInstance();
        
    	if(data == null) {
    		//make the request for site data
    		loadSites(false);
    	} else {
    		this.loadTask = (LoadSitesTask)data[0];
    		if(this.loadTask != null) {
    			this.loadTask.setActivity(this);
        		this.errorMsg = this.loadTask.errorMsg;
    		}
    		this.items = (List<MapItem>)data[1];
    		displaySites();
    	}
	}
	
	@Override
	public Object[] onRetainNonConfigurationInstance() {
		return new Object[]{this.loadTask, this.items};
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
			ErrorMsgDialog errorDialog = new ErrorMsgDialog(this, errorMsg);
			return errorDialog;
		case DIALOG_ID_MASTER_LOADING:
			ProgressDialog masterDialog = new ProgressDialog(this);
			masterDialog.setMessage("Downloading Master Site List...");
			masterDialog.setIndeterminate(true);
			masterDialog.setCancelable(true);
	        return masterDialog;
		case DIALOG_ID_MASTER_LOADING_ERROR:
			ErrorMsgDialog masterErrorDialog = new ErrorMsgDialog(this, errorMsg);
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
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.standard_menu, menu);
	    return true;
	}
	
	/**
	 * @param hardRefresh if true, discard persisted site data as well
	 */
	public void loadSites(boolean hardRefresh) {
		showDialog(DIALOG_ID_LOADING);
		
		findViewById(R.id.site_filter_field).setVisibility(View.INVISIBLE);
		
		this.loadTask = createLoadStationsTask();
		this.loadTask.setActivity(this);
		
		if(hardRefresh) {
			this.loadTask.execute(HARD_REFRESH);
		} else {
			this.loadTask.execute();
		}
	}
	
	public void displaySites() {
		if(items != null) {
			setListAdapter(new MapItemAdapter(getApplicationContext(), items));

			registerForContextMenu(getListView());
		}
		try {
			removeDialog(DIALOG_ID_LOADING);
		} catch(IllegalArgumentException iae) {
			if(Log.isLoggable(TAG, Log.INFO)) {
				Log.i(TAG, "can't remove dialog; activity no longer active");
			}
		}
		if(errorMsg != null) {
			try {
				showDialog(DIALOG_ID_LOADING_ERROR);
			} catch(BadTokenException bte) {
				if(Log.isLoggable(TAG, Log.INFO)) {
					Log.i(TAG, "can't display dialog; activity no longer active");
				}
			}
		} else {
			EditText siteFilterField = (EditText)findViewById(R.id.site_filter_field);
			
			siteFilterField.setVisibility(View.VISIBLE);
			siteFilterField.requestFocus();
		}
	}

	/** parameter for LoadSitesTask */
	public static final Integer HARD_REFRESH = new Integer(1);
	
	public abstract class LoadSitesTask extends AsyncTask<Integer, Integer, List<MapItem>> {
		
		protected static final int STATUS_UPGRADING_FAVORITES = -1;

		public final Date loadTime = new Date();
		private String errorMsg = null;
		private MapItemList activity;
		
		public void setActivity(MapItemList activity) {
			this.activity = activity;
			this.activity.loadTask = this;
		}
		
		protected void setLoadErrorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
		}
		
		@Override
		protected abstract List<MapItem> doInBackground(Integer... params);
		
		@Override
		protected void onPostExecute(List<MapItem> result) {
			this.activity.items = result;
			this.activity.errorMsg = this.errorMsg;
			this.activity.displaySites();
			this.activity.loadTask = null;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			if(values == null || values.length == 0) {
				return;
			}
			if(values.length == 2 && values[0] == STATUS_UPGRADING_FAVORITES) {
				if(values[1] == 0) {
					try {
						this.activity.removeDialog(DIALOG_ID_LOADING);
						this.activity.showDialog(DIALOG_ID_UPGRADE_FAVORITES);
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
						this.activity.removeDialog(DIALOG_ID_UPGRADE_FAVORITES);
						this.activity.showDialog(DIALOG_ID_LOADING);
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
	

	
	private class ViewVariableListener implements MenuItem.OnMenuItemClickListener {

		private Site selectedStation = null;
		private Variable selectedVariable = null;
		
		public ViewVariableListener(Site selectedStation, Variable selectedVariable) {
			this.selectedStation = selectedStation;
			this.selectedVariable = selectedVariable;
		}
		
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			
			Intent i = new Intent(getBaseContext(), ViewSite.class);
			
	        i.putExtra(ViewSite.KEY_SITE, selectedStation);
	        i.putExtra(ViewSite.KEY_VARIABLE, selectedVariable);
	        startActivity(i);
	        return true;
		}
	}
	
	private class AddToFavoritesListener implements MenuItem.OnMenuItemClickListener{

		private MapItem mapItem = null;
        private Variable variable = null;
		
		public AddToFavoritesListener(MapItem mapItem, Variable variable) {
			this.mapItem = mapItem;
            this.variable = variable;
		}

		
		@Override
		public boolean onMenuItemClick(MenuItem item) {

            Favorite favorite = null;
            String confirmation = null;

            if(mapItem.isDestination()) {
                favorite = new Favorite(this.mapItem.destinationFacet);
                confirmation = MessageFormat.format(getString(R.string.add_favorite_dest_confirmation), this.mapItem.getSite().getName());
            } else {
                favorite = new Favorite(this.mapItem.getSite(), this.variable.getId());
                confirmation = MessageFormat.format(getString(R.string.add_favorite_confirmation), variable.getName(), this.mapItem.getSite().getName());
            }

            new ToggleFavoriteTask(MapItemList.this, false, favorite).execute();
			
			if(item.isChecked()) {
				item.setChecked(false);
			} else {
				item.setChecked(true);
			}
			sendBroadcast(Home.getWidgetUpdateIntent());
			//sendBroadcast(new Intent(Home.ACTION_FAVORITES_CHANGED));
            Favorites.softReloadNeeded = true;

			
			Toast.makeText(getApplicationContext(), confirmation, Toast.LENGTH_SHORT).show();
			
			return true;
		}
	}

	private class CreateDestinationListener implements MenuItem.OnMenuItemClickListener{

		private Site selectedStation = null;
		private Variable selectedVariable = null;

		public CreateDestinationListener(Site selectedStation, Variable selectedVariable) {
			this.selectedStation = selectedStation;
			this.selectedVariable = selectedVariable;
		}


		@Override
		public boolean onMenuItemClick(MenuItem item) {

			Intent i = new Intent(MapItemList.this, EditDestination.class);
			i.putExtra(EditDestination.KEY_SITE, selectedStation);
			i.putExtra(EditDestination.KEY_VARIABLE, selectedVariable);

			startActivityForResult(i, REQUEST_CREATE_DESTINATION);

			return true;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		MapItemAdapter adapter = (MapItemAdapter)((ListView)v).getAdapter();
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		
		MapItem mapItem = adapter.getItem(info.position);
		
		Variable[] supportedVars = mapItem.getSite().getSupportedVariables();

		boolean loggedIn = (this.wsSessionManager.getSession(this) != null);

		if(mapItem.isDestination()) {
            if(loggedIn && !FavoritesDaoImpl.isFavorite(getApplicationContext(), mapItem.destinationFacet.getId().intValue())) {
                MenuItem addFavoriteDest = menu.add(ContextMenu.NONE, supportedVars.length, supportedVars.length, "Add To Favorites");
                addFavoriteDest.setOnMenuItemClickListener(new AddToFavoritesListener(mapItem, null));
            }
		} else {
            SubMenu submenu = null;
            if(loggedIn) {
                submenu = menu.addSubMenu(ContextMenu.NONE, supportedVars.length, supportedVars.length, "Create Destination");
            } else {
                submenu = menu.addSubMenu(ContextMenu.NONE, supportedVars.length, supportedVars.length, "Add To Favorites");
            }

            for(int a = 0; a < supportedVars.length; a++) {
                MenuItem viewVariableItem = menu.add(ContextMenu.NONE,a,a,supportedVars[a].getName() + ", " + supportedVars[a].getCommonVariable().getUnit());
                viewVariableItem.setOnMenuItemClickListener(new ViewVariableListener(mapItem.getSite(), supportedVars[a]));

                MenuItem addFavoriteItem = submenu.add(ContextMenu.NONE,a,a,supportedVars[a].getName() + ", " + supportedVars[a].getCommonVariable().getUnit());
                addFavoriteItem.setCheckable(true);
                addFavoriteItem.setChecked(FavoritesDaoImpl.isFavorite(getApplicationContext(), mapItem.getSite().getSiteId(), supportedVars[a]));

                if(loggedIn) {
                    addFavoriteItem.setOnMenuItemClickListener(new CreateDestinationListener(mapItem.getSite(), supportedVars[a]));
                } else {
                    addFavoriteItem.setOnMenuItemClickListener(new AddToFavoritesListener(mapItem, supportedVars[a]));
                }
            }
		}
	}
	
	private void hideSoftKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

		EditText stateFilterField = (EditText)findViewById(R.id.site_filter_field);
		imm.hideSoftInputFromWindow(stateFilterField.getWindowToken(), 0);
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CREATE_DESTINATION) {
            if(resultCode == RESULT_OK) {
                DestinationFacet facet = (DestinationFacet) data.getSerializableExtra(EditDestination.KEY_DESTINATION_FACET);
                Intent i = new Intent(this, ViewDestination.class);
                i.putExtra(ViewDestination.KEY_DESTINATION_FACET, facet);
                startActivity(i);
                return;
            }
        }
    }

    protected abstract LoadSitesTask createLoadStationsTask();
}
