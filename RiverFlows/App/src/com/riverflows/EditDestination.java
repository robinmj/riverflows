package com.riverflows;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Site;
import com.riverflows.data.Variable;

/**
 * Created by robin on 6/23/13.
 */
public class EditDestination extends SherlockFragmentActivity {
	private static final int MAIN_LAYOUT_ID = 8631537;

	public static final String KEY_SITE = "site";
	public static final String KEY_VARIABLE = "variable";

	private ActionMode mMode;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentManager manager = getSupportFragmentManager();

		if(manager.findFragmentByTag("edit_destination") == null) {
			Bundle extras = getIntent().getExtras();

			EditDestinationFragment editDestination = new EditDestinationFragment();
			editDestination.station = (Site)extras.get(KEY_SITE);
			editDestination.variable = (Variable)extras.get(KEY_VARIABLE);

			FragmentTransaction transaction = manager.beginTransaction();
			transaction.add(android.R.id.content, editDestination, "edit_destination");
			transaction.commit();
		}

		getSupportActionBar().setTitle("New Destination");

		mMode = startActionMode(new EditActionMode());
	}

	public static class EditDestinationFragment extends SherlockFragment {

		public Site station;
		public Variable variable;
		public DestinationFacet destinationFacet;

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putSerializable(KEY_SITE, station);
			outState.putSerializable(KEY_VARIABLE, variable);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

			if(savedInstanceState != null) {
				Site savedStation = (Site)savedInstanceState.get(KEY_SITE);
				Variable savedVariable = (Variable)savedInstanceState.get(KEY_VARIABLE);

				if(savedStation != null) {
					station = savedStation;
					variable = savedVariable;
				}
			}

			View v =  inflater.inflate(R.layout.edit_destination, container, false);

			//populate station name
			TextView stationName = (TextView)v.findViewById(R.id.lbl_dest_gage);
			stationName.setText(station.getName());

			ImageView stationAgency = (ImageView)v.findViewById(R.id.agency_icon);
			stationAgency.setImageResource(Home.getAgencyIconResId(station.getAgency()));

			//populate dropdown menu
			Spinner spinner = (Spinner)v.findViewById(R.id.select_facet);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getSherlockActivity(),
					R.array.facet_type_names, android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);


			String unit = variable.getUnit();

			//populate level units
			TextView levelUnit = (TextView)v.findViewById(R.id.lbl_too_high_unit);
			levelUnit.setText(unit);
			levelUnit = (TextView)v.findViewById(R.id.lbl_high_unit);
			levelUnit.setText(unit);
			levelUnit = (TextView)v.findViewById(R.id.lbl_medium_unit);
			levelUnit.setText(unit);
			levelUnit = (TextView)v.findViewById(R.id.lbl_low_unit);
			levelUnit.setText(unit);

			return v;
		}
	}

	private void save() {
		//if creating a favorite {
		//  sendBroadcast(Home.getWidgetUpdateIntent());
		//}
	}


	private final class EditActionMode implements ActionMode.Callback {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {

			getSupportMenuInflater().inflate(R.menu.edit_account_action_mode, menu);

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch(item.getItemId()) {
				case R.id.ai_cancel:
					//cancel()
					break;
			}
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			EditDestination.this.finish();
		}
	}
}