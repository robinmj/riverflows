package com.riverflows.wsclient;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Page;
import com.riverflows.data.USTimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by robin on 9/26/13.
 */
public abstract class WebModel<T> {

	private static final Log LOG = LogFactory.getLog(WebModel.class);

	protected static final SimpleDateFormat RAILS_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	static {
		RAILS_DATE_FORMAT.setTimeZone(USTimeZone.MDT.getTimeZone());
	}

	public final Page<T> get(WsSession session, HashMap<String, List<String>> params, Integer firstResultIndex, Integer resultCount) throws Exception {
		return getPage(session, DataSourceController.MY_RIVERFLOWS_WS_BASE_URL + getResource() + ".json", params, firstResultIndex, resultCount);
	}

	public final Page<T> get(WsSession session, String action, HashMap<String, List<String>> params, Integer firstResultIndex, Integer resultCount) throws Exception {
		return getPage(session, DataSourceController.MY_RIVERFLOWS_WS_BASE_URL + getResource() + '/' + action + ".json", params, firstResultIndex, resultCount);
	}

	private final Page<T> getPage(WsSession session, String url, HashMap<String, List<String>> params, Integer firstResultIndex, Integer resultCount) throws Exception {
		HttpGet getCmd = new HttpGet(url + "?auth_token=" + session.authToken);
		HttpClient client = new DataSourceController.SSLHttpClient();

		getCmd.addHeader("Accept", "application/json");

		HttpResponse httpResponse = client.execute(getCmd);

		LOG.info(getCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

		if(httpResponse.getStatusLine().getStatusCode() != 200) {
			throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
		}

		String responseStr = Utils.getString(httpResponse.getEntity().getContent());

		LOG.info("responseJson: " + responseStr);

		ArrayList<T> resultList = new ArrayList<T>();
		JSONArray results = new JSONArray(responseStr);

		for(int a = 0; a < results.length(); a++) {

			resultList.add(fromJson(results.getJSONObject(a)));
		}

		Page<T> resultPage = new Page<T>(resultList,resultCount);

		return resultPage;
	}

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

	protected static boolean isEmpty(JSONObject jsonObject, String property) throws JSONException {
		return !jsonObject.has(property) || jsonObject.getString(property).equals("null");
	}

	protected static Date getDate(JSONObject jsonObject, String property) throws JSONException, ParseException {
		if(isEmpty(jsonObject, property)) {
			return null;
		}
		String dateStr = jsonObject.getString(property);

		return RAILS_DATE_FORMAT.parse(dateStr);
	}

	public abstract String getResource();
	public abstract T fromJson(JSONObject json) throws Exception;
	public abstract JSONObject toJson(T obj) throws JSONException;
}
