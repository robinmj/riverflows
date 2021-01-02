package com.riverflows;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.inject.Inject;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.DestinationFacets;
import com.riverflows.wsclient.WsSession;

import java.util.List;

import roboguice.activity.RoboFragmentActivity;

import static roboguice.RoboGuice.getInjector;

/**
 * Created by robin on 10/1/13.
 */
public class SetupDestinations extends RoboFragmentActivity implements LoaderManager.LoaderCallbacks<Pair<Favorite,List<DestinationFacet>>> {

	private static final int REQUEST_SIMILAR_DESTINATIONS = 36941;
	private static final int RECOVERY_REQUEST_SIMILAR_DESTINATIONS = 6392;

    @Inject
    private DestinationFacets destinationFacets;

	private int currentFavoriteIndex = 0;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup_destinations);

		getSupportLoaderManager().initLoader(0, null, this);

		((TextView)findViewById(R.id.status)).setText("Finding Similar Destinations");
	}

	private class FindSimilarDestinationsLoader extends AsyncTaskLoader<Pair<Favorite,List<DestinationFacet>>> {

		private FindSimilarDestinations apiCallTask;

		public FindSimilarDestinationsLoader(Context context, int requestCode, int recoveryRequestCode, boolean loginRequired, boolean secondTry) {
			super(context);
			this.apiCallTask = new FindSimilarDestinations(requestCode, recoveryRequestCode, loginRequired, secondTry);
		}

		@Override
		public Pair<Favorite,List<DestinationFacet>> loadInBackground() {

			try {
				List<Favorite> unshared = FavoritesDaoImpl.getLocalFavorites(getApplicationContext());

				if(unshared.size() == 0) {
					return null;
				}

				Favorite firstFavorite = unshared.get(0);

				List<DestinationFacet> result = apiCallTask.inline(firstFavorite);

				if(apiCallTask.getException() != null) {
					return null;
				}

			} catch(Exception e) {
				Log.e(Home.TAG, "", e);
				Crashlytics.logException(e);
			}

			return null;
		}

		@Override
		protected void onStopLoading() {
			super.onStopLoading();

			FindSimilarDestinations currentTask = this.apiCallTask;

			if(currentTask != null) {
				currentTask.cancel(true);
			}
		}
	}

	private class FindSimilarDestinations extends ApiCallTask<Favorite, Integer, List<DestinationFacet>> {
		private FindSimilarDestinations(int requestCode, int recoveryRequestCode, boolean loginRequired, boolean secondTry) {
			super(SetupDestinations.this, requestCode, recoveryRequestCode, loginRequired, secondTry);
            getInjector(SetupDestinations.this).injectMembers(this);
		}

        private FindSimilarDestinations(FindSimilarDestinations findSimilarDestinations) {
            super(findSimilarDestinations);
            getInjector(SetupDestinations.this).injectMembers(this);
        }

        @Override
        public FindSimilarDestinations clone() {
            return new FindSimilarDestinations(this);
        }

		@Override
		protected List<DestinationFacet> doApiCall(WsSession session, Favorite... favorites) throws Exception {

			return destinationFacets.getSimilarDestinations(session, favorites[0]);
		}

		public List<DestinationFacet> inline(Favorite favorite) {
			return doInBackground(favorite);
		}

		@Override
		protected void onNoUIRequired(List<DestinationFacet> favoriteDestinationHashMap) {}
	}

	@Override
	public Loader<Pair<Favorite,List<DestinationFacet>>> onCreateLoader(int i, Bundle bundle) {
		return new FindSimilarDestinationsLoader(this, REQUEST_SIMILAR_DESTINATIONS, RECOVERY_REQUEST_SIMILAR_DESTINATIONS, true, false);
	}

	@Override
	public void onLoadFinished(Loader<Pair<Favorite,List<DestinationFacet>>> loader, Pair<Favorite,List<DestinationFacet>> favorite) {

		Activity activity = SetupDestinations.this;

		if(favorite.second.size() > 0) {

			((TextView)findViewById(R.id.status)).setText("RiverFlows has a destination similar to your favorite " + favorite.first.getName() + " Would you like to use this destination instead?");

			findViewById(R.id.decision_buttons).setVisibility(View.VISIBLE);

			DestinationFragment destFragment = new DestinationFragment();

			//destFragment.setDestinationFacet(favorite.second.get(currentFavoriteIndex));

			((Button)findViewById(R.id.btn_yes)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					//replace favorite with destination
				}
			});

		} else {
			((TextView)findViewById(R.id.status)).setText("Would you like to create a new public destination from your favorite " + favorite.first.getName() + "?");

			((Button)findViewById(R.id.btn_yes)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					//SetupDestinations.this.startActivityForResult(new Intent(SetupDestinations.this, EditDestination.class));
				}
			});
		}
	}

	@Override
	public void onLoaderReset(Loader<Pair<Favorite,List<DestinationFacet>>> loader) {

	}
}