package com.riverflows;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager.BadTokenException;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.SitesDaoImpl;
import com.riverflows.view.HydroGraph;
import com.riverflows.wsclient.DataParseException;
import com.riverflows.wsclient.DataSourceController;

/**
 * Experimenting with using AChartEngine for displaying the hydrograph
 * @author robin
 *
 */
public class ViewChart extends Activity {
	
	private static final String TAG = Home.TAG;
	
	public static final String GAUGE_SCHEME = "gauge";
	
	public static final String KEY_SITE = "site";
	public static final String KEY_VARIABLE = "variable";

	public static final int DIALOG_ID_LOADING_ERROR = 1;
	
	private Site station;
	private Boolean zeroYMin = null;
	private Variable variable;
	private LinearLayout chartLayout;
	private HydroGraph chartView;
	private GenerateDataSetTask runningTask = null;
	SiteData data;
	String errorMsg;

	private static final SimpleDateFormat lastReadingDateFmt = new SimpleDateFormat("M/d h:mm aa zzz");
	
    private class FavoriteButtonListener implements CompoundButton.OnCheckedChangeListener {
    	
    	@Override
    	public void onCheckedChanged(CompoundButton buttonView,
    			boolean isChecked) {
    		if(isChecked) {
    			Favorite f = new Favorite(ViewChart.this.station, ViewChart.this.variable.getId());
    			FavoritesDaoImpl.createFavorite(getApplicationContext(), f);
    		} else {
    			FavoritesDaoImpl.deleteFavorite(getApplicationContext(), ViewChart.this.station.getSiteId(), ViewChart.this.variable);
    		}
			sendBroadcast(Home.getWidgetUpdateIntent());
    	}
    }
    
    private class DataSrcInfoButtonListener implements OnClickListener {
    	@Override
    	public void onClick(View v) {
			Intent viewOriginalData = new Intent(ViewChart.this, DataSrcInfo.class);
			viewOriginalData.putExtra(DataSrcInfo.KEY_INFO, ViewChart.this.data.getDataInfo());
			startActivity(viewOriginalData);
	        return;
    	}
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    	
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_chart);
        
        //LinearLayout layout = null;
        //((Windowlayout.getLayoutParams().
        
        if(getIntent().getData() == null) {
			
			Bundle extras = getIntent().getExtras();
	        
	        this.station = (Site)extras.get(KEY_SITE);
	        this.variable = (Variable)extras.get(KEY_VARIABLE);
        } else {
        	SiteId siteId = new SiteId(getIntent().getData().getSchemeSpecificPart());
        	List<SiteData> sites = SitesDaoImpl.getSites(getApplicationContext(), Collections.singletonList(siteId));
        	this.station = sites.get(0).getSite();
        	this.variable = DataSourceController.getVariable(this.station.getAgency(), getIntent().getData().getFragment());
        }
        
        TextView titleText = (TextView)findViewById(R.id.title);
        titleText.setText(station.getName());
        
        CheckBox favoriteBtn = (CheckBox)findViewById(R.id.favorite_btn);
        favoriteBtn.setVisibility(View.GONE);
        
        chartLayout = (LinearLayout) findViewById(R.id.chart);

        //see onRetainNonConfigurationInstance()
    	final Object data = getLastNonConfigurationInstance();
        
    	if(data == null) {
    		//make the request for site data
    		new GenerateDataSetTask(this, false).execute(this.station);
    	} else {
    		Object[] prevState = (Object[])data;
    		this.variable = (Variable)prevState[0];
    		this.data = (SiteData)prevState[1];
    		this.runningTask = (GenerateDataSetTask)prevState[2];
    		this.zeroYMin = (Boolean)prevState[3];
    		clearData();
    		if(runningTask != null) {
    			this.runningTask.setActivity(this);
    		}
    		//use stored data instead
    		if(this.data != null) {
    			displayData();
    		}
    	}
    }
    
    private void clearData() {
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        CheckBox favoriteBtn = (CheckBox)findViewById(R.id.favorite_btn);
        favoriteBtn.setOnCheckedChangeListener(null);
        favoriteBtn.setVisibility(View.GONE);
        chartLayout.removeView(chartView);
        this.errorMsg = null;
    }
    
    private void reloadData() {
        clearData();
        new GenerateDataSetTask(this, true).execute(this.station);
    }
    
    private void displayData() {
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
        CheckBox favoriteBtn = (CheckBox)findViewById(R.id.favorite_btn);
		
		if(this.data == null || errorMsg != null) {
			if(errorMsg == null) {
				errorMsg = "Error: No Data";
			}
			try {
				showDialog(DIALOG_ID_LOADING_ERROR);
			} catch(BadTokenException bte) {
				if(Log.isLoggable(TAG, Log.INFO)) {
					Log.i(TAG, "can't display dialog; activity no longer active");
				}
				return;
			}
            progressBar.setVisibility(View.GONE);
            initContingencyFavoriteBtn(favoriteBtn);
            return;
		}
		
		String siteAgency = this.data.getSite().getAgency();
		
		ImageView dataSrcInfoButton = (ImageView)findViewById(R.id.dataSrcInfoB);
		
		if(ViewChart.this.data.getDataInfo() != null) {
			dataSrcInfoButton.setVisibility(View.VISIBLE);
			dataSrcInfoButton.setOnClickListener(new DataSrcInfoButtonListener());
			
			Integer agencyIconResId = Home.getAgencyIconResId(siteAgency);
			
	        if(agencyIconResId != null) {
	        	dataSrcInfoButton.setImageResource(agencyIconResId);
	        } else {
	        	Log.e(TAG, "no icon for agency: " + siteAgency);
	        	dataSrcInfoButton.setVisibility(View.GONE);
	        }
		} else {
        	dataSrcInfoButton.setVisibility(View.GONE);
		}
		
		Series displayedSeries = null;
		
		if(this.variable != null) {
			displayedSeries = this.data.getDatasets().get(this.variable.getCommonVariable());
		}
    	if(displayedSeries == null) {
    		displayedSeries = DataSourceController.getPreferredSeries(this.data);
    	}
    	
    	if(displayedSeries == null || displayedSeries.getReadings().size() == 0) {
			if(errorMsg == null) {
				errorMsg = "Error: No Data";
			}
			showDialog(DIALOG_ID_LOADING_ERROR);
            progressBar.setVisibility(View.GONE);
            favoriteBtn.setVisibility(View.VISIBLE);
            initContingencyFavoriteBtn(favoriteBtn);
            return;
    	}
    	
    	if(this.variable == null) {
    		this.variable = displayedSeries.getVariable();
    	}

        Reading mostRecentReading = displayedSeries.getLastObservation();
        
        Date mostRecentReadingTime = mostRecentReading.getDate();
        
        String mostRecentReadingStr = null;
        String unit = null;
        
        if(mostRecentReading.getValue() != null) {
        	mostRecentReadingStr = mostRecentReading.getValue() + "";
	        //get rid of unnecessary digits
	        if(mostRecentReadingStr.endsWith(".0")) {
	        	mostRecentReadingStr = mostRecentReadingStr.substring(0, mostRecentReadingStr.length() - 2);
	        }
	        
	        unit = displayedSeries.getVariable().getUnit();
	        if(unit == null) {
	        	unit = "";
	        }
	        if(unit.trim().length() > 0) {
	        	unit = " " + unit;
	        }
        } else {
        	mostRecentReadingStr = "unknown";
        	if(mostRecentReading.getQualifiers() != null) {
        		unit = ": " + mostRecentReading.getQualifiers();
        	} else {
	        	unit = "";
        	}
        }
        
        TextView lastReading = (TextView)ViewChart.this.findViewById(R.id.lastReading);
        lastReading.setText("Last Reading: " + mostRecentReadingStr + unit + ", on " + lastReadingDateFmt.format(mostRecentReadingTime));
        
        chartView = new HydroGraph(ViewChart.this);
		
		if(zeroYMin == null) {
			//automatically determine the y-axis ranging mode using the variable
			chartView.setSeries(displayedSeries, variable.getCommonVariable().isGraphAgainstZeroMinimum());
		} else if(zeroYMin) {
			chartView.setSeries(displayedSeries, true);
		} else {
			chartView.setSeries(displayedSeries, false);
		}
        
        chartLayout.addView(chartView, new LayoutParams(LayoutParams.FILL_PARENT,
            LayoutParams.FILL_PARENT));
        
        //this can't be called until ViewChart.this.chartView and ViewChart.this.variable have been initialized
    	registerForContextMenu(chartView);
		
        favoriteBtn.setVisibility(View.VISIBLE);
        favoriteBtn.setChecked(isFavorite());
        favoriteBtn.setOnCheckedChangeListener(new FavoriteButtonListener());
        progressBar.setVisibility(View.GONE);
    }
	
	/**
	 * try our damnedest to initialize the variable property so the user can mark this
	 * site as a favorite even if it doesn't have data right now.
	 */
	private void initContingencyFavoriteBtn(CheckBox favoriteBtn) {
		if(this.station.getSupportedVariables().length > 0) {
			this.variable = this.station.getSupportedVariables()[0];
	        favoriteBtn.setOnCheckedChangeListener(new FavoriteButtonListener());
            favoriteBtn.setVisibility(View.VISIBLE);
		}
	}
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{variable,data,runningTask,zeroYMin};
    }

    @Override
    protected void onResume() {
      super.onResume();
      if (chartView != null) {
        //chartView.repaint();
      }
    }
    
    private static class GenerateDataSetTask extends AsyncTask<Site, Integer, SiteData> {
    	
		private String errorMsg = null;
		private ViewChart activity;
		private Variable variable;
		private Site site;
		private boolean hardRefresh = false;
    	
    	public GenerateDataSetTask(ViewChart activity, boolean hardRefresh) {
			super();
    		this.activity = activity;
    		this.activity.runningTask = this;
    		this.variable = this.activity.variable;
    		this.site = this.activity.station;
    		this.hardRefresh = hardRefresh;
		}
    	
    	public void setActivity(ViewChart activity) {
    		this.activity = activity;
    	}

		@Override
    	protected SiteData doInBackground(Site... params) {
			SiteData result = null;
    		Site station = params[0];
    		
            try {            	
            	Variable[] variables = site.getSupportedVariables();
        		if(variables.length == 0) {
        			if(variable != null) {
                		variables = new Variable[]{variable};
                	} else {
	        			//TODO send user to update site
	        			errorMsg = "Sorry, this version of RiverFlows doesn't support any of the gauges at '" + station.toString() + "'";
	        			return null;
                	}
        		} else if(variable != null) {
        			//ensure that the specified variable comes first so it is
        			// guaranteed that the datasource will attempt to retrieve its data.
        			boolean found = false;
        			for(int a = 0; a < variables.length; a++) {
        				if(variables[a].equals(variable)) {
        					variables[a] = variables[0];
        					variables[0] = variable;
        					found = true;
        					break;
        				}
        			}
        			if(!found) {
        				Log.e(TAG, "could not find " + variable.getId() + " in supported vars for " + site.getSiteId());
        				variables = new Variable[]{variable};
        			}
            	}
        		
        		return DataSourceController.getSiteData(site, variables, this.hardRefresh);
            } catch(UnknownHostException uhe) {
            	errorMsg = "Lost network connection.";
            } catch(IOException ioe) {
            	errorMsg = "Could not retrieve site data: an I/O error has occurred.";
            	Log.e(TAG, station.getId(), ioe);
            } catch(DataParseException dpe) {
            	errorMsg = "Could not process data from " + station + "; " + dpe.getMessage();
            	Log.e(TAG, station.toString(), dpe);
            } catch(Exception e) {
            	errorMsg = "Error loading data from " + station + "; " + e.getMessage();
            	Log.e(TAG, station.toString(), e);
            }
            return result;
    	}
		
		@Override
		protected void onPostExecute(SiteData result) {
			this.activity.errorMsg = errorMsg;
			this.activity.data = result;
			this.activity.displayData();
			
			this.activity.runningTask = null;
		}
    }
    
    private boolean isFavorite() {

		if(!FavoritesDaoImpl.isFavorite(getApplicationContext(), this.station.getSiteId(), this.variable)) {
			return false;
		}

    	FavoritesDaoImpl.updateLastViewedTime(getApplicationContext(), this.station.getSiteId());
    	
    	return true;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.standard_menu, menu);
        
        MenuItem otherVarsItem = menu.findItem(R.id.mi_other_variables);
        otherVarsItem.setVisible(true);
		otherVarsItem.setEnabled(ViewChart.this.station.getSupportedVariables().length > 1);
		
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	SubMenu otherVarsMenu = menu.findItem(R.id.mi_other_variables).getSubMenu();
    	otherVarsMenu.clear();
        populateOtherVariablesSubmenu(otherVarsMenu);
    	return super.onPrepareOptionsMenu(menu);
    }
	
	private class ErrorMsgDialog extends AlertDialog {
		public ErrorMsgDialog(Context context, String msg) {
			super(context);
			setMessage(msg);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_ID_LOADING_ERROR:
			ErrorMsgDialog errorDialog = new ErrorMsgDialog(this, this.errorMsg);
			return errorDialog;
		}
		return null;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mi_home:
	    	startActivityIfNeeded(new Intent(this, Home.class), -1);
	    	return true;
	    case R.id.mi_about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
	        return true;
	    case R.id.mi_reload:
	    	reloadData();
	    	return true;
	    case R.id.mi_other_variables:
	    	if(chartView != null) {
		    	return true;
	    	}
	    	return false;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.graph_menu, menu);
		
		if((zeroYMin == null && variable.getCommonVariable().isGraphAgainstZeroMinimum()) || (zeroYMin != null && zeroYMin)) {
			MenuItem fitYAxisItem = menu.findItem(R.id.mi_fit_y_axis);
			fitYAxisItem.setVisible(true);
		} else {
			MenuItem zeroYMinItem = menu.findItem(R.id.mi_zero_y_minimum);
			zeroYMinItem.setVisible(true);
		}
		
        if(ViewChart.this.station.getSupportedVariables().length <= 1) {
        	return;
        }
        
        MenuItem otherVarsMenuItem = menu.findItem(R.id.sm_other_variables);
        otherVarsMenuItem.setVisible(true);
        populateOtherVariablesSubmenu(otherVarsMenuItem.getSubMenu());
	}
	
	private void populateOtherVariablesSubmenu(SubMenu otherVariablesMenu) {
        otherVariablesMenu.setHeaderTitle(R.string.variable_context_menu_title);
		
		Variable[] otherVariables = ViewChart.this.station.getSupportedVariables();
		for(int a = 0; a < otherVariables.length; a++) {
			if(variable.equals(otherVariables[a])) {
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
	}
	
	private class OtherVariableClickListener implements OnMenuItemClickListener {
		private final Variable var;
		
		public OtherVariableClickListener(Variable var) {
			this.var = var;
		}
		
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			
			try {
				ViewChart.this.variable = var;
			} catch(ArrayIndexOutOfBoundsException aioobe) {
				Log.w(TAG,"no variable at index " + item.getItemId());
				return false;
			}
			
			if(ViewChart.this.data.getDatasets().get(ViewChart.this.variable.getCommonVariable()) == null) {
				reloadData();
			} else {
				clearData();
				displayData();
			}
			return true;
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		switch(item.getItemId()) {
		case R.id.mi_zero_y_minimum:
			this.zeroYMin = true;
			clearData();
			displayData();
			return true;
		case R.id.mi_fit_y_axis:
			this.zeroYMin = false;
			clearData();
			displayData();
			return true;
		}
        
        return true;
	}
}
