package com.riverflows.wsclient;

import com.riverflows.data.Page;
import com.riverflows.data.UserAccount;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by robin on 6/2/13.
 */
public class UserAccounts extends WebModel<UserAccount> {

	private static final Log LOG = LogFactory.getLog(UserAccounts.class);

	private HttpClientWrapper clientWrapper;

    public static UserAccounts instance = new UserAccounts();

    @Override
    public String getResourceName() {
        return "account";
    }

    @Override
    public String getResource() {
        return "/" + getResourceName();
    }

    public UserAccount fromJson(JSONObject userObj) throws JSONException {
		UserAccount userAccount = new UserAccount();

		LOG.debug("created_at: " + userObj.getString("created_at"));
        userAccount.setId(userObj.getInt("id"));
		userAccount.setNickname(userObj.getString("nickname"));
		userAccount.setEmail(userObj.getString("email"));
		userAccount.setFirstName(userObj.getString("first_name"));
		userAccount.setLastName(userObj.getString("last_name"));
		userAccount.setFacetTypes(userObj.getInt("facet_types"));

		return userAccount;
	}

	public JSONObject toJson(UserAccount userAccount) throws JSONException {
		JSONObject userObj = new JSONObject();

		userObj.put("nickname", userAccount.getNickname());
		userObj.put("email", userAccount.getEmail());
		userObj.put("first_name", userAccount.getFirstName());
		userObj.put("last_name", userAccount.getLastName());
		userObj.put("facet_types", userAccount.getFacetTypes());

		return userObj;
	}

    public UserAccount get(WsSession session) throws Exception {
        HttpGet getCmd = new HttpGet(DataSourceController.RIVERFLOWS_WS_BASEURL + getResource() + ".json?auth_token=" + session.authToken);
        HttpClient client = getHttpClientFactory().getHttpClient();

        getCmd.addHeader("Accept", "application/json");

        HttpResponse httpResponse = client.execute(getCmd);

        LOG.info(getCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

        if(httpResponse.getStatusLine().getStatusCode() != 200) {
            throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
        }

        String responseStr = Utils.getString(httpResponse.getEntity().getContent());

        LOG.info("responseJson: " + responseStr);

        return fromJson(new JSONObject(responseStr).getJSONObject("user"));
    }

    //TODO remove this- the #update() instance method does the same thing
	public static void updateUserAccount(WsSession session, UserAccount newUserAccount) throws Exception {

		HttpPut putCmd = new HttpPut(DataSourceController.RIVERFLOWS_WS_BASEURL + "/account/update.json?auth_token=" + session.authToken);
		HttpClient client = new DataSourceController.SSLHttpClient();

		JSONObject entity = new JSONObject();

		//entity.put("auth_token", session.authToken);
		entity.put("account", UserAccounts.instance.toJson(newUserAccount));

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
