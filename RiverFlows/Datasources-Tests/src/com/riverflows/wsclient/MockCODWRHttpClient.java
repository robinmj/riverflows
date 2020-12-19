package com.riverflows.wsclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by robin on 10/13/19.
 */

public class MockCODWRHttpClient extends FileHttpClientWrapper {

    private Pattern codwrDataUrlPat = Pattern.compile(CODWRDataSource.SITE_DATA_URL + "\\?s=(\\w+)&d=.+");

    public MockCODWRHttpClient() {
        super("testdata/codwr/", null);
    }

    @Override
    public String getFileName(String requestUrl) {

        Matcher m = codwrDataUrlPat.matcher(requestUrl);

        if(!m.matches()) {
            throw new IllegalArgumentException("url not supported by this mock http client: " + requestUrl);
        }

        return m.group(1);
    }
}
