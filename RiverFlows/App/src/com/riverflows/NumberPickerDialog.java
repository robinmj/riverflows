package com.riverflows;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockDialogFragment;

import net.simonvt.numberpicker.LogarithmicNumberPicker;

/**
 * Created by robin on 8/4/13.
 */
public class NumberPickerDialog extends SherlockDialogFragment {

	private View.OnClickListener okButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {

			LogarithmicNumberPicker np = (LogarithmicNumberPicker)getView().findViewById(R.id.number_picker);

			NumberPickerDialogListener listener = NumberPickerDialog.this.listener;
			if(listener != null) {
				listener.onFinishNumberPicker(np.getValue());
			}

			NumberPickerDialog.this.dismiss();
		}
	};

	public interface NumberPickerDialogListener {
		public void onFinishNumberPicker(Float value);
	}

	private NumberPickerDialogListener listener = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.number_picker, container, false);

		LogarithmicNumberPicker np = (LogarithmicNumberPicker)v.findViewById(R.id.number_picker);
		np.setNegativeNumbersAllowed(false);
		np.setSigFigs(1);
		np.setMaxDecimalPlaces(1);
		np.setMaxDigits(4);
		np.setFocusable(true);
		np.setFocusableInTouchMode(true);

		View okButton = v.findViewById(R.id.ok_button);

		okButton.setOnClickListener(okButtonListener);

		return v;
	}

	public void setListener(NumberPickerDialogListener listener) {
		this.listener = listener;
	}
}
