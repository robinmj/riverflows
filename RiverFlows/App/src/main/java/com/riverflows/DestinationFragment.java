package com.riverflows;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.riverflows.data.Category;
import com.riverflows.data.DecoratedCategory;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.ValueConverter;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.loader.DataSetLoader;
import com.riverflows.view.HydroGraph;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.ToggleFavoriteTask;
import com.riverflows.wsclient.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import roboguice.fragment.RoboFragment;

/**
* Created by robin on 12/13/14.
*/
public class DestinationFragment extends RoboFragment implements LoaderManager.LoaderCallbacks<SiteData> {

    public static final int LOADER_ID_DESTINATION = 5942;

    public static final int REQUEST_EDIT_DESTINATION = 837;

    public static final String ARG_ZERO_Y_MIN = "zeroYMin";
    public static final String ARG_DESTINATION_FACET = "destinationFacet";
    public static final String ARG_CONVERSION_MAP = "conversionMap";

    private DestinationFacet destinationFacet;
    Boolean zeroYMin = null;

    LinearLayout chartLayout;
    HydroGraph chartView;
    Map<Variable.CommonVariable, Variable.CommonVariable> conversionMap = new HashMap<Variable.CommonVariable, Variable.CommonVariable>();
    private SiteData data;
    String errorMsg;

    private CompoundButton.OnCheckedChangeListener favoriteButtonListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Favorite f = new Favorite(DestinationFragment.this.destinationFacet.getDestination().getSite(),
                    DestinationFragment.this.destinationFacet.getVariable().getId());

            f.setDestinationFacet(DestinationFragment.this.destinationFacet);

            new ToggleFavoriteTask(getActivity(), false, f).execute();
        }
    };

    private static final SimpleDateFormat lastReadingDateFmt = new SimpleDateFormat("M/d h:mm aa zzz");

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View v = getView();

        Bundle args = getArguments();
        this.destinationFacet = (DestinationFacet)args.getSerializable(ARG_DESTINATION_FACET);
        this.zeroYMin = args.getBoolean(ARG_ZERO_Y_MIN);
        this.conversionMap = (Map<Variable.CommonVariable,Variable.CommonVariable>)args.getSerializable(ARG_CONVERSION_MAP);

        TextView titleText = (TextView)v.findViewById(R.id.title);
        titleText.setText(this.destinationFacet.getDestination().getSite().getName());

        CheckBox favoriteBtn = (CheckBox)v.findViewById(R.id.favorite_btn);
        favoriteBtn.setVisibility(View.GONE);

        chartLayout = (LinearLayout)v.findViewById(R.id.chart);


        getActivity().getSupportLoaderManager().initLoader(LOADER_ID_DESTINATION, new Bundle(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.view_destination, container, false);
    }

    public DestinationFacet getDestinationFacet() {
        return destinationFacet;
    }

    public DecoratedCategory[] getCategories() {
        Category[] categories = getDestinationFacet().getCategories();

        DecoratedCategory[] decoratedCategories = new DecoratedCategory[categories.length];

        int a = 0;

        for(Category category : categories) {
            DecoratedCategory decoratedCategory = new DecoratedCategory();

            decoratedCategory.category = category;

            if(category.getName().equals(DestinationFacet.CN_HIGH_PLUS)) {
                decoratedCategory.displayName = "Too High";
                decoratedCategory.textColor = getResources().getColor(R.color.txt_level_too_high);
                decoratedCategory.bgColor = getResources().getColor(R.color.bg_level_too_high);
            } else if(category.getName().equals(DestinationFacet.CN_HIGH)) {
                decoratedCategory.displayName = "High";
                decoratedCategory.textColor = getResources().getColor(R.color.txt_level_high);
                decoratedCategory.bgColor = getResources().getColor(R.color.bg_level_high);
            } else if(category.getName().equals(DestinationFacet.CN_MED)) {
                decoratedCategory.displayName = "Medium";
                decoratedCategory.textColor = getResources().getColor(R.color.txt_level_medium);
                decoratedCategory.bgColor = getResources().getColor(R.color.bg_level_medium);
            } else if(category.getName().equals(DestinationFacet.CN_LOW)) {
                decoratedCategory.displayName = "Low";
                decoratedCategory.textColor = getResources().getColor(R.color.txt_level_low);
                decoratedCategory.bgColor = getResources().getColor(R.color.bg_level_low);
            } else if(category.getName().equals(DestinationFacet.CN_TOO_LOW)) {
                decoratedCategory.displayName = "Too Low";
                decoratedCategory.textColor = getResources().getColor(R.color.txt_level_too_low);
                decoratedCategory.bgColor = getResources().getColor(R.color.bg_level_too_low);
            }

            decoratedCategory.bgColor = addAlpha(decoratedCategory.bgColor, 128);
            decoratedCategory.textColor = addAlpha(decoratedCategory.textColor, 128);

            decoratedCategories[a++] = decoratedCategory;
        }

        return decoratedCategories;
    }

    public static int addAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public void setDestinationFacet(DestinationFacet destinationFacet) {
        this.destinationFacet = destinationFacet;
    }

    public SiteData getData() {
        return data;
    }

    @Override
    public Loader<SiteData> onCreateLoader(int i, Bundle bundle) {
        /*Variable v = (Variable)bundle.getSerializable("variable");
        Site s = (Site)bundle.getSerializable("site");
        boolean hardRefresh = bundle.getBoolean("hardRefresh");*/

        return new DataSetLoader(getActivity(), getSite(), getVariable(), false);
    }

    @Override
    public void onLoaderReset(Loader<SiteData> siteDataLoader) {

    }

    @Override
    public void onLoadFinished(Loader<SiteData> siteDataLoader, SiteData siteData) {
        this.errorMsg = ((DataSetLoader)siteDataLoader).getErrorMsg();
        this.data = siteData;

        this.displayData();
    }

    private void clearData() {
        ProgressBar progressBar = (ProgressBar)getView().findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        CheckBox favoriteBtn = (CheckBox)getView().findViewById(R.id.favorite_btn);
        favoriteBtn.setOnCheckedChangeListener(null);
        favoriteBtn.setVisibility(View.GONE);
        chartLayout.removeView(chartView);
        this.errorMsg = null;
    }

    private void displayData() {
        View v = getView();

        ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        CheckBox favoriteBtn = (CheckBox)v.findViewById(R.id.favorite_btn);

        if(this.data == null || errorMsg != null) {
            if(errorMsg == null) {
                errorMsg = "Error: No Data";
            }
            try {
                showMessage(errorMsg);
            } catch(WindowManager.BadTokenException bte) {
                if(Log.isLoggable(App.TAG, Log.INFO)) {
                    Log.i(App.TAG, "can't display dialog; activity no longer active");
                }
                return;
            }
            progressBar.setVisibility(View.GONE);

            favoriteBtn.setOnCheckedChangeListener(this.favoriteButtonListener);
            favoriteBtn.setVisibility(View.VISIBLE);
            return;
        }

        String siteAgency = this.data.getSite().getAgency();

        ImageView dataSrcInfoButton = (ImageView)v.findViewById(R.id.dataSrcInfoB);

        if(this.data.getDataInfo() != null) {
            dataSrcInfoButton.setVisibility(View.VISIBLE);
            dataSrcInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewOriginalData = new Intent(DestinationFragment.this.getActivity(), DataSrcInfo.class);
                    viewOriginalData.putExtra(DataSrcInfo.KEY_INFO, DestinationFragment.this.data.getDataInfo());
                    startActivity(viewOriginalData);
                    return;
                }
            });

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
                showMessage(errorMsg);
            } catch(WindowManager.BadTokenException bte) {
                if(Log.isLoggable(App.TAG, Log.INFO)) {
                    Log.i(App.TAG, "can't display dialog; activity no longer active");
                }
                return;
            }
            progressBar.setVisibility(View.GONE);
            favoriteBtn.setVisibility(View.VISIBLE);

            favoriteBtn.setOnCheckedChangeListener(this.favoriteButtonListener);

            favoriteBtn.setVisibility(View.VISIBLE);
            return;
        }

        boolean converted = ValueConverter.convertIfNecessary(this.conversionMap, displayedSeries);

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

        TextView lastReading = (TextView)v.findViewById(R.id.lastReading);
        lastReading.setText("Last Reading: " + mostRecentReadingStr + unit + ", on " + lastReadingDateFmt.format(mostRecentReadingTime));

        chartView = new HydroGraph(getActivity());

        if(zeroYMin == null) {
            //automatically determine the y-axis ranging mode using the variable
            chartView.setSeries(displayedSeries, getCategories(),
                    getVariable().getCommonVariable().isGraphAgainstZeroMinimum());
        } else if(zeroYMin) {
            chartView.setSeries(displayedSeries, getCategories(), true);
        } else {
            chartView.setSeries(displayedSeries, getCategories(), false);
        }

        chartLayout.addView(chartView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));

        //this can't be called until ViewChart.this.chartView and ViewChart.this.variable have been initialized
        registerForContextMenu(chartView);

        favoriteBtn.setVisibility(View.VISIBLE);
        favoriteBtn.setChecked(isFavorite());
        favoriteBtn.setOnCheckedChangeListener(this.favoriteButtonListener);
        progressBar.setVisibility(View.GONE);
    }

    private void showMessage(String message) {
        TextView statusMsgView = (TextView)getView().findViewById(R.id.status_message);
        statusMsgView.setText(message);
        statusMsgView.setVisibility(View.VISIBLE);
    }

    private void hideMessage() {
        getView().findViewById(R.id.status_message).setVisibility(View.GONE);
    }

    public Site getSite() {
        return this.destinationFacet.getDestination().getSite();
    }

    public Variable getVariable() {
        return this.destinationFacet.getVariable();
    }

    private boolean isFavorite() {

        if(!FavoritesDaoImpl.isFavorite(getActivity(), getSite().getSiteId(), getVariable())) {
            return false;
        }

        FavoritesDaoImpl.updateLastViewedTime(getActivity(), getSite().getSiteId());

        return true;
    }

    public void setZeroYMin(boolean zeroYMin) {
        this.zeroYMin = new Boolean(zeroYMin);
        //persist during orientation changes
        getArguments().putBoolean(ARG_ZERO_Y_MIN, this.zeroYMin);
        clearData();
        displayData();
    }

    public boolean setTempUnit(String unit) {

        Variable fromVar = getVariable();

        if(fromVar == null) {
            return false;
        }

        Variable.CommonVariable displayedVariable = conversionMap.get(fromVar.getCommonVariable());

        if(displayedVariable == null) {
            displayedVariable = fromVar.getCommonVariable();
        }

        if(unit.equals(displayedVariable.getUnit())) {
            return false;
        }

        SharedPreferences settings = getActivity().getSharedPreferences(Home.PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = settings.edit();
        prefsEditor.putString(Home.PREF_TEMP_UNIT, unit);
        prefsEditor.commit();

        this.conversionMap = Variable.CommonVariable.temperatureConversionMap(unit);

        if(this.data == null || this.data.getDatasets().get(fromVar.getCommonVariable()) == null) {
            clearData();
            reload();
        } else {
            clearData();
            displayData();
        }

        return true;
    }

    public void reload() {
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_DESTINATION, new Bundle(), this);
    }

    /**
     * WARNING: check user permissions before calling this!
     */
    public void editDestination() {

        Intent i = new Intent(getActivity(), EditDestination.class);
        i.putExtra(EditDestination.KEY_DESTINATION_FACET, getDestinationFacet());

        startActivityForResult(i, REQUEST_EDIT_DESTINATION);
    }
}
