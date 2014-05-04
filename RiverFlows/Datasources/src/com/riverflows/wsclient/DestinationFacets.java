package com.riverflows.wsclient;

import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Page;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.UserAccount;
import com.riverflows.data.Variable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by robin on 9/26/13.
 */
public class DestinationFacets extends WebModel<DestinationFacet>{

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

	public DestinationFacet fromJson(JSONObject jsonObject) throws JSONException {
		DestinationFacet facet = new DestinationFacet();
		if(jsonObject.has("id")) {
			facet.setId(jsonObject.getInt("id"));
		}
		facet.setDescription(jsonObject.getString("description"));
		facet.setDestination(new Destination());
		facet.getDestination().setPlaceholderObj(true);
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
			facet.getDestination().setSite(new Site());
			facet.getDestination().getSite().setSiteId(new SiteId("",""));
			facet.getDestination().getSite().getSiteId().setPrimaryKey(jsonObject.getInt("site_id"));
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

		return facet;
	}

	private boolean isEmpty(JSONObject jsonObject, String property) throws JSONException {
		return !jsonObject.has(property) || jsonObject.getString(property).equals("null");
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
