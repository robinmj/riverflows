package com.riverflows.wsclient;

import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.UserAccount;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by robin on 9/25/13.
 */
public class Destinations {

    private static final Log LOG = LogFactory.getLog(Destinations.class);

    public static DestinationFacet saveDestinationWithFacet(WsSession session, DestinationFacet destination) throws Exception {
		HttpPost postCmd = new HttpPost(DataSourceController.MY_RIVERFLOWS_WS_BASE_URL + "/destinations.json?auth_token=" + session.authToken);
		HttpClient client = new DataSourceController.SSLHttpClient();

		JSONObject entity = new JSONObject();

		entity.put("destination", destinationAsJson(destination.getDestination()));
		entity.put("destination_facet", DestinationFacets.instance.toJson(destination));

		postCmd.setEntity(new StringEntity(entity.toString()));

		postCmd.addHeader("Content-Type", "application/json");
		postCmd.addHeader("Accept", "application/json");

		HttpResponse httpResponse = client.execute(postCmd);

		LOG.debug(postCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

		if(httpResponse.getStatusLine().getStatusCode() != 201) {
			throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
		}

		JSONObject responseObj = new JSONObject(Utils.getString(httpResponse.getEntity().getContent()));

		LOG.info("responseJson: " + responseObj);

		JSONObject destJson = responseObj.getJSONObject("destination");

		DestinationFacet result = DestinationFacets.instance.fromJson(responseObj.getJSONObject("destination_facet"));
		result.setDestination(parseDestination(destJson));

		return result;
    }

    public static final JSONObject destinationAsJson(Destination destination) throws JSONException {
        JSONObject destJson = new JSONObject();
		destJson.put("id", destination.getId());
		if(destination.getUser() != null) {
	        destJson.put("user_id", destination.getUser().getId());
		}
        destJson.put("name", destination.getName());
        destJson.put("description", destination.getDescription());
        //destJson.put("visual_gauge_latitude",
        //destJson.put("visual_gauge_longitude",

		return destJson;
    }

	public static final Destination parseDestination(JSONObject destJson) throws JSONException {
		Destination destination = new Destination();
		if(destJson.has("id")) {
			destination.setId(destJson.getInt("id"));
		}
		destination.setUser(new UserAccount());
		destination.getUser().setId(destJson.getInt("user_id"));
		destination.setName(destJson.getString("name"));
		destination.setDescription(destJson.getString("description"));
		return destination;
	}
}
