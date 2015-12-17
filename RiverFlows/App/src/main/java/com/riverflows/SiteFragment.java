package com.riverflows;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.google.inject.Inject;
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
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import roboguice.fragment.RoboFragment;
import tourguide.tourguide.Overlay;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

/**
 * Fragment for viewing site or favorite site
 * Created by robin on 4/14/15.
 */
public class SiteFragment extends RoboFragment implements LoaderManager.LoaderCallbacks<SiteData> {

    public static final int LOADER_ID_SITE = 3816;

    public static final String ARG_ZERO_Y_MIN = "zeroYMin";
    public static final String ARG_SITE = "site";
    public static final String ARG_VARIABLE = "variable";
    public static final String ARG_CONVERSION_MAP = "conversionMap";
    public static final String ARG_FAVORITE = "favorite";

    Boolean zeroYMin = null;

    private Site site;
    private Variable variable;

    private LinearLayout chartLayout;
    private HydroGraph chartView;
    private Map<Variable.CommonVariable, Variable.CommonVariable> conversionMap = new HashMap<Variable.CommonVariable, Variable.CommonVariable>();
    private SiteData data;
    String errorMsg;
    private TourGuide mTourGuideHandler;
    private Favorite favorite;

    @Inject
    private WsSessionManager wsSessionManager;

    private CompoundButton.OnCheckedChangeListener favoriteButtonListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            WsSession session = SiteFragment.this.wsSessionManager.getSession(getActivity());

            Variable v = SiteFragment.this.getVariable();

            if(v == null) {
                return;
            }

            Favorite f = new Favorite(SiteFragment.this.getSite(), v.getId());

            if(session == null) {
                boolean isFavorite = FavoritesDaoImpl.isFavorite(getActivity().getApplicationContext(), f.getSite().getSiteId(), f.getVariable());

                if(isFavorite) {
                    FavoritesDaoImpl.deleteFavorite(getActivity().getApplicationContext(), f.getSite().getSiteId(), f.getVariable());
                    setFavorite(null);
                } else {
                    FavoritesDaoImpl.createFavorite(getActivity().getApplicationContext(), f);
                    setFavorite(f);
                }

                getActivity().sendBroadcast(Home.getWidgetUpdateIntent());
                Favorites.softReloadNeeded = true;
            } else {
                new ToggleFavoriteTask(getActivity(), false, f).execute();
            }
        }
    };

    private static final SimpleDateFormat lastReadingDateFmt = new SimpleDateFormat("M/d h:mm aa zzz", Locale.getDefault());

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View v = getView();

        Bundle args = getArguments();
        this.site = (Site) args.getSerializable(ARG_SITE);
        this.variable = (Variable) args.getSerializable(ARG_VARIABLE);
        this.zeroYMin = args.getBoolean(ARG_ZERO_Y_MIN);
        this.conversionMap = (Map<Variable.CommonVariable, Variable.CommonVariable>) args.getSerializable(ARG_CONVERSION_MAP);
        this.favorite = (Favorite) args.getSerializable(ARG_FAVORITE);

        Variable var = this.getVariable();

        if(var != null) {
            ((TextView) v.findViewById(R.id.title)).setText(var.getName());
        }

        CheckBox favoriteBtn = (CheckBox) v.findViewById(R.id.favorite_btn);
        favoriteBtn.setVisibility(View.GONE);

        chartLayout = (LinearLayout) v.findViewById(R.id.chart);


        getActivity().getSupportLoaderManager().initLoader(LOADER_ID_SITE, new Bundle(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.view_destination, container, false);
    }

    public Favorite getFavorite() {
        return favorite;
    }

    public void setFavorite(Favorite favorite) {
        this.favorite = favorite;
        getArguments().putSerializable(ARG_FAVORITE, favorite);
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
    public void onDestroy() {
        // need to check if overlay is null because Robolectric destroys the fragment before
        //  the overlay is constructed, causing an NPE
        if(this.mTourGuideHandler != null && this.mTourGuideHandler.getOverlay() != null) {
            this.mTourGuideHandler.cleanUp();
        }

        super.onDestroy();
    }

    @Override
    public void onLoadFinished(Loader<SiteData> siteDataLoader, SiteData siteData) {
        this.errorMsg = ((DataSetLoader) siteDataLoader).getErrorMsg();
        this.data = siteData;

        this.displayData();
    }

    private void clearData() {
        ProgressBar progressBar = (ProgressBar) getView().findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        CheckBox favoriteBtn = (CheckBox) getView().findViewById(R.id.favorite_btn);
        favoriteBtn.setOnCheckedChangeListener(null);
        favoriteBtn.setVisibility(View.GONE);
        chartLayout.removeView(chartView);
        this.errorMsg = null;
    }

    private void displayData() {
        View v = getView();

        ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        CheckBox favoriteBtn = (CheckBox) v.findViewById(R.id.favorite_btn);

        if (this.data == null || errorMsg != null) {
            if (errorMsg == null) {
                errorMsg = "Error: No Data";
            }
            try {
                showMessage(errorMsg);
            } catch (WindowManager.BadTokenException bte) {
                if (Log.isLoggable(App.TAG, Log.INFO)) {
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

        ImageView dataSrcInfoButton = (ImageView) v.findViewById(R.id.dataSrcInfoB);

        if (this.data.getDataInfo() != null) {
            dataSrcInfoButton.setVisibility(View.VISIBLE);
            dataSrcInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewOriginalData = new Intent(SiteFragment.this.getActivity(), DataSrcInfo.class);
                    viewOriginalData.putExtra(DataSrcInfo.KEY_INFO, SiteFragment.this.data.getDataInfo());
                    startActivity(viewOriginalData);
                }
            });

            Integer agencyIconResId = Home.getAgencyIconResId(siteAgency);

            if (agencyIconResId != null) {
                dataSrcInfoButton.setImageResource(agencyIconResId);
            } else {
                Log.e(App.TAG, "no icon for agency: " + siteAgency);
                dataSrcInfoButton.setVisibility(View.GONE);
            }
        } else {
            dataSrcInfoButton.setVisibility(View.GONE);
        }

        Series displayedSeries = null;

        if(this.variable != null) {
            displayedSeries = this.data.getDatasets().get(this.variable.getCommonVariable());
        }

        if (displayedSeries == null) {
            Log.d(Home.TAG, "No series found for " + getVariable());
            if(!this.data.isComplete() && getVariable() != null) {
                //DataSetLoader will reload data specifically requesting the current variable
                reload();
                return;
            } else {
                displayedSeries = DataSourceController.getPreferredSeries(this.data);
            }
        }

        if (displayedSeries == null || displayedSeries.getReadings().size() == 0) {
            if (errorMsg == null) {
                errorMsg = "Error: No Data";
            }
            try {
                showMessage(errorMsg);
            } catch (WindowManager.BadTokenException bte) {
                if (Log.isLoggable(App.TAG, Log.INFO)) {
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

        if(this.variable == null) {
            this.variable = displayedSeries.getVariable();
            ((TextView) v.findViewById(R.id.title)).setText(this.getVariable().getName());
            Log.d(Home.TAG, "displayed series unit " + this.variable.getUnit());
        }

        ((TextView) v.findViewById(R.id.title)).setText(displayedSeries.getVariable().getName());

        boolean converted = ValueConverter.convertIfNecessary(this.conversionMap, displayedSeries);

        Reading mostRecentReading = displayedSeries.getLastObservation();

        Date mostRecentReadingTime = mostRecentReading.getDate();

        String mostRecentReadingStr = null;
        String unit = null;

        if (mostRecentReading.getValue() != null) {

            //get rid of unnecessary digits
            if (converted) {
                mostRecentReadingStr = Utils.abbreviateNumber(mostRecentReading.getValue(), 3);
            } else {
                mostRecentReadingStr = "" + mostRecentReading.getValue();
                if (mostRecentReadingStr.endsWith(".0")) {
                    mostRecentReadingStr = mostRecentReadingStr.substring(0, mostRecentReadingStr.length() - 2);
                }
            }

            unit = displayedSeries.getVariable().getUnit();
            if (unit == null) {
                unit = "";
            }
            if (unit.trim().length() > 0) {
                unit = " " + unit;
            }
        } else {
            mostRecentReadingStr = "unknown";
            if (mostRecentReading.getQualifiers() != null) {
                unit = ": " + mostRecentReading.getQualifiers();
            } else {
                unit = "";
            }
        }

        TextView lastReading = (TextView) v.findViewById(R.id.lastReading);
        lastReading.setText("Last Reading: " + mostRecentReadingStr + unit + ", on " + lastReadingDateFmt.format(mostRecentReadingTime));

        chartView = new HydroGraph(getActivity());

        if (zeroYMin == null) {
            //automatically determine the y-axis ranging mode using the variable
            chartView.setSeries(displayedSeries, getVariable().getCommonVariable().isGraphAgainstZeroMinimum());
        } else if (zeroYMin) {
            chartView.setSeries(displayedSeries, true);
        } else {
            chartView.setSeries(displayedSeries, false);
        }

        chartLayout.addView(chartView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));

        //this can't be called until SiteFragment.this.chartView and SiteFragment.this.variable have been initialized
        registerForContextMenu(chartView);

        favoriteBtn.setVisibility(View.VISIBLE);
        favoriteBtn.setChecked(this.favorite != null);
        favoriteBtn.setOnCheckedChangeListener(this.favoriteButtonListener);
        progressBar.setVisibility(View.GONE);

        SharedPreferences settings = getActivity().getSharedPreferences(Home.PREFS_FILE, Context.MODE_PRIVATE);
        String tempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);
        boolean favoriteIntroShown = settings.getBoolean(Home.PREF_FAVORITE_INTRO, false);

        if(!favoriteIntroShown) {
            if(this.favorite == null) {
                ToolTip toolTip = new ToolTip().setDescription("Hint: touch the star to save this site as a favorite").setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SiteFragment.this.mTourGuideHandler.cleanUp();
                    }
                });

                Overlay overlay = new Overlay().disableClick(false).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SiteFragment.this.mTourGuideHandler.cleanUp();
                    }
                });

                this.mTourGuideHandler = TourGuide.init(getActivity()).with(TourGuide.Technique.Click)
                        .setPointer(null)
                        .setToolTip(toolTip)
                        .setOverlay(overlay)
                        .playOn(favoriteBtn);
            }

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(Home.PREF_FAVORITE_INTRO, true);
            editor.commit();
        }
    }

    /**
     * try our damnedest to initialize the variable property so the user can mark this
     * site as a favorite even if it doesn't have data right now.
     */
    private void initContingencyFavoriteBtn(CheckBox favoriteBtn) {
        if(this.getSite().getSupportedVariables().length > 0) {
            final Variable variable = this.getSite().getSupportedVariables()[0];

            favoriteBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Favorite f = new Favorite(SiteFragment.this.getSite(), variable.getId());

                    new ToggleFavoriteTask(SiteFragment.this.getActivity(), false, f).execute();
                }
            });
            favoriteBtn.setVisibility(View.VISIBLE);
        }
    }

    private void showMessage(String message) {
        TextView statusMsgView = (TextView) getView().findViewById(R.id.status_message);
        statusMsgView.setText(message);
        statusMsgView.setVisibility(View.VISIBLE);
    }

    private void hideMessage() {
        getView().findViewById(R.id.status_message).setVisibility(View.GONE);
    }

    public Site getSite() {
        return this.site;
    }

    public Variable getVariable() {
        return this.variable;
    }

    public void setVariable(@NonNull Variable  variable) {
        this.variable = variable;

        getArguments().putSerializable(ARG_VARIABLE, this.variable);

        //make sure we have most relevant favorite
        List<Favorite> favorites = FavoritesDaoImpl.getFavorites(getActivity().getApplicationContext(), site.getSiteId(), variable.getId());

        if (favorites.size() > 0) {
            this.favorite = favorites.get(0);
            getArguments().putSerializable(ARG_FAVORITE, this.favorite);
        } else {
            this.favorite = null;
        }

        clearData();
        displayData();
    }

    public Map<Variable.CommonVariable, Variable.CommonVariable> getConversionMap() {
        return this.conversionMap;
    }

    public void setZeroYMin(boolean zeroYMin) {
        this.zeroYMin = zeroYMin;
        //persist during orientation changes
        getArguments().putBoolean(ARG_ZERO_Y_MIN, this.zeroYMin);
        clearData();
        displayData();
    }

    public boolean setTempUnit(String unit) {

        Variable fromVar = getVariable();

        if (fromVar == null) {
            return false;
        }

        Variable.CommonVariable displayedVariable = conversionMap.get(fromVar.getCommonVariable());

        if (displayedVariable == null) {
            displayedVariable = fromVar.getCommonVariable();
        }

        if (unit.equals(displayedVariable.getUnit())) {
            return false;
        }

        SharedPreferences settings = getActivity().getSharedPreferences(Home.PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = settings.edit();
        prefsEditor.putString(Home.PREF_TEMP_UNIT, unit);
        prefsEditor.commit();

        this.conversionMap = Variable.CommonVariable.temperatureConversionMap(unit);

        if (this.data == null || this.data.getDatasets().get(fromVar.getCommonVariable()) == null) {
            clearData();
            reload();
        } else {
            clearData();
            displayData();
        }

        return true;
    }

    public void reload() {
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_SITE, new Bundle(), this);
    }
}
