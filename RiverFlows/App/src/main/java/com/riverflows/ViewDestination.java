package com.riverflows;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager.BadTokenException;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.inject.Inject;
import com.riverflows.data.CelsiusFahrenheitConverter;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.ValueConverter;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.view.HydroGraph;
import com.riverflows.wsclient.DataParseException;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.ToggleFavoriteTask;
import com.riverflows.wsclient.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;

/**
 * Experimenting with using AChartEngine for displaying the hydrograph
 * @author robin
 *
 */
public class ViewDestination extends RoboActivity {

	public static final String KEY_DESTINATION_FACET = "destination_facet";

	public static final int DIALOG_ID_LOADING_ERROR = 1;

	DestinationFacet destinationFacet;
	Boolean zeroYMin = null;

	LinearLayout chartLayout;
	HydroGraph chartView;
	GenerateDataSetTask runningTask = null;
	FetchHydrographTask runningShareTask = null;
	Map<CommonVariable, CommonVariable> conversionMap = new HashMap<CommonVariable, CommonVariable>();
	SiteData data;
	String errorMsg;

	private static final SimpleDateFormat lastReadingDateFmt = new SimpleDateFormat("M/d h:mm aa zzz");

    private class FavoriteButtonListener implements CompoundButton.OnCheckedChangeListener {

    	@Override
    	public void onCheckedChanged(CompoundButton buttonView,
    			boolean isChecked) {
    		Favorite f = new Favorite(ViewDestination.this.getSite(), ViewDestination.this.getVariable().getId());
            f.setDestinationFacet(ViewDestination.this.destinationFacet);

            new ToggleFavoriteTask(ViewDestination.this, false, f).execute();
    	}
    }

    private class DataSrcInfoButtonListener implements OnClickListener {
    	@Override
    	public void onClick(View v) {
			Intent viewOriginalData = new Intent(ViewDestination.this, DataSrcInfo.class);
			viewOriginalData.putExtra(DataSrcInfo.KEY_INFO, ViewDestination.this.data.getDataInfo());
			startActivity(viewOriginalData);
	        return;
    	}
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_chart);

        Bundle extras = getIntent().getExtras();

        this.destinationFacet = (DestinationFacet)extras.get(KEY_DESTINATION_FACET);

        TextView titleText = (TextView)findViewById(R.id.title);
        titleText.setText(this.destinationFacet.getDestination().getName());

        CheckBox favoriteBtn = (CheckBox)findViewById(R.id.favorite_btn);
        favoriteBtn.setVisibility(View.GONE);

        chartLayout = (LinearLayout) findViewById(R.id.chart);


		SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, MODE_PRIVATE);
    	String tempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);

    	//Log.d(Home.TAG, "saved unit: " + tempUnit);
    	conversionMap = CommonVariable.temperatureConversionMap(tempUnit);

        //see onRetainNonConfigurationInstance()
        final Object data = getLastNonConfigurationInstance();

        if(data == null) {
            //make the request for site data
            new GenerateDataSetTask(this, false).execute(this.getSite());
        } else {
            Object[] prevState = (Object[])data;
            this.destinationFacet = (DestinationFacet)prevState[0];
            this.data = (SiteData)prevState[1];
            this.runningTask = (GenerateDataSetTask)prevState[2];
            this.zeroYMin = (Boolean)prevState[3];
            clearData();
            if(runningTask != null) {
                this.runningTask.setActivity(this);
            }
            this.runningShareTask = (FetchHydrographTask)prevState[4];
            if(this.runningShareTask != null) {
                this.runningShareTask.setActivity(this);
            }
            //use stored data instead
            if(this.data != null) {
                displayData();
            }
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
    	tracker.setCustomDimension(4, this.getVariable().getId());
    	tracker.setCustomDimension(5, this.getVariable().getCommonVariable().name());
    }

    public Site getSite() {
        return this.destinationFacet.getDestination().getSite();
    }

    public Variable getVariable() {
        return this.destinationFacet.getVariable();
    }

    @Override
    protected void onStop() {
    	super.onStop();

    	EasyTracker.getInstance().activityStop(this);
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
        new GenerateDataSetTask(this, true).execute(getSite());
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
				if(Log.isLoggable(App.TAG, Log.INFO)) {
					Log.i(App.TAG, "can't display dialog; activity no longer active");
				}
				return;
			}
            progressBar.setVisibility(View.GONE);

            favoriteBtn.setOnCheckedChangeListener(new FavoriteButtonListener());
            favoriteBtn.setVisibility(View.VISIBLE);
            return;
		}

		String siteAgency = this.data.getSite().getAgency();

		ImageView dataSrcInfoButton = (ImageView)findViewById(R.id.dataSrcInfoB);

		if(ViewDestination.this.data.getDataInfo() != null) {
			dataSrcInfoButton.setVisibility(View.VISIBLE);
			dataSrcInfoButton.setOnClickListener(new DataSrcInfoButtonListener());

			Integer agencyIconResId = Home.getAgencyIconResId(siteAgency);

	        if(agencyIconResId != null) {
	        	dataSrcInfoButton.setImageResource(agencyIconResId);
	        } else {
	        	Log.e(App.TAG, "no icon for agency: " + siteAgency);
	        	dataSrcInfoButton.setVisibility(View.GONE);
	        }
		} else {
        	dataSrcInfoButton.setVisibility(View.GONE);
		}

		Series displayedSeries = null;

		displayedSeries = this.data.getDatasets().get(getVariable().getCommonVariable());

        if(displayedSeries == null) {
    		displayedSeries = DataSourceController.getPreferredSeries(this.data);
    		Log.d(Home.TAG, "No series found for " + getVariable());
    	}

    	if(displayedSeries == null || displayedSeries.getReadings().size() == 0) {
			if(errorMsg == null) {
				errorMsg = "Error: No Data";
			}
			try {
				showDialog(DIALOG_ID_LOADING_ERROR);
			} catch(BadTokenException bte) {
				if(Log.isLoggable(App.TAG, Log.INFO)) {
					Log.i(App.TAG, "can't display dialog; activity no longer active");
				}
				return;
			}
            progressBar.setVisibility(View.GONE);
            favoriteBtn.setVisibility(View.VISIBLE);
            favoriteBtn.setOnCheckedChangeListener(new FavoriteButtonListener());
            favoriteBtn.setVisibility(View.VISIBLE);
            return;
    	}

    	boolean converted = ValueConverter.convertIfNecessary(ViewDestination.this.conversionMap, displayedSeries);

        Reading mostRecentReading = displayedSeries.getLastObservation();

        Date mostRecentReadingTime = mostRecentReading.getDate();

        String mostRecentReadingStr = null;
        String unit = null;

        if(mostRecentReading.getValue() != null) {

	        //get rid of unnecessary digits
        	if(converted) {
	            mostRecentReadingStr = Utils.abbreviateNumber(mostRecentReading.getValue(), 3);
        	} else {
        		mostRecentReadingStr = "" + mostRecentReading.getValue();
        		if(mostRecentReadingStr.endsWith(".0")) {
    	        	mostRecentReadingStr = mostRecentReadingStr.substring(0, mostRecentReadingStr.length() - 2);
    	        }
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

        TextView lastReading = (TextView)ViewDestination.this.findViewById(R.id.lastReading);
        lastReading.setText("Last Reading: " + mostRecentReadingStr + unit + ", on " + lastReadingDateFmt.format(mostRecentReadingTime));

        chartView = new HydroGraph(ViewDestination.this);

		if(zeroYMin == null) {
			//automatically determine the y-axis ranging mode using the variable
			chartView.setSeries(displayedSeries, getVariable().getCommonVariable().isGraphAgainstZeroMinimum());
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

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{this.destinationFacet,this.data,this.runningTask,this.zeroYMin,this.runningShareTask};
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
		private ViewDestination activity;
		private Variable variable;
		private Site site;
		private boolean hardRefresh = false;

        @Inject
        private DataSourceController dataSourceController;

    	public GenerateDataSetTask(ViewDestination activity, boolean hardRefresh) {
			super();
    		this.activity = activity;
    		this.activity.runningTask = this;
    		this.variable = this.activity.getVariable();
    		this.site = this.activity.getSite();
    		this.hardRefresh = hardRefresh;
            RoboGuice.getInjector(activity).injectMembers(this);
		}

    	public void setActivity(ViewDestination activity) {
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
        				Log.e(App.TAG, "could not find " + variable.getId() + " in supported vars for " + site.getSiteId());
        				variables = new Variable[]{variable};
        			}
            	}

        		return this.dataSourceController.getSiteData(site, variables, this.hardRefresh);
            } catch(UnknownHostException uhe) {
            	errorMsg = "Lost network connection.";
            } catch(IOException ioe) {
            	errorMsg = "Could not retrieve site data: an I/O error has occurred.";
            	Log.e(App.TAG, station.getId(), ioe);

				EasyTracker.getTracker().sendException(getClass().getCanonicalName(), ioe, false);
            } catch(DataParseException dpe) {
            	errorMsg = "Could not process data from " + station + "; " + dpe.getMessage();
            	Log.e(App.TAG, station.toString(), dpe);

				EasyTracker.getTracker().sendException(getClass().getCanonicalName(), dpe, false);
            } catch(Exception e) {
            	errorMsg = "Error loading data from " + station + "; " + e.getMessage();
            	Log.e(App.TAG, station.toString(), e);

				EasyTracker.getTracker().sendException(getClass().getCanonicalName(), e, false);
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

		if(!FavoritesDaoImpl.isFavorite(getApplicationContext(), getSite().getSiteId(), getVariable())) {
			return false;
		}

    	FavoritesDaoImpl.updateLastViewedTime(getApplicationContext(), getSite().getSiteId());

    	return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.standard_menu, menu);

        MenuItem otherVarsItem = menu.findItem(R.id.mi_other_variables);
        otherVarsItem.setVisible(true);
		otherVarsItem.setEnabled(getSite().getSupportedVariables().length > 1);

        MenuItem unitsItem = menu.findItem(R.id.mi_change_units);
        unitsItem.setVisible(true);

        if(conversionMap.containsKey(getVariable().getCommonVariable())) {
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
	    	reloadData();
	    	return true;
	    case R.id.mi_other_variables:
	    case R.id.mi_change_units:
	    	if(chartView != null) {
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
		private ViewDestination activity = null;
        private Site site = null;
        private Variable variable = null;

		public FetchHydrographTask(ViewDestination activity) {
            this.site = activity.destinationFacet.getDestination().getSite();
            this.variable = activity.destinationFacet.getVariable();

			EasyTracker.getTracker().sendSocial("ACTION_SEND", "start", this.site.getAgency() + ":" + this.site.getId());

			graphUrl = DataSourceController.getDataSource(this.site.getAgency()).getExternalGraphUrl(this.site.getId(), this.variable.getId());
			this.activity = activity;
			activity.runningShareTask = this;
		}

    	public void setActivity(ViewDestination activity) {
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

			Intent intent=new Intent(Intent.ACTION_SEND);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

			String varName = "";

			varName = this.variable.getCommonVariable().getName() + " at ";

			intent.putExtra(Intent.EXTRA_SUBJECT, varName + this.site.getName());

			if(result != null) {
				//share with embedded image

		    	Uri graphUri = Uri.fromFile(result);

		    	Log.i(Home.TAG, "file URI: " + graphUri);

				intent.setType("image/png");
				intent.putExtra(Intent.EXTRA_TEXT,
				        "Shared using the RiverFlows mobile app");
				intent.putExtra(Intent.EXTRA_STREAM, graphUri);
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
        	throw new RuntimeException("data: " + getSite().getSiteId() + " vars: " + otherVariables,npe);
        }
        return true;
	}
	
	private boolean populateUnitsSubmenu(SubMenu unitsMenu) {
        unitsMenu.setHeaderTitle("Units");
        
        CommonVariable displayedVariable = conversionMap.get(getVariable().getCommonVariable());
        
        if(displayedVariable == null) {
        	displayedVariable = getVariable().getCommonVariable();
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
            Intent i = new Intent(ViewDestination.this, ViewChart.class);

            i.putExtra(ViewChart.KEY_SITE, getSite());
            i.putExtra(ViewChart.KEY_VARIABLE, getVariable());
            startActivity(i);

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
			
			Variable fromVar = ViewDestination.this.getVariable();
			
			if(fromVar == null) {
				return false;
			}
			
			CommonVariable displayedVariable = conversionMap.get(fromVar.getCommonVariable());
			
			if(displayedVariable == null) {
				displayedVariable = fromVar.getCommonVariable();
			}
			
			if(unit.equals(displayedVariable.getUnit())) {
				return false;
			}
			
			SharedPreferences settings = getSharedPreferences(Home.PREFS_FILE, MODE_PRIVATE);
        	Editor prefsEditor = settings.edit();
        	prefsEditor.putString(Home.PREF_TEMP_UNIT, unit);
        	prefsEditor.commit();
        	
			conversionMap = CommonVariable.temperatureConversionMap(unit);
			
			if(ViewDestination.this.data == null || ViewDestination.this.data.getDatasets().get(fromVar.getCommonVariable()) == null) {
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
