package com.riverflows;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.inject.Inject;
import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.ApiCallTask;
import com.riverflows.wsclient.DestinationFacets;
import com.riverflows.wsclient.Destinations;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import java.util.concurrent.atomic.AtomicReference;

import roboguice.RoboGuice;
import roboguice.activity.RoboActionBarActivity;

/**
 * Created by robin on 6/23/13.
 */
public class EditDestination extends RoboActionBarActivity {

	public static final String KEY_SITE = "site";
	public static final String KEY_VARIABLE = "variable";
    public static final String KEY_DESTINATION_FACET = "destination_facet";

	public static final int REQUEST_SAVE_DESTINATION = 23943;
	public static final int REQUEST_LOGIN_TO_SAVE_DESTINATION = 35293;

    public static final int RESULT_NOT_LOGGED_IN = RESULT_FIRST_USER;
    public static final int RESULT_NOT_DEST_FACET_OWNER = RESULT_FIRST_USER + 1;

	private EditDestinationFragment editDestination;
	private SaveDestination saveDestTask = null;

    @Inject
    private WsSessionManager wsSessionManager;

    private View.OnClickListener saveListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(editDestination.validateLevels()) {
                //save

				hideEditActionBar();
                setProgressBarIndeterminate(true);

				DestinationFacet destinationFacet= editDestination.reloadDestinationFacet();

				SaveDestination saveDestTask = new SaveDestination(false);
				EditDestination.this.saveDestTask = saveDestTask;
				saveDestTask.execute(destinationFacet);

                //if creating a favorite {
                //  sendBroadcast(Home.getWidgetUpdateIntent());
                //}
            }
        }
    };

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentManager manager = getSupportFragmentManager();

        getSupportActionBar().setTitle("New Destination");

		showEditBar();

        this.editDestination = (EditDestinationFragment)manager.findFragmentByTag("edit_destination");

		if(this.editDestination == null) {

            DestinationFacet destinationFacet = null;

            Bundle intentExtras = getIntent().getExtras();

            if(intentExtras != null) {
                destinationFacet = (DestinationFacet)intentExtras.get(KEY_DESTINATION_FACET);
            }

			Bundle extras = getIntent().getExtras();

            boolean isDestinationOwner = true;
            //boolean isDestinationFacetOwner = true;

            if(destinationFacet == null) {
                Destination destination = new Destination();
                destination.setSite((Site) extras.get(KEY_SITE));
                destination.setShared(true);
                destinationFacet = new DestinationFacet();
                destinationFacet.setDestination(destination);
                destinationFacet.setVariable((Variable) extras.get(KEY_VARIABLE));
            } else {
                WsSession session = this.wsSessionManager.getSession(this);
                if(session == null || session.userAccount == null) {
                    Log.e(App.TAG, "Could not edit destination- not logged in");
                    setResult(RESULT_NOT_LOGGED_IN);
                    finish();
                    return;
                }

                if(!session.userAccount.getId().equals(destinationFacet.getDestination().getUser().getId())) {
                    isDestinationOwner = false;
                }

                if(!session.userAccount.getId().equals(destinationFacet.getUser().getId())) {
                    //isDestinationFacetOwner = false;
                    Log.e(App.TAG, "Could not edit destination- not owner");
                    setResult(RESULT_NOT_DEST_FACET_OWNER);
                    finish();
                    return;
                }
            }

            editDestination = new EditDestinationFragment();
            Bundle arguments = new Bundle();
            arguments.putBoolean("isDestinationOwner", isDestinationOwner);
            //arguments.putBoolean("isDestinationFacetOwner", isDestinationFacetOwner);
            editDestination.setArguments(arguments);
			editDestination.setDestinationFacet(destinationFacet);

			FragmentTransaction transaction = manager.beginTransaction();
			transaction.add(android.R.id.content, editDestination, "edit_destination");
			transaction.commit();
		}
	}

    @Override
    protected void onStart() {
        super.onStart();

        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        EasyTracker.getInstance().activityStop(this);
    }

    private void showEditBar() {

		// BEGIN_INCLUDE (inflate_set_custom_view)
		// Inflate a "Done/Cancel" custom action bar view.
		final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View customActionBarView = inflater.inflate(
				R.layout.actionbar_custom_view_done_cancel, null);
		customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(saveListener);
		customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		// Show the custom action bar view and hide the normal Home icon and title.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(
				ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView,
				new ActionBar.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));
		// END_INCLUDE (inflate_set_custom_view)
	}

	private void hideEditActionBar() {
		getSupportActionBar().setDisplayShowCustomEnabled(false);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		this.saveDestTask.authorizeCallback(requestCode, resultCode, data);
	}

    public EditDestinationFragment getEditDestinationFragment() {
        return this.editDestination;
    }

	public static class EditDestinationFragment extends Fragment {

		public DestinationFacet destinationFacet;

		private Integer validTextColor = null;
		private Integer validBgColor = null;
		private Integer validHintColor = null;

		public EditDestinationFragment(){}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);

            //sync destinationFacet object with contents of text fields
            DestinationFacet destinationFacet = reloadDestinationFacet();
			outState.putSerializable(KEY_DESTINATION_FACET, destinationFacet);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

			if(savedInstanceState != null) {
				DestinationFacet savedDestFacet = (DestinationFacet)savedInstanceState.get(KEY_DESTINATION_FACET);

				if(savedDestFacet != null) {
					destinationFacet = savedDestFacet;
				}
			}

            Bundle arguments = getArguments();

            boolean isDestinationOwner = arguments.getBoolean("isDestinationOwner");
            //boolean isDestinationFacetOwner = arguments.getBoolean("isDestinationFacetOwner");

			View v =  inflater.inflate(R.layout.edit_destination, container, false);

            EditText destName = (EditText)v.findViewById(R.id.fld_dest_name);
            if(destinationFacet.getDestination().getName() != null) {
                destName.setText(destinationFacet.getDestination().getName());
            }

            if(destinationFacet.getId() != null) {
                destName.setEnabled(isDestinationOwner);
            }

			//populate station name
			TextView stationName = (TextView)v.findViewById(R.id.lbl_dest_gage);
			stationName.setText(destinationFacet.getDestination().getSite().getName());

			ImageView stationAgency = (ImageView)v.findViewById(R.id.agency_icon);
			stationAgency.setImageResource(Home.getAgencyIconResId(destinationFacet.getDestination().getSite().getAgency()));

            //populate variable name
            TextView variableName = (TextView)v.findViewById(R.id.var_name);
            variableName.setText(destinationFacet.getVariable().getName() + ", " + destinationFacet.getVariable().getUnit());

			//populate dropdown menu
			final Spinner spinner = (Spinner)v.findViewById(R.id.select_facet);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
					R.array.facet_type_names, android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);

			int[] facetValues = getResources().getIntArray(R.array.facet_type_values);
			for(int a = 0; a < facetValues.length; a++) {
				if(facetValues[a] == destinationFacet.getFacetType()) {
					spinner.setSelection(a);
					break;
				}
			}

            CheckBox publiclyVisible = (CheckBox)v.findViewById(R.id.publicly_visible);
            publiclyVisible.setChecked(destinationFacet.getDestination().isShared());
            publiclyVisible.setEnabled(isDestinationOwner);

            CompoundButton.OnCheckedChangeListener togglePublic = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    spinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            };

            publiclyVisible.setOnCheckedChangeListener(togglePublic);

            togglePublic.onCheckedChanged(publiclyVisible, publiclyVisible.isChecked());

			String unit = destinationFacet.getVariable().getUnit();

			//populate level units
			TextView levelUnit = (TextView)v.findViewById(R.id.lbl_too_high_unit);
			levelUnit.setText(unit);
			levelUnit = (TextView)v.findViewById(R.id.lbl_high_unit);
			levelUnit.setText(unit);
			levelUnit = (TextView)v.findViewById(R.id.lbl_medium_unit);
			levelUnit.setText(unit);
			levelUnit = (TextView)v.findViewById(R.id.lbl_low_unit);
			levelUnit.setText(unit);

            setLevelFieldValue(v, R.id.fld_too_high, destinationFacet.getHighPlus());
            setLevelFieldValue(v, R.id.fld_high, destinationFacet.getHigh());
            setLevelFieldValue(v, R.id.fld_medium, destinationFacet.getMed());
            setLevelFieldValue(v, R.id.fld_low, destinationFacet.getLow());

			return v;
		}

        private void setLevelFieldValue(View v, int resId, Double value) {
            if(value == null) {
                return;
            }
            EditText levelField = (EditText)v.findViewById(resId);
            levelField.setText("" + value);
        }

		private AtomicReference<Integer> editedField = new AtomicReference<Integer>();

        private void setErrorMessage(String message) {
            TextView errorMsgView = (TextView)this.getView().findViewById(R.id.lbl_error_msg);
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

            EditText levelField = (EditText)this.getView().findViewById(resId);

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
			if(validTextColor == null) {
				//nothing has been made invalid yet
				return;
			}

            field.setTextColor(validTextColor);
            field.setBackgroundColor(validBgColor);
			field.setHintTextColor(validHintColor);
        }

        private void setInvalid(EditText field) {

			validTextColor = field.getTextColors().getDefaultColor();
			validBgColor = field.getDrawingCacheBackgroundColor();
			validHintColor = field.getHintTextColors().getDefaultColor();

            int invalidTextColor = getResources().getColor(R.color.validation_error_color);
            int invalidBgColor = getResources().getColor(R.color.validation_error_bgcolor);
			int invalidHintColor = getResources().getColor(R.color.validation_error_hint_color);

            field.setTextColor(invalidTextColor);
            field.setBackgroundColor(invalidBgColor);
			field.setHintTextColor(invalidHintColor);
        }

        private boolean isValid(int resId) {
            EditText levelField = (EditText)getView().findViewById(resId);
            return levelField.getTextColors().getDefaultColor() != getResources().getColor(R.color.validation_error_color);
        }

        public DestinationFacet reloadDestinationFacet() {

            View fragView = getView();

            //destination name
            EditText destNameField = (EditText)fragView.findViewById(R.id.fld_dest_name);

            CharSequence destName = destNameField.getText();
            if(TextUtils.isEmpty(destName)) {
                destName = destinationFacet.getDestination().getSite().getName();
            }

            destinationFacet.getDestination().setName(destName.toString());

            //destination facet type
            Spinner spinner = (Spinner)fragView.findViewById(R.id.select_facet);
            spinner.getSelectedItemPosition();

            int[] facetTypes = getResources().getIntArray(R.array.facet_type_values);

            destinationFacet.setFacetType(facetTypes[spinner.getSelectedItemPosition()]);
            destinationFacet.getDestination().setShared(((CheckBox)fragView.findViewById(R.id.publicly_visible)).isChecked());

            //destination facet level definitions

            destinationFacet.setHighPlus(parseLevel(fragView, R.id.fld_too_high));
            destinationFacet.setHigh(parseLevel(fragView, R.id.fld_high));
            destinationFacet.setMed(parseLevel(fragView, R.id.fld_medium));
            destinationFacet.setLow(parseLevel(fragView, R.id.fld_low));

            return destinationFacet;
        }

        private Double parseLevel(View v, int resId) {
            EditText levelField = (EditText)v.findViewById(resId);

            CharSequence levelText = levelField.getText();

            if(TextUtils.isEmpty(levelText)) {
                return null;
            }

            try {
                return Double.parseDouble(levelText.toString());
            } catch(NumberFormatException nfe) {
                Log.e(Home.TAG, "level field " + resId, nfe);
            }
            return null;
        }

        public void setDestinationFacet(DestinationFacet destinationFacet) {
            this.destinationFacet = destinationFacet;
        }
	}

    private class SaveDestination extends ApiCallTask<DestinationFacet,Integer,DestinationFacet> {

        @Inject
        private Destinations destinations;

        @Inject
        private DestinationFacets destinationFacets;

		public SaveDestination(boolean secondTry) {
			super(EditDestination.this, REQUEST_SAVE_DESTINATION, REQUEST_LOGIN_TO_SAVE_DESTINATION, true, secondTry);
            RoboGuice.getInjector(EditDestination.this).injectMembers(this);
		}

        private SaveDestination(SaveDestination saveDestination) {
            super(saveDestination);
            RoboGuice.getInjector(EditDestination.this).injectMembers(this);
        }

        @Override
        public SaveDestination clone() {
            return new SaveDestination(this);
        }

        @Override
        protected DestinationFacet doApiCall(WsSession session, DestinationFacet... params) throws Exception {
			DestinationFacet facet = params[0];

			Destination dest = facet.getDestination();

            if(facet.getId() == null) {

                facet = destinations.saveDestinationWithFacet(session, facet);

                try {
                    Favorite newFav = destinationFacets.saveFavorite(session, facet.getId());

                    newFav.setDestinationFacet(facet);
                    newFav.setSite(facet.getDestination().getSite());
                    newFav.setVariable(facet.getVariable().getId());

                    //save new favorite locally
                    FavoritesDaoImpl.createFavorite(EditDestination.this, newFav);
                } catch (Exception e) {
                    //non-fatal exception
                    this.exception = e;
                }

            } else {
                if(facet.getDestination().getUser().getId().equals(dest.getUser().getId())) {
                    destinations.update(session, dest);
                }
                destinationFacets.update(session, facet);
            }

            return facet;
        }

		@Override
		protected void onNetworkError() {
			showEditBar();

			if(exception != null) {
				Toast.makeText(EditDestination.this, exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				Log.e(Home.TAG,"", exception);
				return;
			} else {
				Toast.makeText(EditDestination.this, "Network Error", Toast.LENGTH_LONG).show();
			}
		}

		@Override
        protected void onNoUIRequired(DestinationFacet destinationFacet) {
			showEditBar();

			setProgressBarIndeterminate(false);

			if(exception != null) {
                if(exception.getLocalizedMessage() != null) {
                    Toast.makeText(EditDestination.this, exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                Crashlytics.logException(exception);
				Log.e(Home.TAG,"", exception);
			}

            if(destinationFacet == null) {
                return;
            } else {
                sendBroadcast(Home.getWidgetUpdateIntent());
                //sendBroadcast(new Intent(Home.ACTION_FAVORITES_CHANGED));
                Favorites.softReloadNeeded = true;
            }

            Intent resultIntent = new Intent();
			resultIntent.putExtra(KEY_DESTINATION_FACET, destinationFacet);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
}