package com.riverflows.wsclient;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.inject.Inject;
import com.riverflows.App;
import com.riverflows.Favorites;
import com.riverflows.Home;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.db.FavoritesDaoImpl;

import roboguice.RoboGuice;

/**
 * Created by robin on 11/14/14.
 */
public class ToggleFavoriteTask extends ApiCallTask<Object, Integer, Favorite> {

    public static final int REQUEST_TOGGLE_FAVORITE = 7149;
    public static final int REQUEST_TOGGLE_FAVORITE_RECOVER = 13640;

    @Inject
    private DestinationFacets destinationFacets;

    private Favorite favorite;

    public ToggleFavoriteTask(Activity activity, boolean secondTry, Favorite favorite) {
        super(activity, REQUEST_TOGGLE_FAVORITE, REQUEST_TOGGLE_FAVORITE_RECOVER, favorite.getDestinationFacet() != null, secondTry);
        this.favorite = favorite;
        RoboGuice.getInjector(activity).injectMembers(this);
    }

    public ToggleFavoriteTask(ToggleFavoriteTask task) {
        super(task);
        this.favorite = task.favorite;
    }

    @Override
    protected Favorite doApiCall(WsSession session, Object... params) throws Exception {
        DestinationFacet facet = this.favorite.getDestinationFacet();

        boolean isFavorite = FavoritesDaoImpl.isFavorite(this.activity.getApplicationContext(), this.favorite.getSite().getSiteId(), this.favorite.getVariable());

        if(isFavorite) {
            if(facet != null) {
                this.destinationFacets.removeFavorite(session, facet.getId());
                FavoritesDaoImpl.deleteFavorite(this.activity.getApplicationContext(), facet.getId());
            } else {
                FavoritesDaoImpl.deleteFavorite(this.activity.getApplicationContext(), this.favorite.getSite().getSiteId(), this.favorite.getVariable());
            }

            return null;
        } else {
            if(facet != null) {
                this.destinationFacets.saveFavorite(session, facet.getId());
            }

            FavoritesDaoImpl.createFavorite(this.activity.getApplicationContext(), this.favorite);

            return this.favorite;
        }
    }

    /**
     * @param newFavorite will be null if favorite was removed
     */
    @Override
    protected void onNoUIRequired(Favorite newFavorite) {

        if(this.exception != null) {
            Log.e(App.TAG,"could not toggle favorite", this.exception);
        }

        this.activity.sendBroadcast(Home.getWidgetUpdateIntent());
        //this.activity.sendBroadcast(new Intent(Home.ACTION_FAVORITES_CHANGED));
        Favorites.softReloadNeeded = true;
    }

    @Override
    protected ToggleFavoriteTask clone() throws CloneNotSupportedException {
        return new ToggleFavoriteTask(this);
    }
}
