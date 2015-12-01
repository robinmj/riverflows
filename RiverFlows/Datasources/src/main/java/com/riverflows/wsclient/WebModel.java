package com.riverflows.wsclient;

import com.riverflows.data.Page;
import com.riverflows.data.RemoteObject;
import com.riverflows.data.USTimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robin on 9/26/13.
 */
public abstract class WebModel<T extends RemoteObject> {

	private static final Log LOG = LogFactory.getLog(WebModel.class);

	protected static final SimpleDateFormat RAILS_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final String REQUEST_ENCODING = "UTF-8";

	static {
		RAILS_DATE_FORMAT.setTimeZone(USTimeZone.MDT.getTimeZone());
	}

    private static HttpClientFactory httpClientFactory = new SSLHttpClientFactory();

    public static HttpClientFactory getHttpClientFactory() {
        return httpClientFactory;
    }
    public static void setHttpClientFactory(HttpClientFactory source) {
        WebModel.httpClientFactory = source;
    }

	public Page<T> get(WsSession session, Map<String, List<String>> params, Integer firstResultIndex, Integer resultCount) throws Exception {
		return getPage(session, DataSourceController.RIVERFLOWS_WS_BASEURL + getResource() + ".json", params, firstResultIndex, resultCount);
	}

	public Page<T> get(WsSession session, String action, Map<String, List<String>> params, Integer firstResultIndex, Integer resultCount) throws Exception {
		return getPage(session, DataSourceController.RIVERFLOWS_WS_BASEURL + getResource() + '/' + action + ".json", params, firstResultIndex, resultCount);
	}

	private final Page<T> getPage(WsSession session, String url, Map<String, List<String>> params, Integer firstResultIndex, Integer resultCount) throws Exception {

        //insert authToken into params
        if(params == null) {
            params = new HashMap<String, List<String>>();
        } else {
            params = new HashMap<String, List<String>>(params);
        }
        List prevAuthToken = params.put("auth_token", Collections.singletonList(session.authToken));

        //params shouldn't contain anything called auth_token
        assert(prevAuthToken == null);

        if(firstResultIndex != null) {
            List prevFirstParam = params.put("first", Collections.singletonList(firstResultIndex.toString()));
            assert(prevFirstParam == null);
        }
        if(resultCount != null) {
            List prevCountParam = params.put("count", Collections.singletonList(resultCount.toString()));
            assert(prevCountParam == null);
        }

		HttpGet getCmd = new HttpGet(url + generateQueryString(params));
		HttpClient client = httpClientFactory.getHttpClient();

		getCmd.addHeader("Accept", "application/json");

		HttpResponse httpResponse = client.execute(getCmd);

		LOG.info(getCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

		if(httpResponse.getStatusLine().getStatusCode() != 200) {
			throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
		}

		String responseStr = Utils.getString(httpResponse.getEntity().getContent());

		LOG.info("responseJson: " + responseStr);

        ArrayList<T> resultList = fromJsonArray(new JSONArray(responseStr));

		Page<T> resultPage = new Page<T>(resultList,resultCount);

		return resultPage;
	}

    private String generateQueryString(Map<String, List<String>> params) throws UnsupportedEncodingException {

        if(params == null) {
            return "";
        }

        StringBuilder queryString = new StringBuilder();

        for(Map.Entry<String, List<String>> entry : params.entrySet()) {
            for(String value : entry.getValue()) {
                queryString.append(URLEncoder.encode(entry.getKey(),REQUEST_ENCODING));
                queryString.append("=");
                queryString.append(URLEncoder.encode(value, REQUEST_ENCODING));
                queryString.append("&");
            }
        }
        if(queryString.length() == 0) {
            return "";
        }

        //chop off last ampersand
        queryString.setLength(queryString.length() - 1);

        return  "?" + queryString.toString();
    }

    public T get(WsSession session, Integer id) throws Exception {
        if(id == null) {
            throw new NullPointerException();
        }

        HttpGet getCmd = new HttpGet(DataSourceController.RIVERFLOWS_WS_BASEURL + getResource() + '/' + id + ".json?auth_token=" + session.authToken);
        HttpClient client = httpClientFactory.getHttpClient();

        getCmd.addHeader("Accept", "application/json");

        HttpResponse httpResponse = client.execute(getCmd);

        LOG.info(getCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

        if(httpResponse.getStatusLine().getStatusCode() != 200) {
            throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
        }

        String responseStr = Utils.getString(httpResponse.getEntity().getContent());

        LOG.info("responseJson: " + responseStr);

        return fromJson(new JSONObject(responseStr));
    }

	public T create(WsSession session, T obj) throws Exception{
		HttpPost postCmd = new HttpPost(DataSourceController.RIVERFLOWS_WS_BASEURL + getResource() + ".json?auth_token=" + session.authToken);
		HttpClient client = getHttpClientFactory().getHttpClient();

        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put(getResourceName(), getCreateEntity(obj));

        postCmd.setEntity(new StringEntity(jsonEntity.toString()));

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

    public void update(WsSession session, T obj) throws Exception {
        if(obj.getId() == null) {
            throw new NullPointerException();
        }

        HttpPut putCmd = new HttpPut(DataSourceController.RIVERFLOWS_WS_BASEURL + getResource() + "/" + obj.getId() + ".json?auth_token=" + session.authToken);
        HttpClient client = getHttpClientFactory().getHttpClient();

        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put(getResourceName(), getCreateEntity(obj));

        putCmd.setEntity(new StringEntity(jsonEntity.toString()));

        putCmd.addHeader("Content-Type", "application/json");
        putCmd.addHeader("Accept", "application/json");

        HttpResponse httpResponse = client.execute(putCmd);

        LOG.debug(putCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

        if(httpResponse.getStatusLine().getStatusCode() != 204) {
            throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
        }
    }

    public ArrayList<T> fromJsonArray(JSONArray jsonArray) throws Exception {
        ArrayList<T> resultList = new ArrayList<T>();

        for(int a = 0; a < jsonArray.length(); a++) {

            resultList.add(fromJson(jsonArray.getJSONObject(a)));
        }

        return resultList;
    }

	public JSONObject getCreateEntity(T obj) throws JSONException {
		return toJson(obj);
	}

	protected static boolean isEmpty(JSONObject jsonObject, String property) throws JSONException {
		return !jsonObject.has(property) || jsonObject.isNull(property);
	}

	protected static Date getDate(JSONObject jsonObject, String property) throws JSONException, ParseException {
		if(isEmpty(jsonObject, property)) {
			return null;
		}
		String dateStr = jsonObject.getString(property);

		return RAILS_DATE_FORMAT.parse(dateStr);
	}

    protected static Object getObject(JSONObject jsonObj, String key) throws JSONException {
        if(isEmpty(jsonObj, key)) {
            return null;
        }
        return jsonObj.get(key);
    }

    protected static String getString(JSONObject jsonObj, String key) throws JSONException {
        if(isEmpty(jsonObj, key)) {
            return null;
        }
        return jsonObj.getString(key);
    }

    protected Integer getInteger(JSONObject jsonObject, String property) throws JSONException {
        if(isEmpty(jsonObject, property)) {
            return null;
        }
        return jsonObject.getInt(property);
    }

    protected void putInteger(JSONObject jsonObject, String property, Integer integer) throws JSONException {
        if(integer == null) {
            jsonObject.put(property, JSONObject.NULL);
            return;
        }
        jsonObject.put(property, integer);
    }

    protected Double getDouble(JSONObject jsonObject, String property) throws JSONException {
        if(isEmpty(jsonObject, property)) {
            return null;
        }
        return jsonObject.getDouble(property);
    }

    protected void putDouble(JSONObject jsonObject, String property, Double doub) throws JSONException {
        if(doub == null) {
            jsonObject.put(property, JSONObject.NULL);
            return;
        }
        jsonObject.put(property, doub);
    }

    public String getResource() {
        return "/" + getResourceName() + "s";
    }

    public abstract String getResourceName();
	public abstract T fromJson(JSONObject json) throws Exception;
	public abstract JSONObject toJson(T obj) throws JSONException;

}
