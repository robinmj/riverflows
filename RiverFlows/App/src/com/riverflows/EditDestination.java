package com.riverflows;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by robin on 6/23/13.
 */
public class EditDestination extends SherlockFragmentActivity implements NumberPickerDialog.NumberPickerDialogListener {
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

	private TextView.OnFocusChangeListener levelFocusListener = new TextView.OnFocusChangeListener() {
		@Override
		public void onFocusChange(View textView, boolean hasFocus) {
			if(hasFocus && !TextUtils.isEmpty(((TextView) textView).getText())) {
				showEditDialog(textView.getId());
			}
		}
	};

	public class EditDestinationFragment extends SherlockFragment {

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

			EditText levelField = (EditText)v.findViewById(R.id.fld_too_high);
			levelField.setOnFocusChangeListener(levelFocusListener);
			levelField = (EditText)v.findViewById(R.id.fld_high);
			levelField.setOnFocusChangeListener(levelFocusListener);
			levelField = (EditText)v.findViewById(R.id.fld_medium);
			levelField.setOnFocusChangeListener(levelFocusListener);
			levelField = (EditText)v.findViewById(R.id.fld_low);
			levelField.setOnFocusChangeListener(levelFocusListener);

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
			if(validateLevels()) {
				EditDestination.this.finish();
			} else {
				mMode = startActionMode(new EditActionMode());
			}
		}
	}

	private AtomicReference<Integer> editedField = new AtomicReference<Integer>();

	private void showEditDialog(int editedField) {
		if(!this.editedField.compareAndSet(null, editedField)) {
			return;
		}

		FragmentManager fm = getSupportFragmentManager();
		NumberPickerDialog levelSelectDialog = new NumberPickerDialog();
		levelSelectDialog.setListener(this);
		levelSelectDialog.show(fm, "fragment_number_picker");
	}

	@Override
	public void onFinishNumberPicker(Float value) {

		EditText editedField = (EditText)findViewById(this.editedField.getAndSet(null));

		if(value == null) {
			editedField.setText(null);
			return;
		}

		editedField.setText(value.toString());
	}

	private boolean validateLevels() {
		Float upperLimit = validateLevelField(R.id.fld_too_high, null, null);
		upperLimit = validateLevelField(R.id.fld_high, upperLimit, null);
		upperLimit = validateLevelField(R.id.fld_medium, upperLimit, null);
		validateLevelField(R.id.fld_low, upperLimit, 0f);

		return isValid(R.id.fld_too_high) && isValid(R.id.fld_high) && isValid(R.id.fld_medium) && isValid(R.id.fld_low);
//		levelField = (EditText)findViewById(R.id.fld_too_high);
//		levelField = (EditText)findViewById(R.id.fld_high);
//		levelField = (EditText)findViewById(R.id.fld_medium);
//		levelField = (EditText)findViewById(R.id.fld_low);
	}

	private Float validateLevelField(int resId, Float upperLimit, Float lowerLimit) {
		int validLevelTextColor = getResources().getColor(android.R.color.white);

		EditText levelField = (EditText)findViewById(resId);

		int invalidColor = getResources().getColor(android.R.color.holo_red_dark);

		CharSequence contents = levelField.getText();
		if(TextUtils.isEmpty(contents)) {
			//fld_too_high is an optional field
			if(resId != R.id.fld_too_high) {
				levelField.setTextColor(invalidColor);
			}
			return null;
		}

		try {
			Float value =  new Float(contents.toString());
			if(upperLimit != null && upperLimit.compareTo(value) <= 0) {
				//mark as invalid
				levelField.setTextColor(invalidColor);
				return value;
			}
			if(lowerLimit != null && lowerLimit.compareTo(value) >= 0) {
				levelField.setTextColor(invalidColor);
				return value;
			}
			levelField.setTextColor(validLevelTextColor);
			return value;
		} catch(NumberFormatException nfe) {
			levelField.setTextColor(invalidColor);
		}
		return upperLimit;
	}

	private boolean isValid(int resId) {
		EditText levelField = (EditText)findViewById(resId);
		return levelField.getCurrentTextColor() != getResources().getColor(android.R.color.holo_red_dark);
	}
}