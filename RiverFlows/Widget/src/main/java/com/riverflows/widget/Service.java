package com.riverflows.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.CDECDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.USACEDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Service extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.d(Provider.TAG, "onGetViewFactory");
        return new WidgetViewsFactory(this.getApplicationContext(), intent);
    }
}

class WidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    public static final String TAG = "RiverFlows-Widget";

    private static final SimpleDateFormat lastReadingDateFmt = new SimpleDateFormat("h:mm aa");

    private int favoriteCount = 0;

    private long lastUpdated = 0;
    private List<SiteData> mWidgetItems = null;
    private Context mContext;
    private int mAppWidgetId;
    private Exception mException = null;

    public WidgetViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
    }

    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        List<SiteData> tmpWidgetItems = mWidgetItems;
        if(tmpWidgetItems != null) {
            tmpWidgetItems.clear();
        }
    }

    public int getCount() {
        return favoriteCount;
    }

    public RemoteViews getViewAt(int position) {
        // position will always range from 0 to getCount() - 1.

        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        System.out.println("Loading view " + position);

        updateFavoriteViews(mContext, rv, position);

        // Return the remote views object.
        return rv;
    }

    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged");

        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.
        try {
            mWidgetItems = getFavorites(mContext);
            lastUpdated = System.currentTimeMillis();
            mException = null;
        } catch (Exception e) {
            favoriteCount = 0;
            Log.e(TAG, "load failed", e);
            mException = e;
            if(System.currentTimeMillis() - lastUpdated > 60000) {
                if(mException instanceof LoadFailedException) {
                    LoadFailedException lfe = (LoadFailedException)mException;

                    switch (lfe.getErrorCode()) {
                        case -2:

                            Log.w(TAG,"riverFlows out of date");

                            reportCompletion("The RiverFlows app is out-of-date for this version of the widget.",
                                    "Update RiverFlows",
                                    Intent.ACTION_VIEW,
                                    "market://details?id=com.riverflows");

                            return;
                    }
                    Log.w(TAG, "load failed. errorCode: " + lfe.getErrorCode());
                } else {
                    Log.e(TAG, "load failed", mException);
                }
                return;
            }
        }

        if(mWidgetItems == null) {
            favoriteCount = 0;
            Log.w(TAG,"RiverFlows not installed");
            reportCompletion("The RiverFlows app must be installed in order to use this widget",
                    "Install RiverFlows",
                    Intent.ACTION_VIEW,
                    "market://details?id=com.riverflows");

            return;
        }

        favoriteCount = mWidgetItems.size();

        if(favoriteCount == 0) {
            Log.w(TAG,"no mWidgetItems defined");

            reportCompletion("Your first 5 favorite sites from the RiverFlows app will appear here.");

            return;
        }

        reportCompletion();
    }

    private void reportCompletion() {
        reportCompletion(null);
    }

    private void reportCompletion(String message) {
        reportCompletion(message, null, null, null);
    }

    private void reportCompletion(String message, String buttonText, String intentAction, String intentUri) {
        Intent completionIntent = new Intent(mContext, Provider.class);
        completionIntent.setAction(Provider.ACTION_UPDATE_COMPLETE);
        completionIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        completionIntent.putExtra(Provider.EN_ERROR_MESSAGE, message);
        completionIntent.putExtra(Provider.EN_ERROR_BUTTON_TEXT, buttonText);
        completionIntent.putExtra(Provider.EN_ERROR_BUTTON_INTENT_ACTION, intentAction);
        completionIntent.putExtra(Provider.EN_ERROR_BUTTON_INTENT_URI, intentUri);

        mContext.sendBroadcast(completionIntent);
    }

    @SuppressWarnings("serial")
    private static class LoadFailedException extends Exception {

        private int errorCode;

        public LoadFailedException(int errorCode) {
            super();
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return this.errorCode;
        }

    }

    @SuppressWarnings("serial")
    private static class UpdateAbortedException extends Exception {
        public UpdateAbortedException() {}
    }

    private RemoteViews updateFavoriteViews(Context context, RemoteViews views, int favPosition) {

        if(favPosition >= mWidgetItems.size()) {
            views.setViewVisibility(R.id.favorite, View.INVISIBLE);
            return views;
        }

        Log.d(TAG, "drawing favorite " + mWidgetItems.get(favPosition).getSite().getName());

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        extras.putString(Provider.EN_SITE_ID, mWidgetItems.get(favPosition).getSite().getSiteId().toString());
        extras.putString(Provider.EN_VARIABLE_ID, mWidgetItems.get(favPosition).getDatasets().values().iterator().next().getVariable().getId());
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        views.setOnClickFillInIntent(R.id.favorite, fillInIntent);

        views.setTextViewText(R.id.favorite_name, mWidgetItems.get(favPosition).getSite().getName());

        //display the last reading for this site, if present
        Series flowSeries = DataSourceController.getPreferredSeries(mWidgetItems.get(favPosition));
        Reading lastReading = getLastReading(flowSeries);

        Log.d(TAG, "last reading time: " + (lastReading == null ? "null" : lastReading.getDate()));

        //only show the reading if it is less than 6 hours old
        if(lastReading != null && lastReading.getValue() != null &&
                (lastReading.getDate().getTime() + (6 * 60 * 60 * 1000)) > System.currentTimeMillis()) {

            views.setTextViewText(R.id.subtext, getLastReadingText(lastReading, flowSeries.getVariable().getUnit()));

            views.setTextViewText(R.id.timestamp, getLastReadingTimestamp(lastReading));
        }

        String siteAgency = mWidgetItems.get(favPosition).getSite().getAgency();
        Integer agencyIconResId = getAgencyIconResId(siteAgency);
        if(agencyIconResId != null) {
            views.setImageViewResource(R.id.agency_icon, agencyIconResId);
        } else {
            Log.e(TAG, "no icon for agency: " + siteAgency);
            views.setViewVisibility(R.id.agency_icon, View.GONE);
        }

        views.setViewVisibility(R.id.favorite, View.VISIBLE);

        return views;
    }

    private String getLastReadingText(Reading lastReading, String unit) {

        if(lastReading == null) {
            return "";
        }

        if(lastReading.getValue() == null) {
            if(lastReading.getQualifiers() == null) {
                return "unknown";
            }

            return lastReading.getQualifiers();
        }

        //use this many significant figures for decimal values
        int sigfigs = 4;

        String readingStr = Utils.abbreviateNumber(lastReading.getValue(), sigfigs);

        return readingStr + " " + unit;
    }

    private String getLastReadingTimestamp(Reading r) {

        return lastReadingDateFmt.format(r.getDate());

    }

    private Reading getLastReading(Series s) {
        if(s == null)
            return null;

        if(s.getReadings() == null) {
            Log.e(TAG, "null readings");
            return null;
        }

        try {
            Reading lastReading = s.getLastObservation();

            if(lastReading == null) {
                Log.i(TAG, "null reading");
                return null;
            }

            return lastReading;

        } catch(IndexOutOfBoundsException ioobe) {
            //there will be an empty readings list if the site has no recent reading
            return null;
        }

    }

    public static Integer getAgencyIconResId(String siteAgency) {
        if(UsgsCsvDataSource.AGENCY.equals(siteAgency)) {
            return R.drawable.usgs;
        } else if(AHPSXmlDataSource.AGENCY.equals(siteAgency)) {
            return R.drawable.ahps;
        } else if(CODWRDataSource.AGENCY.equals(siteAgency)) {
            return R.drawable.codwr;
        } else if(CDECDataSource.AGENCY.equals(siteAgency)) {
            return R.drawable.cdec;
        } else if(USACEDataSource.AGENCY.equals(siteAgency)) {
            return R.drawable.usace;
        }
        return null;
    }

    /**
     * @param context
     * @return null if the Favorites ContentProvider cannot be found
     * @throws LoadFailedException if the favorites could not be loaded for some other reason
     */
    private List<SiteData> getFavorites(Context context) throws LoadFailedException {

        ContentResolver cr = context.getContentResolver();

        Log.v(TAG,"testing for content provider");

        //com.riverflows.content.Favorites.CONTENT_URI
        Cursor favoritesC = cr.query(Uri.parse("content://com.riverflows.content.favorites?uLimit=5&version=1"), null, null, null, null);

//Test single-result favorites service
//	        Cursor favoritesC = cr.query(Uri.parse("content://com.riverflows.content.favorites/USGS/09119000/00060?version=1"), null, null, null, null);

        if(favoritesC == null) {
            Log.d(TAG, "null cursor!");
            return null;
        }

        int errorCode = favoritesC.getExtras().getInt("errorCode");

        if(errorCode < 0) {
            throw new LoadFailedException(errorCode);
        }

        List<SiteData> favorites = new ArrayList<SiteData>();

        if(favoritesC.getCount() == 0) {
            favoritesC.close();
            return favorites;
        }

        if(!favoritesC.moveToFirst()) {
            favoritesC.close();
            return favorites;
        }

        do {
            SiteData favoriteData = new SiteData();

            Site favoriteSite = new Site();

            SiteId siteId = new SiteId(favoritesC.getString(1), favoritesC.getString(0));

            favoriteSite.setSiteId(siteId);
            favoriteSite.setName(favoritesC.getString(2));
            //favoriteSite.setLatitude(favoritesC.getDouble(3));
            //favoriteSite.setLongitude(favoritesC.getDouble(4));
            favoriteData.setSite(favoriteSite);
            //favoriteData.setDataInfo(dataInfo);

            String variableId = favoritesC.getString(3);

            Reading lastReading = new Reading();
            lastReading.setDate(new Date(favoritesC.getLong(4)));
            lastReading.setValue(favoritesC.getDouble(5));
            lastReading.setQualifiers(favoritesC.getString(6));

            String unit = null;

            if(favoritesC.getColumnCount() > 7) {
                unit = favoritesC.getString(7);
                //Log.v(TAG, "unit=" + unit);
            }

            Variable var = DataSourceController.getVariable(siteId.getAgency(), variableId);

            Series s = new Series();

            s.setVariable(var);
            s.setReadings(Collections.singletonList(lastReading));

            if(var != null) {

                Variable.CommonVariable convertedVar = null;

                if(unit != null) {
                    convertedVar = Variable.CommonVariable.getByNameAndUnit(var.getCommonVariable().getName(), unit);
                    //Log.v(TAG, "convertedVar unit=" + convertedVar.getUnit());
                }

                if(convertedVar != null) {
                    Variable newVar = new Variable(convertedVar, var.getId(), var.getMagicNullValue());

                    s.setVariable(newVar);

                    favoriteData.getDatasets().put(convertedVar, s);
                } else {
                    favoriteData.getDatasets().put(var.getCommonVariable(), s);
                }
            } else {
                Log.e(TAG, "could not find variable: " + siteId.getAgency() + " " + variableId);
                continue;
            }
            favorites.add(favoriteData);
        } while(favoritesC.moveToNext());

        favoritesC.close();

        return favorites;
    }
}
