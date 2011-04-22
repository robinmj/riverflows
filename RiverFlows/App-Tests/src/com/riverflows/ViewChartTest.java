package com.riverflows;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;

import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.test.AssetAhpsXmlHttpClient;
import com.riverflows.test.AssetCoDwrCsvHttpClient;
import com.riverflows.test.AssetUsgsCsvHttpClient;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.RESTDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;

public class ViewChartTest extends ActivityInstrumentationTestCase2<ViewChart> {
	
	public static final String TAG = ViewChartTest.class.getSimpleName();
	
	public ViewChartTest() {
		super("com.riverflows", ViewChart.class);
		Log.i(TAG, "cwd: " + System.getProperty("user.dir"));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		((RESTDataSource)DataSourceController.getDataSource("USGS")).setHttpClientWrapper(new AssetUsgsCsvHttpClient(getInstrumentation().getContext().getAssets()));
		((RESTDataSource)DataSourceController.getDataSource("CODWR")).setHttpClientWrapper(new AssetCoDwrCsvHttpClient(getInstrumentation().getContext().getAssets()));
		((RESTDataSource)DataSourceController.getDataSource("AHPS")).setHttpClientWrapper(new AssetAhpsXmlHttpClient(getInstrumentation().getContext().getAssets()));
	    setActivityInitialTouchMode(false);
	}
	
	public void testArkAtTulsa() throws Throwable {
	
		Variable[] supportedVars = new Variable[]{UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS,
				UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT,
				UsgsCsvDataSource.VTYPE_WATER_TEMP_C,
				UsgsCsvDataSource.VTYPE_DISSOLVED_O2_MG_L,
				UsgsCsvDataSource.VTYPE_DCP_BATTERY_VOLTAGE};
		Site arkAtTulsa = new Site(new SiteId("USGS", "07164500"), "Arkansas River at Tulsa, OK", USState.OK, supportedVars);
		Intent viewArkAtTulsa = new Intent(getInstrumentation().getContext(), ViewChart.class);
		viewArkAtTulsa.putExtra(ViewChart.KEY_SITE, arkAtTulsa);
		viewArkAtTulsa.putExtra(ViewChart.KEY_VARIABLE, UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS);
		this.setActivityIntent(viewArkAtTulsa);
		
		TextView titleView = (TextView)getActivity().findViewById(R.id.title);
		
		assertEquals("Arkansas River at Tulsa, OK", titleView.getText());
		
		boolean completed = false;
		
		//wait until data is loaded
		for(int a = 0; a < 1000; a++) {
			Thread.sleep(100);
			TextView lastReadingView = (TextView)getActivity().findViewById(R.id.lastReading);
			if(lastReadingView.getText().length() > 0) {
				assertEquals("Last Reading: 1160 cfs, on 3/15 8:00 AM MDT", lastReadingView.getText());
				completed = true;
				break;
			}
			
			if(getActivity().errorMsg != null) {
				throw new RuntimeException("failed to load data: " + getActivity().errorMsg);
			}
		}
		
		if(!completed) {
			throw new RuntimeException("data load timed out");
		}
	}
	
	public void testAnimasAtHowardsville() throws Throwable {
		Variable[] supportedVars = new Variable[]{ CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT };
		Site animasAtHowardsville = new Site(new SiteId("CODWR", "ANIHOWCO"), "ANIMAS RIVER NEAR HOWARDSVILLE", USState.CO, supportedVars);
		Intent viewAnimasAtHowardsville = new Intent(getInstrumentation().getContext(), ViewChart.class);
		viewAnimasAtHowardsville.putExtra(ViewChart.KEY_SITE, animasAtHowardsville);
		viewAnimasAtHowardsville.putExtra(ViewChart.KEY_VARIABLE, CODWRDataSource.VTYPE_STREAMFLOW_CFS);
		this.setActivityIntent(viewAnimasAtHowardsville);
		
		TextView titleView = (TextView)getActivity().findViewById(R.id.title);
		
		assertEquals("ANIMAS RIVER NEAR HOWARDSVILLE", titleView.getText());
		
		boolean completed = false;
		
		//wait until data is loaded
		for(int a = 0; a < 1000; a++) {
			Thread.sleep(100);
			TextView lastReadingView = (TextView)getActivity().findViewById(R.id.lastReading);
			if(lastReadingView.getText().length() > 0) {
				assertEquals("Last Reading: 572 cfs, on 10/6 11:45 PM MDT", lastReadingView.getText());
				completed = true;
				break;
			}
			
			if(getActivity().errorMsg != null) {
				throw new RuntimeException("failed to load data: " + getActivity().errorMsg);
			}
		}
		
		if(!completed) {
			throw new RuntimeException("data load timed out");
		}
	}
	
	public void testCCBelowCCRes() throws Throwable {
		Variable[] supportedVars = new Variable[]{ CODWRDataSource.VTYPE_STREAMFLOW_CFS, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT };
		Site ccBelowCCRes = new Site(new SiteId("CODWR", "CCBCCRCO"), "CLEAR CREEK BELOW CLEAR CREEK RESERVOIR", USState.CO, supportedVars);
		Intent viewCCBelowCCRes = new Intent(getInstrumentation().getContext(), ViewChart.class);
		viewCCBelowCCRes.putExtra(ViewChart.KEY_SITE, ccBelowCCRes);
		viewCCBelowCCRes.putExtra(ViewChart.KEY_VARIABLE, CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT);
		this.setActivityIntent(viewCCBelowCCRes);
		
		TextView titleView = (TextView)getActivity().findViewById(R.id.title);
		
		assertEquals("CLEAR CREEK BELOW CLEAR CREEK RESERVOIR", titleView.getText());
		
		boolean completed = false;
		
		//wait until data is loaded
		for(int a = 0; a < 1000; a++) {
			Thread.sleep(100);
			TextView lastReadingView = (TextView)getActivity().findViewById(R.id.lastReading);
			if(lastReadingView.getText().length() > 0) {
				assertEquals("Last Reading: 0.6 ft, on 3/15 5:15 PM MDT", lastReadingView.getText());
				completed = true;
				break;
			}
			
			if(getActivity().errorMsg != null) {
				throw new RuntimeException("failed to load data: " + getActivity().errorMsg);
			}
		}
		
		if(!completed) {
			throw new RuntimeException("data load timed out");
		}
	}
	
	
	public void testFavoriteCheckbox() {
		Site animasAtDurango = new Site(new SiteId("AHPS", "DRGC2"), "Animas River at Durango", USState.CO, AHPSXmlDataSource.ACCEPTED_VARIABLES);
		Intent viewAnimasAtDurango = new Intent(getInstrumentation().getContext(), ViewChart.class);
		viewAnimasAtDurango.putExtra(ViewChart.KEY_SITE, animasAtDurango);
		viewAnimasAtDurango.putExtra(ViewChart.KEY_VARIABLE, AHPSXmlDataSource.VTYPE_FLOW);
		this.setActivityIntent(viewAnimasAtDurango);
		
		//make this a favorite
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
		        CheckBox favoriteBtn = (CheckBox)getActivity().findViewById(R.id.favorite_btn);
		        favoriteBtn.setChecked(true);
				//favoriteBtn.dispatchTouchEvent(MotionEvent.obtain(1000, SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, -1, -1, 0));
		        
		        //make sure the favorite was saved
		        assertTrue(FavoritesDaoImpl.isFavorite(getActivity().getApplicationContext(), new SiteId("AHPS", "DRGC2"), AHPSXmlDataSource.VTYPE_FLOW));
		        assertEquals(1, FavoritesDaoImpl.getFavorites(getActivity().getApplicationContext()).size());
			}
		});
        
	}
}
