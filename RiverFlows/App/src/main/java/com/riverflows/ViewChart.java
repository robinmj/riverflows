package com.riverflows;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.widget.ProgressBar;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.inject.Inject;
import com.riverflows.data.CelsiusFahrenheitConverter;
import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.DataSourceController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import roboguice.activity.RoboActionBarActivity;

/**
 * Experimenting with using AChartEngine for displaying the hydrograph
 * @author robin
 *
 */
public class ViewChart extends RoboActionBarActivity {
	
	private static final String TAG = Home.TAG;
	
	public static final String GAUGE_SCHEME = "gauge";
	
	public static final String KEY_SITE = "site";
	public static final String KEY_VARIABLE = "variable";

	public static final int DIALOG_ID_LOADING_ERROR = 1;

    @Inject
    private DataSourceController dataSourceController;
	
	private SiteFragment siteFragment;

	private FetchHydrographTask runningShareTask = null;
	private HashMap<CommonVariable, CommonVariable> conversionMap = new HashMap<CommonVariable, CommonVariable>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        String title = null;

        Site site = null;
        Variable variable = null;
        
        if(getIntent().getData() == null) {
			
			Bundle extras = getIntent().getExtras();
	        
	        site = (Site)extras.get(KEY_SITE);
	        variable = (Variable)extras.get(KEY_VARIABLE);

            if(variable != null) {
                List<Favorite> favorites = FavoritesDaoImpl.getFavorites(getApplicationContext(), site.getSiteId(), variable.getId());

                if(favorites.size() > 0) {
                    title = favorites.get(0).getName();
                }
            }

        } else {
        	SiteId siteId = new SiteId(getIntent().getData().getSchemeSpecificPart());
			List<Favorite> favorites = FavoritesDaoImpl.getFavorites(getApplicationContext(), siteId, getIntent().getData().getFragment());

        	site = favorites.get(0).getSite();

            title = favorites.get(0).getName();

        	variable = DataSourceController.getVariable(site.getAgency(), getIntent().getData().getFragment());
        }

        if(TextUtils.isEmpty(title)) {
            title = site.getName();
        }

        FragmentManager manager = getSupportFragmentManager();

        getSupportActionBar().setTitle(title);
		
		SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, MODE_PRIVATE);
    	String tempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);
    	
    	//Log.d(Home.TAG, "saved unit: " + tempUnit);
    	conversionMap = CommonVariable.temperatureConversionMap(tempUnit);

        this.siteFragment = (SiteFragment)manager.findFragmentByTag("site");

        if(this.siteFragment == null) {
            this.siteFragment = new SiteFragment();
            Bundle arguments = new Bundle();
            arguments.putBoolean(SiteFragment.ARG_ZERO_Y_MIN, false);
            arguments.putSerializable(SiteFragment.ARG_SITE, site);
            arguments.putSerializable(SiteFragment.ARG_VARIABLE, variable);
            arguments.putSerializable(SiteFragment.ARG_CONVERSION_MAP, this.conversionMap);
            this.siteFragment.setArguments(arguments);

            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(android.R.id.content, this.siteFragment, "site");
            transaction.commit();
        }
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
    	EasyTracker.getInstance().activityStart(this);

    	Tracker tracker =  EasyTracker.getTracker();

        Site site = this.getSite();

        tracker.setCustomDimension(1, "" + site.getState());
        tracker.setCustomDimension(2, site.getAgency());
        tracker.setCustomDimension(3, site.getId());
        if(this.getVariable() == null) {
            tracker.setCustomDimension(4, null);
            tracker.setCustomDimension(5, null);
        } else {
            tracker.setCustomDimension(4, this.getVariable().getId());
            tracker.setCustomDimension(5, this.getVariable().getCommonVariable().name());
        }
    }

    public Site getSite() {
        return this.siteFragment.getSite();
    }

    public Variable getVariable() {
        return this.siteFragment.getVariable();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	
    	EasyTracker.getInstance().activityStop(this);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.standard_menu, menu);
        
        MenuItem otherVarsItem = menu.findItem(R.id.mi_other_variables);
        otherVarsItem.setVisible(true);
		otherVarsItem.setEnabled(ViewChart.this.getSite().getSupportedVariables().length > 1);
        
        MenuItem unitsItem = menu.findItem(R.id.mi_change_units);
        unitsItem.setVisible(true);
        
        if(this.getVariable() != null && conversionMap.containsKey(this.getVariable().getCommonVariable())) {
        	unitsItem.setEnabled(true);
        }
		
        MenuItem shareItem = menu.findItem(R.id.mi_share);
        shareItem.setVisible(true);
        shareItem.setEnabled(true);
        
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	
    	MenuItem otherVariablesItem = menu.findItem(R.id.mi_other_variables);
    	
    	SubMenu otherVarsMenu = otherVariablesItem.getSubMenu();
    	otherVarsMenu.clear();
        boolean hasOtherVariables = populateOtherVariablesSubmenu(otherVarsMenu);
    	otherVariablesItem.setVisible(hasOtherVariables);
    	otherVariablesItem.setEnabled(hasOtherVariables);
        
    	MenuItem changeUnitsItem = menu.findItem(R.id.mi_change_units);
    	
        SubMenu unitsMenu = changeUnitsItem.getSubMenu();
        unitsMenu.clear();
        boolean unitConversionsAvailable = populateUnitsSubmenu(unitsMenu);
        changeUnitsItem.setVisible(unitConversionsAvailable);
        changeUnitsItem.setEnabled(unitConversionsAvailable);
        
    	return super.onPrepareOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mi_home:
	    	startActivityIfNeeded(new Intent(this, Home.class), -1);
	    	return true;
	    case R.id.mi_share:
	        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
	        progressBar.setVisibility(View.VISIBLE);
	    	new FetchHydrographTask(this).execute();
	    	return true;
	    case R.id.mi_about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
	        return true;
	    case R.id.mi_reload:
	    	this.siteFragment.reload();
	    	return true;
	    case R.id.mi_other_variables:
	    case R.id.mi_change_units:
	    	if(this.siteFragment != null) {
		    	return true;
	    	}
	    	return false;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private static final int REQUEST_SHARE = 61294;

	private static class FetchHydrographTask extends AsyncTask<String, Integer, File> {
		
		private String graphUrl = null;
		private File savedFile = null;
		private ViewChart activity = null;
        private Site site = null;
        private Variable variable = null;
		
		public FetchHydrographTask(ViewChart activity) {
            this.site = activity.siteFragment.getSite();
            this.variable = activity.siteFragment.getVariable();

			EasyTracker.getTracker().sendSocial("ACTION_SEND", "start", this.site.getAgency() + ":" + this.site.getId());
			
			if(this.variable != null) {
				graphUrl = DataSourceController.getDataSource(this.site.getAgency()).getExternalGraphUrl(activity.getSite().getId(), this.variable.getId());
			}
			this.activity = activity;
			activity.runningShareTask = this;
		}
    	
    	public void setActivity(ViewChart activity) {
    		this.activity = activity;
    	}
		
	    @Override
	    protected File doInBackground(String... arg0) {
	    	if(graphUrl == null) {
	    		return null;
	    	}
	    	
	    	Bitmap b = null;
	    	try {
				b = BitmapFactory.decodeStream((InputStream) new URL(graphUrl).getContent());
				
				String file_name = "share_hydrograph.png";

		        //use this instead once minSdkVersion is set to 8 or above
		        //File png = new File(activity.getExternalCacheDir(), file_name);
		        File png = new File(new File(Environment.getExternalStorageDirectory(), "Pictures"), file_name);
	            
	            Log.i(Home.TAG, "saving to " + png);
	            
		        FileOutputStream out = null;
		        try {
		            out = new FileOutputStream(png);
		            b.compress(Bitmap.CompressFormat.PNG, 100, out);
		            
		            out.flush();
		        } catch (Exception e) {
		            Log.e(Home.TAG,"", e);
		            
					EasyTracker.getTracker().sendException("saving " + graphUrl, e, false);
					
		            return null;
		        } finally {
		            try {
		                if (out != null) out.close();
		            }
		            catch (IOException ignore) {}
		        }
				return png;
			} catch (MalformedURLException e) {
				Log.e(Home.TAG, graphUrl,e);
			} catch (IOException e) {
				Log.w(Home.TAG, graphUrl,e);
				
				EasyTracker.getTracker().sendException("downloading " + graphUrl, e, false);
			} 
	        return null;
	    }
	    
	    @Override
	    protected void onPostExecute(File result) {
	    	this.savedFile = result;
	    	
	        ProgressBar progressBar = (ProgressBar)activity.findViewById(R.id.progressBar);
	        progressBar.setVisibility(View.GONE);
			
			Intent intent=new Intent(android.content.Intent.ACTION_SEND);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			
			String varName = "";
			
			if(this.variable != null) {
				varName = this.variable.getCommonVariable().getName() + " at ";
			}
			
			intent.putExtra(Intent.EXTRA_SUBJECT, varName + this.site.getName());
			
			if(result != null) {
				//share with embedded image
		    	
		    	Uri graphUri = Uri.fromFile(result);
		    	
		    	Log.i(Home.TAG, "file URI: " + graphUri);
				
				intent.setType("image/png");
				intent.putExtra(android.content.Intent.EXTRA_TEXT,
				        "Shared using the RiverFlows mobile app");
				intent.putExtra(android.content.Intent.EXTRA_STREAM, graphUri);
				activity.startActivityForResult(Intent.createChooser(intent, "Share Hydrograph"), REQUEST_SHARE);

				EasyTracker.getTracker().sendSocial("ACTION_SEND", "image", this.site.getAgency() + ":" + this.site.getId());
		    	
		    	/*
		    	 * decided not to use this because it doesn't look very good in email and doesn't include RiverFlows branding
		    	else {
					intent.setType("text/plain");
					intent.putExtra(android.content.Intent.EXTRA_TEXT,
					        graphUrl);
					startActivity(Intent.createChooser(intent, "Share Hydrograph"));
					
					return;
				} */
			} else {
			
				//send email with embedded link
				String graphLink = DataSourceController.getDataSource(this.site.getAgency()).getExternalSiteUrl(this.site.getId());
				graphLink = "<a href=\"" + graphLink + "\">" + this.site.getName() + "</a>";
				
				intent.setType("message/rfc822") ;
				
				Log.v(Home.TAG, "footer: " + activity.getString(R.string.email_share_footer));
				
				intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(graphLink + activity.getString(R.string.email_share_footer)));
				activity.startActivityForResult(Intent.createChooser(intent, "Email Link"), REQUEST_SHARE);

				EasyTracker.getTracker().sendSocial("ACTION_SEND", "email", this.site.getAgency() + ":" + this.site.getId());
			}
	    }
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == REQUEST_SHARE) {

			EasyTracker.getTracker().sendSocial("ACTION_SEND", "completed", getSite().getAgency() + ":" + getSite().getId());
			
			if(this.runningShareTask == null) {
				return;
			}
			
			File savedFile = this.runningShareTask.savedFile;
			this.runningShareTask = null;

			Log.i(Home.TAG, "deleting file: " + savedFile);
			
			//remove hydrograph file that was saved during share
			if(savedFile == null || !savedFile.delete()) {
				Log.e(Home.TAG, "couldn't delete file: " + savedFile);
			}
		}
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.graph_menu, menu);

        Boolean zeroYMin = this.siteFragment.zeroYMin;
		
		if((zeroYMin == null && this.getVariable().getCommonVariable().isGraphAgainstZeroMinimum()) || (zeroYMin != null && zeroYMin)) {
			MenuItem fitYAxisItem = menu.findItem(R.id.mi_fit_y_axis);
			fitYAxisItem.setVisible(true);
		} else {
			MenuItem zeroYMinItem = menu.findItem(R.id.mi_zero_y_minimum);
			zeroYMinItem.setVisible(true);
		}
		
        if(this.getSite().getSupportedVariables().length > 1) {
            MenuItem otherVarsMenuItem = menu.findItem(R.id.sm_other_variables);
            otherVarsMenuItem.setVisible(true);
            populateOtherVariablesSubmenu(otherVarsMenuItem.getSubMenu());
        }
        
        String[] toUnit = new CelsiusFahrenheitConverter().convertsTo(this.getVariable().getUnit());
        
        if(toUnit.length > 0) {
	        MenuItem unitsMenuItem = menu.findItem(R.id.sm_change_units);
	        unitsMenuItem.setVisible(true);
	        populateUnitsSubmenu(unitsMenuItem.getSubMenu());
        }
	}
	
	private boolean populateOtherVariablesSubmenu(SubMenu otherVariablesMenu) {
        otherVariablesMenu.setHeaderTitle(R.string.variable_context_menu_title);
        
		Variable[] otherVariables = this.getSite().getSupportedVariables();
		
		if(otherVariables.length <= 1) {
			return false;
		}

		if(this.getVariable() == null) {
			return false;
		}
		
        try {
			for(int a = 0; a < otherVariables.length; a++) {
				if(otherVariables[a] == null) {
					throw new NullPointerException("otherVariables[" + a + "]");
				}
				
				//hide currently displayed variable
				if(this.getVariable().equals(otherVariables[a])) {
					continue;
				}
				if(TextUtils.isEmpty(otherVariables[a].getCommonVariable().getUnit())) {
					otherVariablesMenu.add(otherVariables[a].getName())
						.setOnMenuItemClickListener(new OtherVariableClickListener(otherVariables[a]));
				} else {
					otherVariablesMenu.add(otherVariables[a].getName() + ", " + otherVariables[a].getCommonVariable().getUnit())
						.setOnMenuItemClickListener(new OtherVariableClickListener(otherVariables[a]));
				}
			}
        } catch(NullPointerException npe) {
        	//TODO remove this once we find the source of the NPE
        	throw new RuntimeException("data: " + this.getSite().getSiteId() + " vars: " + otherVariables,npe);
        }
        return true;
	}
	
	private boolean populateUnitsSubmenu(SubMenu unitsMenu) {
        unitsMenu.setHeaderTitle("Units");

        if(this.getVariable() == null) {
            return false;
        }
        
        CommonVariable displayedVariable = conversionMap.get(this.getVariable().getCommonVariable());
        
        if(displayedVariable == null) {
        	displayedVariable = this.getVariable().getCommonVariable();
        }
        
        String[] toUnit = new CelsiusFahrenheitConverter().convertsTo(displayedVariable.getUnit());
        
        if(toUnit.length == 0) {
        	return false;
        }
		
		for(int a = 0; a < toUnit.length; a++) {
			unitsMenu.add(toUnit[a])
					.setOnMenuItemClickListener(new UnitClickListener(toUnit[a]));
		}
		return true;
	}

    private class OtherVariableClickListener implements OnMenuItemClickListener {
        private final Variable var;

        public OtherVariableClickListener(Variable var) {
            this.var = var;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            ViewChart.this.siteFragment.setVariable(var);

            return true;
        }
    }

    private class UnitClickListener implements OnMenuItemClickListener {
        private final String unit;

        public UnitClickListener(String unit) {
            this.unit = unit;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return ViewChart.this.siteFragment.setTempUnit(unit);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.mi_zero_y_minimum:
                this.siteFragment.setZeroYMin(true);
                return true;
            case R.id.mi_fit_y_axis:
                this.siteFragment.setZeroYMin(false);
                return true;
        }

        return true;
    }
}
