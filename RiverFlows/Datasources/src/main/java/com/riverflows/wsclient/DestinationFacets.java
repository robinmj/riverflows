package com.riverflows.wsclient;

import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Page;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.UserAccount;
import com.riverflows.data.Variable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by robin on 9/26/13.
 */
public class DestinationFacets extends WebModel<DestinationFacet>{

    private static final Log LOG = LogFactory.getLog(DestinationFacets.class);

	//singleton
	public static final DestinationFacets instance = new DestinationFacets();
	private DestinationFacets(){}

	@Override
	public String getResource() {
		return "/destination_facets";
	}

	public List<DestinationFacet> getSimilarDestinations(WsSession session, Favorite favorite) throws Exception {
		HashMap<String,List<String>> params = new HashMap<String, List<String>>();
		params.put("agency", Collections.singletonList(favorite.getSite().getAgency()));
		params.put("agency_specific_id", Collections.singletonList(favorite.getSite().getId()));
		params.put("variable_id", Collections.singletonList(favorite.getVariable()));

		Page<DestinationFacet> resultPage = get(session,params, null, null);

		return resultPage.pageElements;
	}

	public List<DestinationFacet> getFavorites(WsSession session) throws Exception {
		Page<DestinationFacet> remoteFavorites = get(session, "favorites", null, null, null);

		return remoteFavorites.pageElements;
	}

    public Favorite saveFavorite(WsSession session, int destFacetId) throws Exception {
        HttpPost postCmd = new HttpPost(DataSourceController.MY_RIVERFLOWS_WS_BASE_URL + getResource()
                + "/" + destFacetId + "/save_favorite.json?auth_token=" + session.authToken);

        HttpClient client = getHttpClientFactory().getHttpClient();

        HttpResponse httpResponse = client.execute(postCmd);

        LOG.debug(postCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

        if(httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
            throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
        }

        JSONObject responseObj = new JSONObject(Utils.getString(httpResponse.getEntity().getContent()));

        LOG.info("responseJson: " + responseObj);

        return RemoteFavorites.instance.fromJson(responseObj.getJSONObject("favorite"));
    }

    public Favorite updateFavorite(WsSession session, int destFacetId, int order) throws Exception {
        HttpPut putCmd = new HttpPut(DataSourceController.MY_RIVERFLOWS_WS_BASE_URL + getResource()
                + "/" + destFacetId + "/update_favorite.json?auth_token=" + session.authToken);
        putCmd.setEntity(new UrlEncodedFormEntity(Collections.singletonList(new BasicNameValuePair("order", order + ""))));

        HttpClient client = getHttpClientFactory().getHttpClient();

        HttpResponse httpResponse = client.execute(putCmd);

        LOG.debug(putCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

        if(httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
        }

        JSONObject responseObj = new JSONObject(Utils.getString(httpResponse.getEntity().getContent()));

        LOG.info("responseJson: " + responseObj);

        return RemoteFavorites.instance.fromJson(responseObj.getJSONObject("favorite"));
    }

    public void removeFavorite(WsSession session, int destFacetId) throws Exception {
        HttpDelete deleteCmd = new HttpDelete(DataSourceController.MY_RIVERFLOWS_WS_BASE_URL + getResource()
                + "/" + destFacetId + "/remove_favorite.json?auth_token=" + session.authToken);
        HttpClient client = getHttpClientFactory().getHttpClient();

        HttpResponse httpResponse = client.execute(deleteCmd);

        LOG.debug(deleteCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

        if(httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
        }

        JSONObject responseObj = new JSONObject(Utils.getString(httpResponse.getEntity().getContent()));

        LOG.info("responseJson: " + responseObj);
	}

	public JSONObject toJson(DestinationFacet facet) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("id", facet.getId());
		result.put("description", facet.getDescription());
		result.put("destination_id", facet.getDestination().getId());
		if(facet.getUser() != null) {
			result.put("user_id",facet.getUser().getId());
		}
		result.put("too_low",facet.getTooLow());
		result.put("low",facet.getLow());
		result.put("med",facet.getMed());
		result.put("high",facet.getHigh());
		result.put("high_plus",facet.getHighPlus());
		result.put("low_difficulty",facet.getLowDifficulty());
		result.put("med_difficulty",facet.getMedDifficulty());
		result.put("high_difficulty",facet.getHighDifficulty());
		result.put("facet_type", facet.getFacetType());

		//TODO maybe site property should be moved to DestinationFacet
		result.put("site_id", facet.getDestination().getSite().getSiteId().getPrimaryKey());

		result.put("variable_id", facet.getVariable().getId());
		result.put("low_port_difficulty",facet.getLowPortDifficulty());
		result.put("med_port_difficulty",facet.getMedPortDifficulty());
		result.put("high_port_difficulty",facet.getHighPortDifficulty());
		result.put("quality_low",facet.getQualityLow());
		result.put("quality_med",facet.getQualityMed());
		result.put("quality_high",facet.getQualityHigh());

		return result;
	}

	public DestinationFacet fromJson(JSONObject jsonObject) throws Exception {
		DestinationFacet facet = new DestinationFacet();
		if(jsonObject.has("id")) {
			facet.setId(jsonObject.getInt("id"));
		}

		facet.setDescription(jsonObject.getString("description"));
		facet.setDestination(new Destination());
		facet.getDestination().setId(jsonObject.getInt("destination_id"));
		facet.setUser(new UserAccount());
		facet.getUser().setPlaceholderObj(true);
		facet.getUser().setId(jsonObject.getInt("user_id"));
		facet.setTooLow(getDouble(jsonObject, "too_low"));
		facet.setLow(getDouble(jsonObject, "low"));
		facet.setMed(getDouble(jsonObject, "med"));
		facet.setHigh(getDouble(jsonObject, "high"));
		facet.setHighPlus(getDouble(jsonObject, "high_plus"));
		facet.setLowDifficulty(getInteger(jsonObject, "low_difficulty"));
		facet.setMedDifficulty(getInteger(jsonObject, "med_difficulty"));
		facet.setHighDifficulty(getInteger(jsonObject, "high_difficulty"));

		facet.setFacetType(jsonObject.getInt("facet_type"));

		if(jsonObject.has("site_id")) {

			Site site = new Site();
			facet.getDestination().setSite(site);

			if(jsonObject.has("site")) {
				JSONObject siteObj = jsonObject.getJSONObject("site");

				site.setSiteId(new SiteId(siteObj.getString("agency"), siteObj.getString("agency_specific_id")));
				site.setName(siteObj.getString("name"));
				site.setState(USState.valueOf(siteObj.getString("state")));
				site.setSupportedVariables(DataSourceController.getVariablesFromString(
						site.getAgency(),
						siteObj.getString("supported_var_ids")));
				site.setLatitude(siteObj.getDouble("latitude"));
				site.setLongitude(siteObj.getDouble("longitude"));
			} else {
				site.setSiteId(new SiteId("", ""));
			}
			site.getSiteId().setPrimaryKey(jsonObject.getInt("site_id"));
		}

		if(jsonObject.has("variable_id")) {
			facet.setVariable(new Variable());
			facet.getVariable().setId(jsonObject.getString("variable_id"));
		}

		facet.setLowPortDifficulty(getInteger(jsonObject, "low_port_difficulty"));
		facet.setMedPortDifficulty(getInteger(jsonObject, "med_port_difficulty"));
		facet.setHighPortDifficulty(getInteger(jsonObject, "high_port_difficulty"));

		facet.setQualityLow(getInteger(jsonObject, "quality_low"));
		facet.setQualityMed(getInteger(jsonObject, "quality_med"));
		facet.setQualityHigh(getInteger(jsonObject, "quality_high"));

		facet.setCreationDate(getDate(jsonObject, "created_at"));
		facet.setModificationDate(getDate(jsonObject, "updated_at"));

		if(jsonObject.has("destination")) {
			JSONObject jsonDestination = jsonObject.getJSONObject("destination");
			facet.getDestination().setName(jsonDestination.getString("name"));
			facet.getDestination().setCreationDate(getDate(jsonObject, "created_at"));
			facet.getDestination().setModificationDate(getDate(jsonObject, "updated_at"));
		} else {
			facet.getDestination().setPlaceholderObj(true);
			//TODO this shouldn't be necessary
			facet.getDestination().setCreationDate(facet.getCreationDate());
			facet.getDestination().setModificationDate(facet.getModificationDate());
		}

		return facet;
	}

	private Integer getInteger(JSONObject jsonObject, String property) throws JSONException {
		if(isEmpty(jsonObject, property)) {
			return null;
		}
		return jsonObject.getInt(property);
	}

	private Double getDouble(JSONObject jsonObject, String property) throws JSONException {
		if(isEmpty(jsonObject, property)) {
			return null;
		}
		return jsonObject.getDouble(property);
	}
}
