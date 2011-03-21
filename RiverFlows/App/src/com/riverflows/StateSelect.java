package com.riverflows;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.riverflows.data.USState;

public class StateSelect extends ListActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setListAdapter(new StateAdapter(this));

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Toast.makeText(getApplicationContext(), R.string.list_filter_tip, Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
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
}
