package com.riverflows;

import android.content.Intent;
import android.widget.TextView;

import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.CODWRDataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by robin on 11/10/14.
 */
@RunWith(RobolectricTestRunner.class)
public class EditDestinationTest {

    public EditDestination createEditDestination(Intent i) throws Exception {
        ActivityController<EditDestination> activityController= Robolectric.buildActivity(EditDestination.class);

        activityController.withIntent(i).create().start().resume().visible();

        return activityController.get();
    }

    @Test
    public void shouldCreateDestinationFacetIfNoneSpecified() throws Exception {

        Intent i = new Intent(Robolectric.application, EditFavorite.class);

        Site clearCreek = new Site();
        clearCreek.setSiteId(new SiteId(CODWRDataSource.AGENCY, "CCACCRCO"));
        clearCreek.setName("CLEAR CREEK ABOVE CLEAR CREEK RESERVOIR");
        clearCreek.setState(USState.CO);
        clearCreek.setSupportedVariables(new Variable[]{CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT});

        i.putExtra(EditDestination.KEY_SITE, clearCreek);
        i.putExtra(EditDestination.KEY_VARIABLE, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT);

        EditDestination activity = createEditDestination(i);

        DestinationFacet prebuiltFacet = activity.getEditDestinationFragment().destinationFacet;

        //destination should be populated
        assertThat(prebuiltFacet.getDestination().getSite().getSiteId(), equalTo(clearCreek.getSiteId()));

        EditDestination.EditDestinationFragment frag = activity.getEditDestinationFragment();

        assertThat(frag, notNullValue());
        assertThat(frag.getView(), notNullValue());

        //destination name should be set to site name
        assertThat(((TextView)frag.getView().findViewById(R.id.lbl_dest_gage)).getText().toString(), equalTo(clearCreek.getName()));
    }
}
