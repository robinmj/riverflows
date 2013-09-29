package com.riverflows;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
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
public class EditDestination extends SherlockFragmentActivity {
	private static final int MAIN_LAYOUT_ID = 8631537;

	public static final String KEY_SITE = "site";
	public static final String KEY_VARIABLE = "variable";

	private ActionMode mMode;
	private EditDestinationFragment editDestination;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentManager manager = getSupportFragmentManager();

		if(manager.findFragmentByTag("edit_destination") == null) {
			Bundle extras = getIntent().getExtras();

			editDestination = new EditDestinationFragment();
			editDestination.station = (Site)extras.get(KEY_SITE);
			editDestination.variable = (Variable)extras.get(KEY_VARIABLE);

			FragmentTransaction transaction = manager.beginTransaction();
			transaction.add(android.R.id.content, editDestination, "edit_destination");
			transaction.commit();
		}

		getSupportActionBar().setTitle("New Destination");

		mMode = startActionMode(new EditActionMode());
	}

	public static class EditDestinationFragment extends SherlockFragment implements NumberPickerDialog.NumberPickerDialogListener {

		public Site station;
		public Variable variable;
		public DestinationFacet destinationFacet;

		private TextView.OnFocusChangeListener levelFocusListener = new TextView.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View textView, boolean hasFocus) {
				if(hasFocus && !TextUtils.isEmpty(((TextView) textView).getText())) {
					showEditDialog(textView.getId());
				}
			}
		};

		public EditDestinationFragment(){}

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

		private AtomicReference<Integer> editedField = new AtomicReference<Integer>();

		private void showEditDialog(int editedField) {
			if(!this.editedField.compareAndSet(null, editedField)) {
				return;
			}

			FragmentManager fm = getActivity().getSupportFragmentManager();
			NumberPickerDialog levelSelectDialog = new NumberPickerDialog();
			levelSelectDialog.setListener(this);
			levelSelectDialog.show(fm, "fragment_number_picker");
		}

		@Override
		public void onFinishNumberPicker(Float value) {

			EditText editedField = (EditText)getView().findViewById(this.editedField.getAndSet(null));

			if(value == null) {
				editedField.setText(null);
				return;
			}

			editedField.setText(value.toString());
		}
	}



	@Override
	public void onActionModeFinished(ActionMode mode) {
		super.onActionModeFinished(mode);

		EditText destNameField = (EditText)findViewById(R.id.fld_dest_name);

		CharSequence destName = destNameField.getText();
		if(TextUtils.isEmpty(destName)) {
			destName = editDestination.station.getName();
		}

//		EditText levelField = (EditText)findViewById(R.id.fld_too_high);
//		levelField = (EditText)findViewById(R.id.fld_high);
//		levelField = (EditText)findViewById(R.id.fld_medium);
//		levelField = (EditText)findViewById(R.id.fld_low);

		//if creating a favorite {
		//  sendBroadcast(Home.getWidgetUpdateIntent());
		//}
	}

	private final class EditActionMode implements ActionMode.Callback {

		private boolean save = true;

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
			save = false;
			switch(item.getItemId()) {
				case R.id.ai_cancel:
					//nothing specific to do here yet
					break;
			}
			mode.finish();
			return true;
		}



		@Override
		public void onDestroyActionMode(ActionMode mode) {
			if(!save || validateLevels()) {
				EditDestination.this.finish();
			} else {
				mMode = startActionMode(new EditActionMode());
			}
		}
	}

	private void setErrorMessage(String message) {
		TextView errorMsgView = (TextView)findViewById(R.id.lbl_error_msg);
		if(message == null) {
			errorMsgView.setVisibility(View.GONE);
			return;
		}
		errorMsgView.setText(message);
		errorMsgView.setVisibility(View.VISIBLE);
	}

	private boolean validateLevels() {
		Float upperLimit = validateLevelField(R.id.fld_too_high, null, null);
		upperLimit = validateLevelField(R.id.fld_high, upperLimit, null);
		upperLimit = validateLevelField(R.id.fld_medium, upperLimit, null);
		validateLevelField(R.id.fld_low, upperLimit, 0f);

		if(isValid(R.id.fld_high) && isValid(R.id.fld_medium) && isValid(R.id.fld_low)) {
			return true;
		}

		setErrorMessage("Missing or invalid level definition(s)");

		return false;
	}

	private Float validateLevelField(int resId, Float upperLimit, Float lowerLimit) {

		EditText levelField = (EditText)findViewById(resId);

		CharSequence contents = levelField.getText();
		if(TextUtils.isEmpty(contents)) {
			//fld_too_high is an optional field
			if(resId == R.id.fld_too_high) {
				setValid(levelField);
			} else {
				setInvalid(levelField);
			}
			return null;
		}

		try {
			Float value =  new Float(contents.toString());
			if(upperLimit != null && upperLimit.compareTo(value) <= 0) {
				//mark as invalid
				setInvalid(levelField);
				return value;
			}
			if(lowerLimit != null && lowerLimit.compareTo(value) >= 0) {
				setInvalid(levelField);
				return value;
			}
			setValid(levelField);
			return value;
		} catch(NumberFormatException nfe) {
			setInvalid(levelField);
		}
		return upperLimit;
	}

	private void setValid(EditText field) {
		int validTextColor = getResources().getColor(android.R.color.white);
		int validBgColor = getResources().getColor(android.R.color.black);

		field.setTextColor(validTextColor);
		field.setBackgroundColor(validBgColor);
	}

	private void setInvalid(EditText field) {
		int invalidTextColor = getResources().getColor(R.color.validation_error_color);
		int invalidBgColor = getResources().getColor(R.color.validation_error_bgcolor);

		field.setTextColor(invalidTextColor);
		field.setBackgroundColor(invalidBgColor);
	}

	private boolean isValid(int resId) {
		EditText levelField = (EditText)findViewById(resId);
		return levelField.getCurrentTextColor() != getResources().getColor(R.color.validation_error_color);
	}
}