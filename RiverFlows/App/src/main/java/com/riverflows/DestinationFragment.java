package com.riverflows;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.riverflows.data.DestinationFacet;

/**
 * Created by robin on 11/23/13.
 */
public class DestinationFragment extends Fragment {

	private DestinationFacet destinationFacet;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.destination, container, false);
	}
}
