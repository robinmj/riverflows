package com.riverflows.wsclient;

import com.riverflows.data.WrappedHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;

public class FileHttpClientWrapper implements HttpClientWrapper {
	
	private static final Log LOG = LogFactory.getLog(FileHttpClientWrapper.class);
	
	private File sourceDir;
	
	private String baseUrl;
	
	public FileHttpClientWrapper(String sourceDirStr, String baseUrl) {
		this.sourceDir = new File(sourceDirStr);
		this.baseUrl = baseUrl;
	}
	
	@Override
	public WrappedHttpResponse doGet(String requestUrl, boolean hardRefresh) throws ClientProtocolException,
			IOException {
		
		File responseFile = new File(sourceDir, getFileName(requestUrl));
		
		if(!responseFile.exists()) {
			LOG.error("no such file: " + responseFile);

			InputStream responseStream = new ByteArrayInputStream("<html><head><title>No Such File</title></head><body>No Such File</body></html>".getBytes());

			return new WrappedHttpResponse(responseStream, null, 404, "no such file: " + responseFile);
		}

		return new WrappedHttpResponse(new FileInputStream(responseFile), null, 200, null);
	}
	
	public String getFileName(String requestUrl) {
		if(!requestUrl.startsWith(baseUrl)) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + requestUrl);
		}
		
		return requestUrl.substring(this.baseUrl.length());
	}
}
