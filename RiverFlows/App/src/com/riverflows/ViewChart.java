package com.riverflows;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.view.HydroGraph;
import com.riverflows.wsclient.DataParseException;
import com.riverflows.wsclient.DataSourceController;

/**
 * Experimenting with using AChartEngine for displaying the hydrograph
 * @author robin
 *
 */
public class ViewChart extends Activity {
	
	private static final String TAG = ViewChart.class.getSimpleName();
	
	public static final String KEY_SITE = "site";
	public static final String KEY_VARIABLE = "variable";

	public static final int DIALOG_ID_LOADING_ERROR = 1;
	
	private Site station;
	private Variable variable;
	private LinearLayout chartLayout;
	private HydroGraph chartView;
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
    	}
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		
		Bundle extras = getIntent().getExtras();

		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_chart);
        
        //LinearLayout layout = null;
        //((Windowlayout.getLayoutParams().
        
        this.station = (Site)extras.get(KEY_SITE);
        this.variable = (Variable)extras.get(KEY_VARIABLE);
        
        TextView titleText = (TextView)findViewById(R.id.title);
        titleText.setText(station.getName());
        
        CheckBox favoriteBtn = (CheckBox)findViewById(R.id.favorite_btn);
        favoriteBtn.setVisibility(View.GONE);
        
        chartLayout = (LinearLayout) findViewById(R.id.chart);

        //see onRetainNonConfigurationInstance()
    	final Object data = getLastNonConfigurationInstance();
        
    	if(data == null) {
    		//make the request for site data
    		new GenerateDataSetTask().execute(this.station);
    	} else {
    		Object[] prevState = (Object[])data;
    		this.variable = (Variable)prevState[0];
    		this.data = (SiteData)prevState[1];
    		//use stored data instead
    		displayData();
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
        new GenerateDataSetTask().execute(this.station);
    }
    
    private void displayData() {
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
        CheckBox favoriteBtn = (CheckBox)findViewById(R.id.favorite_btn);
		
		if(this.data == null || errorMsg != null) {
			if(errorMsg == null) {
				errorMsg = "Error: No Data";
			}
			showDialog(DIALOG_ID_LOADING_ERROR);
            progressBar.setVisibility(View.GONE);
            initContingencyFavoriteBtn(favoriteBtn);
            return;
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

        Reading mostRecentReading = DataSourceController.getLastObservation(displayedSeries);
        
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
        chartView.setSeries(displayedSeries);
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
        return new Object[]{variable,data};
    }

    @Override
    protected void onResume() {
      super.onResume();
      if (chartView != null) {
        //chartView.repaint();
      }
    }
    
    private class GenerateDataSetTask extends AsyncTask<Site, Integer, SiteData> {
    	
		private String errorMsg = null;
    	
    	public GenerateDataSetTask() {
			super();
		}

		@Override
    	protected SiteData doInBackground(Site... params) {
    		Site station = params[0];
    		
            try {            	
            	Variable[] variables = ViewChart.this.station.getSupportedVariables();
        		if(variables.length == 0) {
        			if(ViewChart.this.variable != null) {
                		variables = new Variable[]{ViewChart.this.variable};
                	} else {
	        			//TODO send user to update site
	        			errorMsg = "Sorry, this version of RiverFlows doesn't support any of the gauges at '" + station.toString() + "'";
	        			return null;
                	}
        		} else if(ViewChart.this.variable != null) {
        			//ensure that the specified variable comes first so it is
        			// guaranteed that the datasource will attempt to retrieve its data.
        			boolean found = false;
        			for(int a = 0; a < variables.length; a++) {
        				if(variables[a].equals(ViewChart.this.variable)) {
        					variables[a] = variables[0];
        					variables[0] = ViewChart.this.variable;
        					found = true;
        					break;
        				}
        			}
        			if(!found) {
        				Log.e(TAG, "could not find " + ViewChart.this.variable.getId() + " in supported vars for " + ViewChart.this.station.getSiteId());
        				variables = new Variable[]{ViewChart.this.variable};
        			}
            	}
            	
                return DataSourceController.getSiteData(ViewChart.this.station, variables);
            } catch(UnknownHostException uhe) {
            	errorMsg = "Lost network connection.";
            	return null;
            } catch(IOException ioe) {
            	errorMsg = "Could not retrieve site data: an I/O error has occurred.";
            	Log.e(TAG, station.getId(), ioe);
                return null;
            } catch(DataParseException dpe) {
            	errorMsg = "Could not process data from " + station + "; " + dpe.getMessage();
            	Log.e(TAG, station.toString(), dpe);
            	return null;
            }
    	}
		
		@Override
		protected void onPostExecute(SiteData result) {
			ViewChart.this.errorMsg = errorMsg;
			ViewChart.this.data = result;
			ViewChart.this.displayData();
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
		
		menu.findItem(R.id.mi_primary_source).setVisible(true);
		
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem primarySourceItem = menu.findItem(R.id.mi_primary_source);
	    try {
	    	String sourceUrl = this.data.getDatasets().get(this.variable.getCommonVariable()).getSourceUrl();
	    	primarySourceItem.setEnabled(sourceUrl != null);
	    } catch(NullPointerException npe) {
	    	primarySourceItem.setEnabled(false);
	    }
	    return true;
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
	    		openContextMenu(chartView);
		    	return true;
	    	}
	    	return false;
	    case R.id.mi_primary_source:
	    	String sourceUrl = null;
	    	try {
	    		sourceUrl = this.data.getDatasets().get(this.variable.getCommonVariable()).getSourceUrl();
	    	} catch(NullPointerException npe) {
				Toast.makeText(getApplicationContext(),  "Cannot view primary source if the data failed to load", Toast.LENGTH_LONG).show();
				return true;
	    	}
	    	
			Intent viewOriginalData = new Intent(this, OriginalData.class);
			viewOriginalData.putExtra(OriginalData.KEY_SITE, this.station);
			viewOriginalData.putExtra(OriginalData.KEY_SOURCE_URL, sourceUrl);
			startActivity(viewOriginalData);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.setHeaderTitle(R.string.variable_context_menu_title);
		
		Variable[] otherVariables = ViewChart.this.station.getSupportedVariables();
		for(int a = 0; a < otherVariables.length; a++) {
			if(variable.equals(otherVariables[a])) {
				continue;
			}
			menu.add(1,a,a,otherVariables[a].getName() + ", " + otherVariables[a].getCommonVariable().getUnit());
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		Site selectedStation = null;
		
		Variable[] otherVariables = this.station.getSupportedVariables();
		for(int a = 0; a < otherVariables.length; a++) {
			if(a == item.getItemId()){
				selectedStation =ViewChart.this.station;
				this.variable = otherVariables[a];
				break;
			}
		}
		
		if(selectedStation == null) {
			Log.w(TAG,"no variable at index " + item.getItemId());
			return false;
		}
		
		if(this.data.getDatasets().get(this.variable.getCommonVariable()) == null) {
			reloadData();
		} else {
			clearData();
			displayData();
		}
        
        return true;
	}
}
