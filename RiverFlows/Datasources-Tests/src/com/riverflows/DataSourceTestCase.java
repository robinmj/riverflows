package com.riverflows;

import com.riverflows.wsclient.BetamaxHttpClientWrapper;

import junit.framework.TestCase;

import org.junit.Rule;

import co.freeside.betamax.Recorder;

/**
 * Created by robin on 3/30/15.
 */
public class DataSourceTestCase extends TestCase {

    @Rule
    public Recorder recorder = new Recorder();

    public BetamaxHttpClientWrapper httpClientWrapper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        httpClientWrapper = new BetamaxHttpClientWrapper(recorder);
    }

    @Override
    protected void tearDown() throws Exception {
        recorder.ejectTape();
        super.tearDown();
    }
}
