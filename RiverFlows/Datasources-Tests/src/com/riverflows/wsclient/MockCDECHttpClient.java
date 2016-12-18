package com.riverflows.wsclient;

import com.riverflows.data.WrappedHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;

public class MockCDECHttpClient extends FileHttpClientWrapper {
	
	private static final Log LOG = LogFactory.getLog(MockCDECHttpClient.class);
	
	private Pattern cdecDataUrlPat = Pattern.compile(CDECDataSource.SITE_DATA_URL + "\\?s=(\\w+)&d=.+");

	public MockCDECHttpClient() {
		super("testdata/cdec/", null);
	}

	@Override
	public String getFileName(String requestUrl) {
		
		Matcher m = cdecDataUrlPat.matcher(requestUrl);
		
		if(!m.matches()) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + requestUrl);
		}
		
		return m.group(1);
	}
}
