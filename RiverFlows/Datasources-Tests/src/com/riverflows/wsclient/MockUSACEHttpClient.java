package com.riverflows.wsclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MockUSACEHttpClient extends FileHttpClientWrapper {
	
	private static final Log LOG = LogFactory.getLog(MockUSACEHttpClient.class);
	
	private Pattern usaceDataUrlPat = Pattern.compile(USACEDataSource.SITE_DATA_URL + "shefdata2.cfm\\?sid=(\\S+)");

	public MockUSACEHttpClient() {
		super("testdata/usace/", null);
	}

	public String getFileName(String requestUrl) {

		Matcher m = usaceDataUrlPat.matcher(requestUrl);

		if(!m.matches()) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + requestUrl);
		}

		return m.group(1);
	}
}
