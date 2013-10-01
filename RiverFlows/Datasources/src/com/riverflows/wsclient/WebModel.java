package com.riverflows.wsclient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by robin on 9/26/13.
 */
public abstract class WebModel<T> {

	private static final Log LOG = LogFactory.getLog(WebModel.class);

	public final T create(WsSession session, T obj) throws Exception{
		HttpPost postCmd = new HttpPost(DataSourceController.MY_RIVERFLOWS_WS_BASE_URL + getResource() + ".json?auth_token=" + session.authToken);
		HttpClient client = new DataSourceController.SSLHttpClient();

		postCmd.setEntity(new StringEntity(getCreateEntity(obj).toString()));

		postCmd.addHeader("Content-Type", "application/json");
		postCmd.addHeader("Accept", "application/json");

		HttpResponse httpResponse = client.execute(postCmd);

		LOG.debug(postCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

		if(httpResponse.getStatusLine().getStatusCode() != 200) {
			throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
		}

		JSONObject responseObj = new JSONObject(Utils.getString(httpResponse.getEntity().getContent()));

		LOG.debug("responseJson: " + responseObj);

		//JSONObject destJson = responseObj.getJSONObject(getJSONObjectKey());

		return fromJson(responseObj);
	}

	public JSONObject getCreateEntity(T obj) throws JSONException {
		return toJson(obj);
	}

	public abstract String getResource();
	public abstract T fromJson(JSONObject json) throws JSONException;
	public abstract JSONObject toJson(T obj) throws JSONException;
}
