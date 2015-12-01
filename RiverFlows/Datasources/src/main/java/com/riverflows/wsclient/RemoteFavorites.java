package com.riverflows.wsclient;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by robin on 6/2/13.
 *
 * @see com.riverflows.wsclient.DestinationFacets
 */
public class RemoteFavorites extends WebModel<Favorite> {

    private DestinationFacets destinationFacets = new DestinationFacets();

    private static final Log LOG = LogFactory.getLog(RemoteFavorites.class);

    //singleton
    public static final RemoteFavorites instance = new RemoteFavorites();

    @Override
    public String getResourceName() {
        return "favorite";
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
            favorite.setDestinationFacet(destinationFacets.fromJson(json.getJSONObject("destination_facet")));
        } else {
            DestinationFacet placeholderFacet = new DestinationFacet();
            placeholderFacet.setId(json.getInt("destination_facet_id"));
            placeholderFacet.setPlaceholderObj(true);
            favorite.setDestinationFacet(placeholderFacet);
        }
        favorite.setCreationDate(getDate(json,"created_at"));
        try {
            favorite.setOrder(json.getInt("order"));
        } catch (JSONException jsone) {
            //order is null- this favorite should appear after those with the order specified
            favorite.setOrder(Integer.MAX_VALUE);
        }

        return favorite;
    }

    public List<Favorite> reorderFavorites(WsSession session, int[] destFacetIds) throws Exception {
        HttpPut putCmd = new HttpPut(DataSourceController.RIVERFLOWS_WS_BASEURL + getResource()
                + "/reorder.json?auth_token=" + session.authToken);

        if(destFacetIds.length == 0) {
            throw new IllegalArgumentException("no destination facet IDs specified");
        }

        List<BasicNameValuePair> destFacetIdsParams = new ArrayList<BasicNameValuePair>(destFacetIds.length);

        for(int destFacetId: destFacetIds) {
            destFacetIdsParams.add(new BasicNameValuePair("destination_facet_ids[]", "" + destFacetId));
        }

        putCmd.setEntity(new UrlEncodedFormEntity(destFacetIdsParams));

        HttpClient client = getHttpClientFactory().getHttpClient();

        HttpResponse httpResponse = client.execute(putCmd);

        LOG.debug(putCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

        if(httpResponse.getStatusLine().getStatusCode() != 200) {
            throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
        }

        JSONArray responseArray = new JSONArray(Utils.getString(httpResponse.getEntity().getContent()));

        LOG.info("responseJson: " + responseArray);

        return RemoteFavorites.instance.fromJsonArray(responseArray);
    }
}
