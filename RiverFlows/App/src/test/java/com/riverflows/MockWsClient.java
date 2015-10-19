package com.riverflows;

import com.google.inject.AbstractModule;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.DestinationFacets;
import com.riverflows.wsclient.Destinations;

import static org.mockito.Mockito.mock;

/**
 * Created by robin on 11/17/14.
 */
public class MockWsClient extends AbstractModule {

    public DataSourceController dsControllerMock = mock(DataSourceController.class);
    public Destinations destinationsMock = mock(Destinations.class);
    public DestinationFacets destinationFacetsMock = mock(DestinationFacets.class);

    @Override
    protected void configure() {
        bind(Destinations.class).toInstance(destinationsMock);
        bind(DestinationFacets.class).toInstance(destinationFacetsMock);
        bind(DataSourceController.class).toInstance(dsControllerMock);
    }
}
