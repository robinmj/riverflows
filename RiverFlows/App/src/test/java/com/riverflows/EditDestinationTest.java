package com.riverflows;

import android.content.Intent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.factory.DestinationFacetFactory;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.WsSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ActivityController;

import java.util.Date;

import roboguice.RoboGuice;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

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

        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, new RobinSession());
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
        Intent i = new Intent(RuntimeEnvironment.application, EditFavorite.class);

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

        //destination should be shared by default
        assertTrue(((CheckBox) frag.getView().findViewById(R.id.publicly_visible)).isChecked());
    }

    @Test
    public void levelFieldsShouldBeRequired() throws Throwable {

        //the WebModels should not be modified
        doThrow(new RuntimeException()).when(wsClient.destinationsMock).update(any(WsSession.class), any(Destination.class));
        doThrow(new RuntimeException()).when(wsClient.destinationsMock).saveDestinationWithFacet(any(WsSession.class), any(DestinationFacet.class));
        doThrow(new RuntimeException()).when(wsClient.destinationFacetsMock).update(any(WsSession.class), any(DestinationFacet.class));
        doThrow(new RuntimeException()).when(wsClient.destinationFacetsMock).saveFavorite(any(WsSession.class), anyInt());

        EditDestination activity = editNewDestination();

        activity.getSupportActionBar().getCustomView().findViewById(R.id.actionbar_done).performClick();

        //high field should be in error
        assertThat(highField.getTextColors().getDefaultColor(), equalTo(RuntimeEnvironment.application.getResources().getColor(R.color.validation_error_color)));
    }

    @Test
    public void shouldSaveNewDestination() throws Throwable {

        //the WebModels should not be modified
        doThrow(new RuntimeException()).when(wsClient.destinationsMock).update(any(WsSession.class), any(Destination.class));
        doThrow(new RuntimeException()).when(wsClient.destinationFacetsMock).update(any(WsSession.class), any(DestinationFacet.class));

        DestinationFacet clearCreek = DestinationFacetFactory.getClearCreekKayak();

        when(wsClient.destinationsMock.saveDestinationWithFacet(any(WsSession.class), any(DestinationFacet.class))).thenReturn(clearCreek);

        Favorite responseFavorite = new Favorite(null, null);
        responseFavorite.setId(674);
        DestinationFacet placeholderFacet = new DestinationFacet();
        placeholderFacet.setId(clearCreek.getId());
        placeholderFacet.setPlaceholderObj(true);
        responseFavorite.setDestinationFacet(placeholderFacet);
        responseFavorite.setCreationDate(new Date());

        when(wsClient.destinationFacetsMock.saveFavorite(any(WsSession.class), anyInt())).thenReturn(responseFavorite);

        EditDestination activity = editNewDestination();

        highField.setText("" + clearCreek.getHigh().intValue());
        medField.setText("" + clearCreek.getMed().intValue());
        lowField.setText("" + clearCreek.getLow().intValue());

        assertThat(!FavoritesDaoImpl.hasFavorites(activity), equalTo(true));

        activity.getSupportActionBar().getCustomView().findViewById(R.id.actionbar_done).performClick();

        assertThat(FavoritesDaoImpl.hasFavorites(activity), equalTo(true));
    }
}
