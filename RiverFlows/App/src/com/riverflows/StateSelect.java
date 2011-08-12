package com.riverflows;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.riverflows.data.USState;

public class StateSelect extends ListActivity {
	
	public static final String TAG = Home.TAG;
	
	private TextWatcher filterFieldWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) { }

        @Override
        public void afterTextChanged(Editable s) {
            ((StateAdapter)getListAdapter()).getFilter().filter(s.toString());

        }
    };
	
	private OnFocusChangeListener filterFieldFocusListener = new OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			if(!hasFocus) {
				hideSoftKeyboard();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.state_select);
		
		StateAdapter adapter = new StateAdapter(this);
		setListAdapter(adapter);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		
		EditText stateFilterField = (EditText)findViewById(R.id.state_filter_field);
		stateFilterField.addTextChangedListener(filterFieldWatcher);
		stateFilterField.setOnFocusChangeListener(filterFieldFocusListener);
		stateFilterField.requestFocus();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		hideSoftKeyboard();
		Intent i = new Intent(this, RiverSelect.class);
        i.putExtra(RiverSelect.KEY_STATE, USState.values()[(int)id]);
        startActivity(i);
	}
	
	private class StateAdapter extends ArrayAdapter<USState> {

		public StateAdapter(Context context) {
			super(context, R.layout.state_list_item, USState.asList());
		}
		
		@Override
		public long getItemId(int position) {
			try {
				return getItem(position).ordinal();
			} catch(IndexOutOfBoundsException aioobe) {
			}
			return -1;
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.standard_menu, menu);
	    
	    menu.findItem(R.id.mi_home).setVisible(false);
	    menu.findItem(R.id.mi_reload).setVisible(false);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	hideSoftKeyboard();
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mi_about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private void hideSoftKeyboard() {
		Log.d(TAG,"hiding soft keyboard");
		
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

		EditText stateFilterField = (EditText)findViewById(R.id.state_filter_field);
		imm.hideSoftInputFromWindow(stateFilterField.getWindowToken(), 0);
	}
}
