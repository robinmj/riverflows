package com.riverflows.wsclient;

/**
 * Support for injection of HTTP client
 * @author robin
 *
 */
public interface RESTDataSource extends DataSource {
	public HttpClientWrapper getHttpClientWrapper();
	public void setHttpClientWrapper(HttpClientWrapper source);
}
