package com.riverflows.data;

import java.io.File;
import java.io.InputStream;

/**
 * Created by robin on 12/17/16.
 */
public class WrappedHttpResponse {
    public final InputStream responseStream;
    public final File cacheFile;
    public final int statusCode;
    public final String message;

    public WrappedHttpResponse(InputStream responseStream, File cacheFile, int statusCode, String message) {
        this.responseStream = responseStream;
        this.cacheFile = cacheFile;
        this.statusCode = statusCode;
        this.message = message;
    }
}
