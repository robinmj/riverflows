package com.riverflows.wsclient;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.riverflows.Home;
import com.riverflows.data.UserAccount;

/**
 * Utility class for managing login credentials and sessions in the Mobile application
 * @author robin
 *
 */
public class WsSessionManager {

	//public static final String WS_BASE_URL = "https://ws-staging.riverflowsapp.com";
	public static final String WS_BASE_URL = "http://192.168.103.3:3000";

	public static final String AUTH_APP_URL = WS_BASE_URL + "/application/check_mobile_login";
	
	private static final String TAG = "WsSessionManager";
	
	public static final String PREFS_FILE_NAME = "main";
    public static final String PREF_REFRESH_TOKEN = "refresh_token";
    public static final String PREF_REFRESH_TOKEN_EXPIRES = "refresh_token_expires";
	public static final String PROMPTED_TO_SHARE_FAVORITES = "prompted_to_share_favorites";
	public static final String CONVERTED_FAVORITES_TO_DESTINATIONS = "converted_favorites";

	private static CopyOnWriteArraySet<SessionChangeListener> sessionListeners = new CopyOnWriteArraySet<WsSessionManager.SessionChangeListener>();
	
	private static volatile boolean promptedToLogin = false;

	private static volatile boolean promptedToRegister = false;
	private static volatile Session session = null;
	
	public static interface SessionChangeListener {
		public void onSessionChange(Session newSession, String error);
	}
	
	public static boolean addListener(SessionChangeListener listener) {
		return sessionListeners.add(listener);
	}
	
	public static boolean removeListener(SessionChangeListener listener) {
		return sessionListeners.remove(listener);
	}
	
	public static class Session implements Serializable {
		
		private static final long serialVersionUID = -872458198770348431L;
		
		public final String accountName;
		public final String authToken;
		public final long accessTokenExpires;
//		public final String refreshToken;
//		public final long refreshTokenExpires;
		public final UserAccount userAccount;

		public Session(String accountName, UserAccount account, String accessToken, long accessTokenExpires) { //, String refreshToken, long refreshTokenExpires) {
			super();
			this.accountName = accountName;
			this.authToken = accessToken;
			this.accessTokenExpires = accessTokenExpires;
			this.userAccount = account;
		}
		
		public boolean isExpired() {
			return System.currentTimeMillis() > this.accessTokenExpires;
		}
		
//		public boolean isRefreshTokenExpired() {
//			return System.currentTimeMillis() > this.refreshTokenExpires;
//		}
	}
	
	public static void notifyAccountSessionChange(Session newSession, String error) {
		WsSessionManager.session = newSession;
		
		Iterator<SessionChangeListener> notifyListeners = WsSessionManager.sessionListeners.iterator();
		
		while(notifyListeners.hasNext()) {
			notifyListeners.next().onSessionChange(newSession, error);
		}
	}
	
	public static Session getSession() {
		return session;
	}
	
	public static String getAccessToken() {
		Session tmp = WsSessionManager.session;
		return (tmp == null) ? null : tmp.authToken;
	}
	
	public static Session getWsAuthToken(String scheme, String username, String password) throws IOException, UnexpectedResultException, JSONException {
		Log.d(Home.TAG, "authenticating with Riverflows server...");

		HttpPost postCmd = new HttpPost(AUTH_APP_URL);
		HttpClient client = new DataSourceController.SSLHttpClient();
		postCmd.getParams().setParameter("http.socket.timeout", new Integer(10000));

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

		boolean anon = scheme.equals("anonymous");

		if(anon) {

			byte[] s = new byte[]{106,76,109,-26,-72,-102,7,87,71,-78,57,94,45,52,28,38,-96,-35,-41,2,-30,-17,16,-93,-52,103,127,-91,-41,38,101,13,0,121,44,-78,115,111,79,-96,101,32,-100,-51,-14,63,-70,-113,121,-14,-99,-68,2,-37,74,-53,-66,84,-51,-101,-109,-15};

			String merged = m(s);

			java j = new java(merged);

			nameValuePairs.add(new BasicNameValuePair("username", "riverflowsuser@localhost.localdomain"));
			nameValuePairs.add(new BasicNameValuePair("password", j.toString()));
		} else if(scheme.equals("google")) {
			nameValuePairs.add(new BasicNameValuePair("google_oauth2_access_token", password));
		}
		postCmd.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		postCmd.addHeader("Accept", "application/json");

		HttpResponse httpResponse = client.execute(postCmd);

		Log.d(Home.TAG, AUTH_APP_URL + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

		if(httpResponse.getStatusLine().getStatusCode() != 200) {
			throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
		}

		JSONObject responseObj = new JSONObject(Utils.getString(httpResponse.getEntity().getContent()));
		JSONObject userObj = responseObj.getJSONObject("user");

		Log.d(Home.TAG, "user: " + responseObj.getString("user"));

		UserAccount userAccount = UserAccounts.parseUser(userObj);

		String authToken = userObj.getString("authentication_token");

		session = new Session(username, userAccount, authToken, Long.MAX_VALUE);
		return session;
	}
	
	public static void logOut(Context ctx) {
		Session currentSession = session;
		if(currentSession == null) {
			return;
		}

		GoogleAuthUtil.invalidateToken(ctx.getApplicationContext(), currentSession.authToken);
		WsSessionManager.session = null;
		
		notifyAccountSessionChange(null, null);
	}
	public static boolean wasPromptedToLogin() {
		return promptedToLogin;
	}
	
	public static void setPromptedToLogin() {
		WsSessionManager.promptedToLogin = true;
	}

	public static boolean wasPromptedToRegister() {
		return promptedToRegister;
	}
	
	private static class java {
		
		private String s = null;

		public java(String s) {
			this.s = s;
		}
		
		@Override
		public String toString() {
			return s;
		}
	}
	
	private static String m(byte[] e) {
		byte[] k = new byte[]{24,37,27,-125,-54,-4,107,56,48,-63,76,45,72,70,92,74,-49,-66,-74,110,-118,-128,99,-41,-30,11,16,-58,-74,74,1,98,109,24,69,-36,73,5,43,-45,3,90,-12,-6,-109,90,-55,-8,78,-54,-17,-113,54,-77,56,-1,-115,102,-68,-84,-85,-63,-55,-2,17,-81,23,-120,-67,-12,64,108,-65,42,110,-4,11,6,85,-91,-111,-33,100,8,-118,113,54,105,21,-35,78,76,2,-12,3,93};
		byte[] r = new byte[e.length - 37];
		
		for(int a = 37; a < e.length; a++) {
			r[a - 37] = (byte)(e[a] ^ k[a]);
		}
		try {
			return new String(r, "ASCII");
		} catch(UnsupportedEncodingException uee) {
			return null;
		}
	}
	
	public static Session loginWithGoogleOAuth2(Activity activity, String accountName) throws IOException, GoogleAuthException, InterruptedException, JSONException {

   		String gToken = null;
   		
   		IOException exception = null;
		
		int timeout = 10000;
   		int waitUntilRetry = 200;

		while(gToken == null && timeout > 0) {
			try {
				gToken = GoogleAuthUtil.getToken(activity.getApplicationContext(), accountName, "oauth2:https://www.googleapis.com/auth/userinfo.email");
				Log.d(Home.TAG, "google auth2 token: " + gToken);
			} catch(IOException e) {
				exception = e;
				Log.e(Home.TAG, "could not get google oauth2 token for account " + accountName, e);

				Thread.sleep(waitUntilRetry);
				timeout -= waitUntilRetry;
				waitUntilRetry *= 2;
			}
		}

		if(gToken == null && exception != null) {
			throw exception;
		}

		if(gToken == null) {
			//exception should already be set to an IOException
			return null;
		}

		return WsSessionManager.getWsAuthToken("google", accountName, gToken);
	}

	public static void updateUserAccount(UserAccount newUserAccount) throws Exception {

		HttpPut putCmd = new HttpPut(WsSessionManager.WS_BASE_URL + "/account/update.json?auth_token=" + session.authToken);
		HttpClient client = new DataSourceController.SSLHttpClient();

		JSONObject entity = new JSONObject();

		//entity.put("auth_token", session.authToken);
		entity.put("account", UserAccounts.userAsJson(newUserAccount));

		putCmd.setEntity(new StringEntity(entity.toString()));

		putCmd.addHeader("Content-Type", "application/json");
		putCmd.addHeader("Accept", "application/json");

		HttpResponse httpResponse = client.execute(putCmd);

		Log.d(Home.TAG, putCmd + " response: " + httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());

		if(httpResponse.getStatusLine().getStatusCode() != 200) {
			throw new UnexpectedResultException(httpResponse.getStatusLine().getReasonPhrase(), httpResponse.getStatusLine().getStatusCode());
		}

		Session newSession = new Session(session.accountName, newUserAccount, session.authToken, session.accessTokenExpires);

		notifyAccountSessionChange(newSession, null);
	}
}
