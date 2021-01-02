package com.riverflows;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.riverflows.data.Favorite;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;

public class EditFavorite extends ActionBarActivity {

	public static final int RESULT_NOT_FOUND = RESULT_FIRST_USER;

	public static final String KEY_SITE_ID = "siteId";
	public static final String KEY_VARIABLE_ID = "variableId";
	
	private Favorite favorite;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    setContentView(R.layout.edit_favorite);

		Bundle extras = getIntent().getExtras();

        SiteId siteId = (SiteId)extras.get(KEY_SITE_ID);
        String variableId = (String)extras.get(KEY_VARIABLE_ID);
        
        List<Favorite> favorites = FavoritesDaoImpl.getFavorites(getApplicationContext(), siteId, variableId);

		if(favorites.size() == 0) {
			Toast.makeText(this, "The favorite " + siteId + "|" + variableId + " no longer exists",Toast.LENGTH_LONG).show();
			setResult(RESULT_NOT_FOUND);
			finish();
			return;
		}
        
        this.favorite = favorites.get(0);
        
        EditText nameField = (EditText)findViewById(R.id.favoriteName);
        if(this.favorite.getName() != null) {
        	nameField.setText(this.favorite.getName());
        } else {
        	nameField.setText(this.favorite.getSite().getName());
        }
        
        Variable[] vars = this.favorite.getSite().getSupportedVariables();
        
        RadioGroup varGroup = (RadioGroup)findViewById(R.id.variables);
        
        for(int a = 0; a < vars.length; a++) {
        	RadioButton varButton = new RadioButton(this);

            varButton.setId(2101 + a);
        	
        	if(TextUtils.isEmpty(vars[a].getCommonVariable().getUnit())) {
        		varButton.setText(vars[a].getName());
        	} else {
        		varButton.setText(vars[a].getName() + ", " + vars[a].getCommonVariable().getUnit());
        	}
        	varButton.setTag(vars[a]);
        	
        	varGroup.addView(varButton, new RadioGroup.LayoutParams(RadioGroup.LayoutParams.FILL_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT));
        	
        	if(vars[a].getId().equals(this.favorite.getVariable())) {
        		varButton.setChecked(true);
        	}
        }

        getSupportActionBar().setTitle("Edit Favorite");
    	
    	findViewById(R.id.saveButton).setOnClickListener(saveListener);
    	findViewById(R.id.cancelButton).setOnClickListener(cancelListener);
	}

	@Override
	protected void onStart() {
		super.onStart();

//		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	protected void onStop() {
		super.onStop();

//		EasyTracker.getInstance().activityStop(this);
	}
	
	private OnClickListener saveListener = new OnClickListener() {
		public void onClick(View v) {
			boolean changed = false;
			
			EditText nameField = (EditText)findViewById(R.id.favoriteName);
			if(nameField.getText().toString().trim().length() == 0) {
				nameField.setError("Please enter a name for this favorite");
				nameField.requestFocus();
				return;
			}
			
			String newName = nameField.getText().toString().trim();
			
			if(!newName.equals(EditFavorite.this.favorite.getName())) {
				EditFavorite.this.favorite.setName(newName);
				changed = true;
			}
			
			RadioGroup varGroup = (RadioGroup)findViewById(R.id.variables);
			int varButtonId = varGroup.getCheckedRadioButtonId();
			
			if(varButtonId == -1) {
				Toast.makeText(EditFavorite.this, "Please select a measurement", Toast.LENGTH_SHORT).show();
				varGroup.requestFocus();
				return;
			}
			
			Variable var = (Variable)((RadioButton)findViewById(varButtonId)).getTag();
			
			if(!favorite.getVariable().equals(var)) {
				favorite.setVariable(var.getId());
				changed = true;
			}
			
			if(changed) {
				FavoritesDaoImpl.updateFavorite(EditFavorite.this, favorite);
				setResult(RESULT_OK, new Intent(Intent.ACTION_EDIT, Uri.fromParts("riverflows",
						Favorites.FAVORITES_PATH + favorite.getId(), "")));

                sendBroadcast(Home.getWidgetUpdateIntent());
                Favorites.softReloadNeeded = true;
			} else {
				setResult(RESULT_OK, null);
			}
			finish();
		}
	};

	private OnClickListener cancelListener = new OnClickListener() {
		public void onClick(View v) {
			/*Intent i = new Intent(EditFavorite.this, Home.class);
			
			//TODO use request code to switch to the correct tab
			startActivityIfNeeded(i, -1);*/
			
			setResult(RESULT_CANCELED);
			finish();
		}
	};
}
