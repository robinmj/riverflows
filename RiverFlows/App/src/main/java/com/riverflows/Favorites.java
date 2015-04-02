package com.riverflows;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.inject.Inject;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.UserAccount;
import com.riverflows.data.ValueConverter;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.ApiCallLoader;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.DestinationFacets;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;
import com.riverflows.wsclient.WsSessionUIHelper;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import roboguice.fragment.RoboListFragment;

import static roboguice.RoboGuice.getInjector;

public class Favorites extends ListFragment implements LoaderManager.LoaderCallbacks<List<FavoriteData>> {

	private static final String TAG = Home.TAG;

	public static final String FAVORITES_PATH = "/favorites/";

	public static final String PREF_MIGRATE_DESTINATIONS_NOTICE_SHOWN = "pref_share_favorites_notice_shown";

	public static final int REQUEST_EDIT_FAVORITE = 1;
	public static final int REQUEST_REORDER_FAVORITES = 2;
	public static final int REQUEST_CREATE_ACCOUNT = 3247;
	public static final int REQUEST_GET_FAVORITES = 15319;
	public static final int REQUEST_GET_FAVORITES_RECOVER = 4193;
    public static final int REQUEST_DELETE_FAVORITE = 394;
    public static final int REQUEST_DELETE_FAVORITE_RECOVER = 9423;
    public static final int REQUEST_EDIT_DESTINATION = 837;

	public static final int FAVORITES_LOADER_ID = 68312;

	public static final int DIALOG_ID_LOADING_ERROR = 2;
	public static final int DIALOG_ID_SIGNING_IN = 6;

    @Inject
    private DestinationFacets destinationFacets;

    @Inject
    private WsSessionManager wsSessionManager;

	private SignIn signin;

	private String tempUnit = null;

	private volatile Date lastLoadTime = null;

	public static volatile boolean softReloadNeeded = false;

	private Date v2MigrationDate = null;

	private List<FavoriteData> gauges;

	private WsSessionUIHelper wsSessionUIHelper;

	public BroadcastReceiver favChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//list of favorites has changed- reload.
			Favorites.this.softReloadNeeded = true;
		}
	};

	@Override
	public FavoriteAdapter getListAdapter() {
		return (FavoriteAdapter)super.getListAdapter();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		return inflater.inflate(R.layout.favorites, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        getInjector(getActivity()).injectMembers(this);

        CharSequence introStr = getResources().getText(R.string.destinations_intro_2);

        SpannableString str = new SpannableString(introStr);
        str.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                signin = new SignIn();
                signin.execute();
                Log.d(App.TAG, "signin click");
            }
        }, 23,introStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView destination_intro = (TextView)getView().findViewById(R.id.register_sign_in_instructions);
        destination_intro.setText(str);
        destination_intro.setMovementMethod(LinkMovementMethod.getInstance());

		ListView lv = getListView();

		setHasOptionsMenu(true);

		registerForContextMenu(lv);

        getListView().getEmptyView().setVisibility(View.GONE);

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

		Bundle args = new Bundle();
		args.putBoolean(FavoritesLoader.PARAM_HARD_REFRESH, false);

		getActivity().getSupportLoaderManager().initLoader(FAVORITES_LOADER_ID, args, this);
        Log.v(TAG, "initLoader()");

		//getActivity().registerReceiver(this.favChangedReceiver, new IntentFilter(Home.ACTION_FAVORITES_CHANGED));
	}

	@Override
	public void onResume() {
		super.onResume();

        Log.v(App.TAG, "Favorites.onResume()");

		Date lastLoadTime = this.lastLoadTime;

		//discard the cached list items after 2 hours
		if(lastLoadTime != null && lastLoadTime.getTime() < (System.currentTimeMillis() - 120000)) {
			loadSites(true);
			return;
		}

		if(this.softReloadNeeded) {
			loadSites(false);
			return;
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
    public void onDestroy() {
        super.onDestroy();

        //getActivity().unregisterReceiver(this.favChangedReceiver);
    }

    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		FavoriteAdapter adapter = getListAdapter();
		
		if(adapter == null) {
			return;
		}

		FavoriteData selectedFavorite = adapter.getItem(position);
		
		if(selectedFavorite == null) {
			Log.w(TAG,"no such data: " + id);
			return;
		}

        DestinationFacet destinationFacet = selectedFavorite.getFavorite().getDestinationFacet();

        if(destinationFacet != null) {
            Intent i = new Intent(Favorites.this.getActivity(), ViewDestination.class);
            i.putExtra(EditDestination.KEY_DESTINATION_FACET, destinationFacet);
            startActivity(i);
            return;
        }

        Intent i = new Intent(getActivity(), ViewChart.class);
		
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
			this.wsSessionManager.logOut(getActivity());
			return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public Loader<List<FavoriteData>> onCreateLoader(int i, Bundle bundle) {
        Log.v(TAG, "onCreateLoader()");
        showProgress();
		return new FavoritesLoader(getActivity(), getWsSessionUIHelper(), this.tempUnit, bundle.getBoolean(FavoritesLoader.PARAM_HARD_REFRESH));
	}

	private WsSessionUIHelper getWsSessionUIHelper() {
		WsSessionUIHelper helper = this.wsSessionUIHelper;

		if(helper == null) {
			helper = new WsSessionUIHelper(getActivity(), FAVORITES_LOADER_ID, this, REQUEST_GET_FAVORITES, REQUEST_GET_FAVORITES_RECOVER, false);
			this.wsSessionUIHelper = helper;
		}

		return helper;
	}

	@Override
	public void onLoadFinished(Loader<List<FavoriteData>> listLoader, List<FavoriteData> favoriteData) {

        Log.v(TAG, "onLoadFinished()");
		FavoritesLoader loader = (FavoritesLoader)listLoader;

		hideProgress();

        View v = getView();

		if(loader.getException() != null) {
			Log.e(Home.TAG, "failed to get remote favorites: " + loader.getException().getMessage(), loader.getException());

			String detailMsg = loader.getException().getMessage();

			if(detailMsg == null) {
				detailMsg = "Could Not Load Favorites";
			} else {
				detailMsg = "Could Not Load Favorites: " + detailMsg;
			}

            if(v != null) {
                showMessage(v, detailMsg);
            }

			return;
		} else {
            if(v != null) {
                hideMessage(v);
            }
		}

		Activity activity = getActivity();

		if(activity == null) {
			return;
		}

		if(favoriteData != null) {

//			if(this.loadTask.getGauges().size() > 0) {
//				SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, MODE_PRIVATE);
//				boolean showDestinationsNotice = !settings.getBoolean(PREF_MIGRATE_DESTINATIONS_NOTICE_SHOWN, false);
//
//				if (showDestinationsNotice) {
//					startActivity(new Intent(this, MigrateToDestinations.class));
//				}
//

			setListAdapter(new FavoriteAdapter(activity, favoriteData));
            this.lastLoadTime = new Date();

			/*
			NOT READY FOR PRIME TIME
			if(v2MigrationDate == null && this.loadTask.getGauges().size() > 0) {
				Intent i = new Intent(this, DestinationOnboard.class);
				startActivity(i);
			}
			 */
		}
		hideProgress();
		if(favoriteData == null) {
			Log.e(Home.TAG, "null favorites");
            this.softReloadNeeded = true;
            getListView().getEmptyView().setVisibility(View.VISIBLE);
		} else if(favoriteData.size() == 0) {
            getListView().getEmptyView().setVisibility(View.VISIBLE);
		}

        WsSession session = this.wsSessionManager.getSession(activity);

        if(session == null) {
            getView().findViewById(R.id.register_sign_in_instructions).setVisibility(View.VISIBLE);
            return;
        }
        getView().findViewById(R.id.register_sign_in_instructions).setVisibility(View.GONE);

        UserAccount userAccount = session.userAccount;

        if(userAccount != null && userAccount.getFacetTypes() == 0) {
            //set up this user's account
            startActivityForResult(new Intent(Favorites.this.getActivity(), AccountSettings.class), REQUEST_CREATE_ACCOUNT);
        }

		//needed for Android 3.0+
		//invalidateOptionsMenu();
	}

	@Override
	public void onLoaderReset(Loader<List<FavoriteData>> listLoader) {
        Log.v(App.TAG, "Favorites.onLoaderReset()");
	}

	/**
	 * @param hardRefresh if true, discard persisted site data as well
	 */
	public void loadSites(boolean hardRefresh) {

		showProgress();

		//getListView().getEmptyView().setVisibility(View.INVISIBLE);

		Bundle args = new Bundle();
		args.putBoolean(FavoritesLoader.PARAM_HARD_REFRESH, hardRefresh);

		getActivity().getSupportLoaderManager().restartLoader(FAVORITES_LOADER_ID, args, this);
	}

	private void showMessage(View rootView, String message) {
		TextView statusMsgView = (TextView)rootView.findViewById(R.id.status_message);
		statusMsgView.setText(message);
		statusMsgView.setVisibility(View.VISIBLE);
	}

	private void hideMessage(View rootView) {
        rootView.findViewById(R.id.status_message).setVisibility(View.GONE);
	}

	public static class FavoritesLoader extends ApiCallLoader<List<FavoriteData>> {
		private static final String PARAM_HARD_REFRESH = "hardRefresh";

        @Inject
        private DestinationFacets destinationFacets;

		public List<Favorite> favorites = null;
		private boolean hardRefresh = false;
		private String tempUnit = null;

		public FavoritesLoader(Activity activity, WsSessionUIHelper helper, String tempUnit, boolean hardRefresh) {
			super(activity, helper);
            getInjector(activity).injectMembers(this);
			this.hardRefresh = hardRefresh;
			this.tempUnit = tempUnit;
		}

		@Override
		protected void onStartLoading() {
			super.onStartLoading();
			if(this.favorites == null) {
                Log.v(TAG, "forceLoad()");
				forceLoad();
			}
		}

		@Override
		protected List<FavoriteData> doApiCall(WsSession session) throws Exception {
            Log.v(TAG, "doApiCall()");

			this.favorites = FavoritesDaoImpl.getFavorites(FavoritesLoader.this.getContext(), null, null);

			List<DestinationFacet> destinationFacets = this.destinationFacets.getFavorites(session);

			HashSet<Integer> localDestFacetIds = new HashSet<Integer>(this.favorites.size());

			//TODO do all this with a special SQLite call
			for(int a = 0; a < this.favorites.size(); a++) {
				
				Favorite currentFav = this.favorites.get(a);

				if(currentFav.getDestinationFacet() == null) {
					//TODO send favorite to remote server

					EasyTracker.getTracker().sendException("favorite is not synced", new Exception(), false);
					Log.e(getClass().getName(), "favorite is not synced: " + currentFav.getId());
				} else {
					localDestFacetIds.add(currentFav.getDestinationFacet().getId());

					for(int b = 0; b < destinationFacets.size(); b++) {

						//if modification date of remote favorite is later than that of local favorite
						if(currentFav.getDestinationFacet().getId().equals(destinationFacets.get(b).getId())) {
                            //local DB doesn't store destination ID, which is needed for remotely updating destination
                            currentFav.getDestinationFacet().getDestination().setId(destinationFacets.get(b).getDestination().getId());

                            if(destinationFacets.get(b).getModificationDate().after(currentFav.getDestinationFacet().getModificationDate())
                                || destinationFacets.get(b).getDestination().getModificationDate().after(currentFav.getDestinationFacet().getDestination().getModificationDate())) {
                                //update local favorite

                                currentFav.setDestinationFacet(destinationFacets.get(b));

                                FavoritesDaoImpl.updateFavorite(FavoritesLoader.this.getContext(), currentFav);
                                break;
                            }
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
					FavoritesDaoImpl.createFavorite(FavoritesLoader.this.getContext(), newFav);
					this.favorites.add(newFav);
				}
			}

			if(this.favorites.size() == 0) {
				return Collections.emptyList();
			}

			List<FavoriteData> favoriteData = DataSourceController.getSiteData(this.favorites, this.hardRefresh);

			Map<CommonVariable, CommonVariable> unitConversionMap = CommonVariable.temperatureConversionMap(this.tempUnit);

			for(FavoriteData currentData: favoriteData) {

				//convert °C to °F if that setting is enabled
				Map<CommonVariable,Series> datasets = currentData.getSiteData().getDatasets();
				for(Series dataset: datasets.values()) {
					ValueConverter.convertIfNecessary(unitConversionMap, dataset);
				}
			}

            Favorites.softReloadNeeded = false;
			return favoriteData;
		}

		public Bundle makeArgs() {
			Bundle loaderParams = new Bundle();
			loaderParams.putBoolean(PARAM_HARD_REFRESH, this.hardRefresh);

			return loaderParams;
		}
	}

	public static class SignInDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			ProgressDialog signingInDialog = new ProgressDialog(getActivity());
			signingInDialog.setMessage("Signing In...");
			signingInDialog.setIndeterminate(true);
			signingInDialog.setCancelable(true);
			return signingInDialog;
		}
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		FavoriteAdapter adapter = getListAdapter();

		if(adapter != null && adapter.getCount() > 0) {
			MenuItem reorderFavorites = menu.findItem(R.id.mi_reorder);
			reorderFavorites.setVisible(true);
		}

		MenuItem signin = menu.findItem(R.id.mi_sign_in);
		MenuItem signout = menu.findItem(R.id.mi_sign_out);

		if(this.wsSessionManager.getSession(getActivity()) != null) {
			signin.setVisible(false);
			signout.setVisible(true);
		} else {
			signin.setVisible(true);
			signout.setVisible(false);
		}
		
		super.onPrepareOptionsMenu(menu);
	}
	
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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

        boolean allowEdit = true;

        if(favoriteData.getFavorite().getDestinationFacet() != null) {
            WsSession session = this.wsSessionManager.getSession(getActivity());
            //there is a slim possibility that we won't have a valid session here- in that case,
            // still show the Edit menu item and allow EditDestination to be responsible for preventing
            // editing if the user doesn't own the destination facet.
            if(session != null && session.userAccount != null) {
                if(!session.userAccount.getId().equals(favoriteData.getFavorite().getDestinationFacet().getUser().getId())) {
                    allowEdit = false;
                }
            }
        }

        if(allowEdit) {
            android.view.MenuItem edit = menu.add("Edit");
            edit.setOnMenuItemClickListener(new EditFavoriteListener(favoriteData.getFavorite()));
        }

		android.view.MenuItem delete = menu.add("Delete");
		delete.setOnMenuItemClickListener(new DeleteFavoriteListener(favoriteData));
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == Home.REQUEST_CHOOSE_ACCOUNT || requestCode == Home.REQUEST_HANDLE_RECOVERABLE_AUTH_EXC) {
			signin.authorizeCallback(requestCode, resultCode, data);
			return;
		}

		if(requestCode == REQUEST_GET_FAVORITES || requestCode == REQUEST_GET_FAVORITES_RECOVER) {
			WsSessionUIHelper helper = this.wsSessionUIHelper;

			if(helper == null) {
				Log.e(Home.TAG, "handling result from requestCode " + requestCode + " without session");
				return;
			}

			helper.authorizeCallback(requestCode, resultCode, data, getArguments());
			return;
		}
		
		//update the view, if necessary
		
		switch(requestCode) {
		case REQUEST_EDIT_FAVORITE:
			if(resultCode == Activity.RESULT_OK) {
				getActivity().sendBroadcast(Home.getWidgetUpdateIntent());
				
	        	if(Favorites.this.gauges == null) {
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
		        	for(FavoriteData oldFavorite: Favorites.this.gauges) {
		        		if(oldFavorite.getFavorite().getId() == favoriteId) {
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
		        			
		        			for(FavoriteData favoriteData: Favorites.this.gauges) {
                                if(newFavorite.getId().equals(favoriteData.getFavorite().getId())) {
                                    if(newName == null) {
                                        //revert to original name of the site
                                        newName = newFavorite.getSite().getName();
                                    }

                                    //update the favorite that is displayed so we don't have to
                                    // reload anything
                                    oldFavorite.getFavorite().setName(newName);

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
        case REQUEST_EDIT_DESTINATION:

            if(data == null) {
                //nothing was changed
                return;
            }

            DestinationFacet updatedFacet = (DestinationFacet)data.getSerializableExtra(EditDestination.KEY_DESTINATION_FACET);

            if(updatedFacet != null) {

                FavoriteAdapter adapter = getListAdapter();

                if(adapter == null) {
                    return;
                }

                FavoriteData changedFavorite = adapter.update(updatedFacet);

                if(changedFavorite != null) {
                    FavoritesDaoImpl.updateFavorite(this.getActivity(), changedFavorite.getFavorite());
                }
            }
		case REQUEST_REORDER_FAVORITES:
			if(resultCode == Activity.RESULT_OK) {
				getActivity().sendBroadcast(Home.getWidgetUpdateIntent());

				loadSites(false);
			} else if(resultCode == ReorderFavorites.RESULT_SAVE_FAILED) {
                getActivity().sendBroadcast(Home.getWidgetUpdateIntent());

                loadSites(false);

                View v = getView();

                if(data != null) {
                    String msg = data.getStringExtra(ReorderFavorites.EXTRA_SAVE_FAILED_EXCEPTION_DETAIL);
                    if(msg != null && v != null) {
                        showMessage(v, msg);
                    }
                } else if(v != null) {
                    showMessage(v, "Could not save order of favorites");
                }
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

        private Favorite favorite;
		
		public EditFavoriteListener(Favorite favorite) {
			super();
			this.favorite = favorite;
		}

		@Override
		public boolean onMenuItemClick(android.view.MenuItem item) {

            if(favorite.getDestinationFacet() == null) {

                Intent i = new Intent(Favorites.this.getActivity(), EditFavorite.class);

                i.putExtra(EditFavorite.KEY_SITE_ID, favorite.getSite().getSiteId());
                i.putExtra(EditFavorite.KEY_VARIABLE_ID, favorite.getVariable());
                startActivityForResult(i, REQUEST_EDIT_FAVORITE);
                return true;
            }

            Intent i = new Intent(Favorites.this.getActivity(), EditDestination.class);
            i.putExtra(EditDestination.KEY_DESTINATION_FACET, this.favorite.getDestinationFacet());

            startActivityForResult(i, REQUEST_EDIT_DESTINATION);
            return true;
        }
	}
	
	private class DeleteFavoriteListener implements android.view.MenuItem.OnMenuItemClickListener {
		
		private FavoriteData favoriteData;

		public DeleteFavoriteListener(FavoriteData favoriteData) {
			super();
			this.favoriteData = favoriteData;
		}

		@Override
		public boolean onMenuItemClick(android.view.MenuItem item) {

            if(this.favoriteData.getFavorite().getDestinationFacet() != null) {
                showProgress();
                new DeleteRemoteFavoriteTask(getActivity()).execute(this.favoriteData);
            } else {
                FavoritesDaoImpl.deleteFavorite(Favorites.this.getActivity(), favoriteData.getFavorite().getSite().getSiteId(), favoriteData.getVariable());
                deleteFavoriteFromView(this.favoriteData);
            }
            return true;
		}
	}

    private class DeleteRemoteFavoriteTask extends ApiCallTask<FavoriteData,Integer,FavoriteData> {
        private DeleteRemoteFavoriteTask(Activity activity) {
            super(activity,REQUEST_DELETE_FAVORITE, REQUEST_DELETE_FAVORITE_RECOVER, true, false);
        }

        private DeleteRemoteFavoriteTask(DeleteRemoteFavoriteTask oldTask) {
            super(oldTask);
        }

        @Override
        protected FavoriteData doApiCall(WsSession session, FavoriteData... params) throws Exception {
            destinationFacets.removeFavorite(session, params[0].getFavorite().getDestinationFacet().getId());

            FavoritesDaoImpl.deleteFavorite(Favorites.this.getActivity(), params[0].getFavorite().getSite().getSiteId(), params[0].getVariable());
            return params[0];
        }

        @Override
        protected void onNetworkError() {
            View v = getView();

            if(v != null) {
                Favorites.this.showMessage(v, "Failed to delete favorite: network error");
            }
            Log.w(App.TAG, "Failed to delete favorite: network error", getException());
        }

        @Override
        protected void onNoUIRequired(FavoriteData favoriteData) {
            View v = getView();

            if(getException() != null) {
                if(v != null) {
                    Favorites.this.showMessage(v, "Failed to delete favorite: " + getException().getMessage());
                }
                Log.e(App.TAG, "Failed to delete favorite: network error", getException());
                EasyTracker.getTracker().sendException(getClass().getSimpleName(), getException(), false);

                return;
            }


            if(v != null) {
                hideProgress();
                hideMessage(v);
            }
            deleteFavoriteFromView(favoriteData);
        }

        @Override
        protected DeleteRemoteFavoriteTask clone() throws CloneNotSupportedException {
            return new DeleteRemoteFavoriteTask(this);
        }
    }

	private class SignIn extends ApiCallTask<String, Integer, UserAccount> {

		private SignInDialogFragment signInDialog;

		public SignIn(){
			super(Favorites.this.getActivity(), Home.REQUEST_CHOOSE_ACCOUNT, Home.REQUEST_HANDLE_RECOVERABLE_AUTH_EXC, true, false);

			signInDialog = new SignInDialogFragment();
			signInDialog.show(Favorites.this.getFragmentManager(), "signin");
		}

		public SignIn(SignIn oldTask) {
			super(oldTask);
			signInDialog = new SignInDialogFragment();
			signInDialog.show(Favorites.this.getFragmentManager(), "signin");
		}

		@Override
		protected UserAccount doApiCall(WsSession session, String... params) {
			return session.userAccount;
		}

		@Override
		protected void onNoUIRequired(UserAccount userAccount) {

			SignInDialogFragment signInDialog = this.signInDialog;

			if(signInDialog != null) {
				signInDialog.dismiss();
				this.signInDialog = null;
			}

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

	private void showProgress() {
		View v = getView();

		if(v == null)
			return;

		v.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
	}

	private void hideProgress() {
		View v = getView();

		if(v == null)
			return;

		v.findViewById(R.id.progress_bar).setVisibility(View.GONE);
	}

    private void deleteFavoriteFromView(FavoriteData favoriteData) {

        FavoriteAdapter adapter = getListAdapter();

        if(adapter == null) {
            return;
        }

        adapter.remove(favoriteData);
    }
}
