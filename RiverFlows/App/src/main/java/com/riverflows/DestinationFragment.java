package com.riverflows;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.riverflows.data.DestinationFacet;
import com.riverflows.R;

/**
 * Created by robin on 11/23/13.
 */
public class DestinationFragment extends SherlockFragment {

	private DestinationFacet destinationFacet;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.destination, container, false);
	}
}
