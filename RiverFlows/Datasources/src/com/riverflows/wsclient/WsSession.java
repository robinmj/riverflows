package com.riverflows.wsclient;

import com.riverflows.data.UserAccount;

import java.io.Serializable;

/**
* Created by robin on 9/26/13.
*/
public class WsSession implements Serializable {

	private static final long serialVersionUID = -872458198770348431L;

	public final String accountName;
	public final String authToken;
	public final long accessTokenExpires;
//		public final String refreshToken;
//		public final long refreshTokenExpires;
	public final UserAccount userAccount;

	public WsSession(String accountName, UserAccount account, String accessToken, long accessTokenExpires) { //, String refreshToken, long refreshTokenExpires) {
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
