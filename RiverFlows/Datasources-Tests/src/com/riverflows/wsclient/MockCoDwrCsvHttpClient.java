package com.riverflows.wsclient;


public class MockCoDwrCsvHttpClient extends FileHttpClientWrapper {
	
	/**
	 * tail end of the URL that needs to be chopped off
	 */
	private static final String SUFFIX = "&START=00/00/00&END=00/00/00";
	
	public MockCoDwrCsvHttpClient(String sourceDirStr) {
		super(sourceDirStr, CODWRDataSource.SITE_DATA_URL);
	}

	@Override
	public String getFileName(String requestUrl) {
		requestUrl = super.getFileName(requestUrl);
		
		return requestUrl.substring(0, requestUrl.length() - SUFFIX.length());
	}
}
