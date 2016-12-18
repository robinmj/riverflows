package com.riverflows.wsclient;

import com.riverflows.data.WrappedHttpResponse;

import java.io.IOException;

public interface HttpClientWrapper {
	
	WrappedHttpResponse doGet(String url, boolean hardRefresh) throws IOException;
}
