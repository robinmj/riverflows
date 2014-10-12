package com.riverflows.wsclient;

import com.riverflows.data.UserAccount;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
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
		userObj.put("email", userAccount.getEmail());
		userObj.put("first_name", userAccount.getFirstName());
		userObj.put("last_name", userAccount.getLastName());
		userObj.put("facet_types", userAccount.getFacetTypes());

		return userObj;
	}

	public static void updateUserAccount(WsSession session, UserAccount newUserAccount) throws Exception {

		HttpPut putCmd = new HttpPut(DataSourceController.MY_RIVERFLOWS_WS_BASE_URL + "/account/update.json?auth_token=" + session.authToken);
		HttpClient client = new DataSourceController.SSLHttpClient();

		JSONObject entity = new JSONObject();

		//entity.put("auth_token", session.authToken);
		entity.put("account", UserAccounts.userAsJson(newUserAccount));

		putCmd.setEntity(new StringEntity(entity.toString()));

		putCmd.addHeader("Content-Type", "application/json");
		putCmd.addHeader("Accept", "application/json");

		HttpResponse httpResponse = client.execute(putCmd);

		LOG.debug(putCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

		if(httpResponse.getStatusLine().getStatusCode() != 200) {
			throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
		}
	}
}
