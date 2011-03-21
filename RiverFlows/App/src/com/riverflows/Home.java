package com.riverflows;

import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

import com.riverflows.db.DbMaintenance;
import com.riverflows.db.FavoritesDaoImpl;

public class Home extends TabActivity {
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    //Logger.getLogger("").setLevel(Level.WARNING);
	    
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.main);

	    Resources res = getResources();
	    TabHost tabHost = getTabHost();
	    TabHost.TabSpec spec;
	    Intent intent;

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, StateSelect.class);
	    spec = tabHost.newTabSpec("browse").setIndicator("Browse by State",
	                      res.getDrawable(R.drawable.ic_tab_browse))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, Favorites.class);
	    spec = tabHost.newTabSpec("favorites").setIndicator("Favorites",
	                      res.getDrawable(R.drawable.ic_tab_favorites))
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    DbMaintenance.upgradeIfNecessary(getApplicationContext());
	    
	    if(FavoritesDaoImpl.hasFavorites(getCurrentActivity())) {
	    	tabHost.setCurrentTab(1);
	    } else {
	    	tabHost.setCurrentTab(0);
	    }
	}
}
