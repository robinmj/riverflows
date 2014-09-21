package com.riverflows;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class DestinationOnboard extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.destination_onboard);
    }

	public void okClicked(View v) {
		finish();
		startActivity(new Intent(this, SetupDestinations.class));
	}

	public void cancelClicked(View v) {
		finish();
	}

}
