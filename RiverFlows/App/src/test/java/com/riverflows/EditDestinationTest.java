package com.riverflows;

import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.UserAccount;
import com.riverflows.data.Variable;
import com.riverflows.factory.DestinationFacetFactory;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import roboguice.RoboGuice;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.clickOn;

/**
 * Created by robin on 11/10/14.
 */
@RunWith(RobolectricGradleTestRunner.class)
public class EditDestinationTest {

    private EditText nameField;
    private EditText highField;
    private EditText medField;
    private EditText lowField;
    private EditText tooHighField;

    private Site clearCreek = null;

    private MockWsClient wsClient = new MockWsClient();

    @Before
    public void setup() {
        RoboGuice.overrideApplicationInjector(Robolectric.application, wsClient, new RobinSession());
    }

    public EditDestination createEditDestination(Intent i) throws Exception {
        ActivityController<EditDestination> activityController= Robolectric.buildActivity(EditDestination.class);

        activityController.withIntent(i).create().start().resume().visible();

        EditDestination activity = activityController.get();

        nameField = (EditText) activity.findViewById(R.id.fld_dest_name);

        highField = (EditText) activity.findViewById(R.id.fld_high);
        medField = (EditText) activity.findViewById(R.id.fld_medium);
        lowField = (EditText) activity.findViewById(R.id.fld_low);
        tooHighField = (EditText) activity.findViewById(R.id.fld_too_high);

        return activity;
    }

    public EditDestination editNewDestination() throws Exception {
        Intent i = new Intent(Robolectric.application, EditFavorite.class);

        clearCreek = new Site();
        clearCreek.setSiteId(new SiteId(CODWRDataSource.AGENCY, "CCACCRCO"));
        clearCreek.setName("CLEAR CREEK ABOVE CLEAR CREEK RESERVOIR");
        clearCreek.setState(USState.CO);
        clearCreek.setSupportedVariables(new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT});

        i.putExtra(EditDestination.KEY_SITE, clearCreek);
        i.putExtra(EditDestination.KEY_VARIABLE, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT);

        return createEditDestination(i);
    }

    @Test
    public void shouldPopulateSite() throws Exception {

        EditDestination activity = editNewDestination();

        DestinationFacet prebuiltFacet = activity.getEditDestinationFragment().destinationFacet;

        //destination should be populated
        assertThat(prebuiltFacet.getDestination().getSite().getSiteId(), equalTo(clearCreek.getSiteId()));

        EditDestination.EditDestinationFragment frag = activity.getEditDestinationFragment();

        assertThat(frag, notNullValue());
        assertThat(frag.getView(), notNullValue());

        //site name should be displayed
        assertThat(((TextView) frag.getView().findViewById(R.id.lbl_dest_gage)).getText().toString(), equalTo(clearCreek.getName()));
    }

    @Test
    public void levelFieldsShouldBeRequired() throws Throwable {

        //the WebModels should not be modified
        doThrow(new RuntimeException()).when(wsClient.destinationsMock).update(any(WsSession.class), any(Destination.class));
        doThrow(new RuntimeException()).when(wsClient.destinationsMock).saveDestinationWithFacet(any(WsSession.class), any(DestinationFacet.class));
        doThrow(new RuntimeException()).when(wsClient.destinationFacetsMock).update(any(WsSession.class), any(DestinationFacet.class));
        doThrow(new RuntimeException()).when(wsClient.destinationFacetsMock).saveFavorite(any(WsSession.class), anyInt());

        EditDestination activity = editNewDestination();

        clickOn(activity.getSupportActionBar().getCustomView().findViewById(R.id.actionbar_done));

        //high field should be in error
        assertThat(highField.getTextColors().getDefaultColor(), equalTo(Robolectric.application.getResources().getColor(R.color.validation_error_color)));
    }

    @Test
    public void shouldSaveNewDestination() throws Throwable {

        //the WebModels should not be modified
        doThrow(new RuntimeException()).when(wsClient.destinationsMock).update(any(WsSession.class), any(Destination.class));
        doThrow(new RuntimeException()).when(wsClient.destinationFacetsMock).update(any(WsSession.class), any(DestinationFacet.class));

        DestinationFacet clearCreek = DestinationFacetFactory.getClearCreekKayak();

        when(wsClient.destinationsMock.saveDestinationWithFacet(any(WsSession.class), any(DestinationFacet.class))).thenReturn(clearCreek);
        when(wsClient.destinationFacetsMock.saveFavorite(any(WsSession.class), anyInt())).thenReturn(new Favorite(clearCreek));

        EditDestination activity = editNewDestination();

        highField.setText("" + clearCreek.getHigh().intValue());
        medField.setText("" + clearCreek.getMed().intValue());
        lowField.setText("" + clearCreek.getLow().intValue());

        clickOn(activity.getSupportActionBar().getCustomView().findViewById(R.id.actionbar_done));
    }
}
