package com.riverflows.wsclient;

import com.riverflows.data.UserAccount;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by robin on 6/2/13.
 */
public class UserAccounts {

	private static final Log LOG = LogFactory.getLog(UserAccounts.class);

	private HttpClientWrapper clientWrapper;

	public static UserAccount parseUser(JSONObject userObj) throws JSONException {
		UserAccount userAccount = new UserAccount();

		LOG.debug("created_at: " + userObj.getString("created_at"));
		userAccount.setNickname(userObj.getString("nickname"));
		userAccount.setEmail(userObj.getString("email"));
		userAccount.setFirstName(userObj.getString("first_name"));
		userAccount.setLastName(userObj.getString("last_name"));
		userAccount.setFacetTypes(userObj.getInt("facet_types"));

		return userAccount;
	}

	public static JSONObject userAsJson(UserAccount userAccount) throws JSONException {
		JSONObject userObj = new JSONObject();

		userObj.put("nickname", userAccount.getNickname());
		userObj.put("email",userAccount.getEmail());
		userObj.put("first_name",userAccount.getFirstName());
		userObj.put("last_name",userAccount.getLastName());
		userObj.put("facet_types",userAccount.getFacetTypes());

		return userObj;
	}
}
