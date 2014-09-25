package com.riverflows.wsclient;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by robin on 6/2/13.
 */
public class RemoteFavorites extends WebModel<Favorite> {
    //singleton
    public static final RemoteFavorites instance = new RemoteFavorites();

    @Override
    public String getResource() {
        return "/favorites";
    }

    @Override
    public JSONObject toJson(Favorite obj) throws JSONException {
        return null;
    }

    @Override
    public Favorite fromJson(JSONObject json) throws Exception {

        Favorite favorite = new Favorite(null, null);
        if(json.has("id")) {
            favorite.setId(json.getInt("id"));
        }

        if(json.has("destination_facet")) {
            favorite.setDestinationFacet(DestinationFacets.instance.fromJson(json.getJSONObject("destination_facet")));
        } else {
            DestinationFacet placeholderFacet = new DestinationFacet();
            placeholderFacet.setId(json.getInt("destination_facet_id"));
            placeholderFacet.setPlaceholderObj(true);
            favorite.setDestinationFacet(placeholderFacet);
        }
        favorite.setCreationDate(getDate(json,"created_at"));
        favorite.setOrder(json.getInt("order"));

        return null;
    }
}
