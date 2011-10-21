package com.riverflows;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class WidgetAd extends Activity  implements OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.widget_ad);
		
		TextView pitch = (TextView)findViewById(R.id.pitch);
		
		SpannableString ss = new SpannableString("Make your first 5 favorite gauge sites visible from your phone's desktop!  Now for sale on the Android Market.");
		ss.setSpan(new URLSpan("market://details?id=com.riverflows"), 75, 109, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		pitch.setText(ss);
		pitch.setMovementMethod(LinkMovementMethod.getInstance());
		
		View closeButton = (View)findViewById(R.id.close_link);
		
		closeButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		finish();
	}
}
