package com.riverflows.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.riverflows.Home;
import com.riverflows.ViewChart;
import com.riverflows.content.Favorites;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.Utils;

public class Provider extends AppWidgetProvider {
	
	public static final String TAG = "RiverFlows-Widget";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            
            List<SiteData> favorites = getFavorites(context);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
            
            int favoriteCount = 1;
            
            for(int a = 0; a < favoriteCount && a < favorites.size(); a++) {

                Intent intent = new Intent(Intent.ACTION_VIEW,Uri.fromParts(ViewChart.GAUGE_SCHEME,
                		favorites.get(a).getSite().getSiteId().toString(),
                		favorites.get(a).getDatasets().values().iterator().next().getVariable().getId()));
                //intent.putExtra(ViewChart.KEY_SITE, favorites.get(a).getSite());
                //intent.putExtra(ViewChart.KEY_VARIABLE, favorites.get(a).getDatasets().values().iterator().next().getVariable());
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                
                views.setOnClickPendingIntent(getFavoriteViewId(a), pendingIntent);
            	
            	views.setTextViewText(getTextViewId(a), favorites.get(a).getSite().getName());

            	views.setTextViewText(getSubtextViewId(a), getLastReadingText(favorites.get(a)));
            	
                String siteAgency = favorites.get(a).getSite().getAgency();
                Integer agencyIconResId = Home.getAgencyIconResId(siteAgency);
                if(agencyIconResId != null) {
                	views.setImageViewResource(getAgencyIconViewId(a), agencyIconResId);
                } else {
                	Log.e(TAG, "no icon for agency: " + siteAgency);
                	views.setViewVisibility(getAgencyIconViewId(a), View.GONE);
                }
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
	}
	
	private String getLastReadingText(SiteData data) {

        //display the last reading for this site, if present
        Series flowSeries = DataSourceController.getPreferredSeries(data);
    	Reading lastReading = getLastReadingValue(flowSeries);
    	
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
		
		return readingStr + " " + flowSeries.getVariable().getUnit();
	}
	
	private Reading getLastReadingValue(Series s) {
		if(s == null)
			return null;
		
		if(s.getReadings() == null) {
			Log.e(TAG, "null readings");
			return null;
		}
		
		try {
			Reading lastReading = DataSourceController.getLastObservation(s);
			
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
	
	private int getFavoriteViewId(int index) {
		switch(index) {
		case 0:
			return R.id.favorite_0;
		}
		throw new IllegalArgumentException();
	}
	
	private int getTextViewId(int index) {
		switch(index) {
		case 0:
			return R.id.favorite_name_0;
		}
		throw new IllegalArgumentException();
	}
	
	private int getSubtextViewId(int index) {
		switch(index) {
		case 0:
			return R.id.subtext_0;
		}
		throw new IllegalArgumentException();
	}
	
	private int getAgencyIconViewId(int index) {
		switch(index) {
		case 0:
			return R.id.agency_icon_0;
		}
		throw new IllegalArgumentException();
	}
	
	private List<SiteData> getFavorites(Context context) {

        ContentResolver cr = context.getContentResolver();
        
        Cursor favoritesC = cr.query(Favorites.CONTENT_URI, null, null, null, null);
		
        List<SiteData> favorites = new ArrayList<SiteData>();
        
        if(favoritesC.getCount() == 0) {
        	favoritesC.close();
        	return favorites;
		}
        
        if(!favoritesC.moveToFirst()) {
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
			
			Variable var = DataSourceController.getVariable(siteId.getAgency(), variableId);
			
			Series s = new Series();
			
			s.setVariable(var);
			s.setReadings(Collections.singletonList(lastReading));
			
			if(var != null) {
				favoriteData.getDatasets().put(var.getCommonVariable(), s);
			} else {
				Log.e(TAG, "could not find variable: " + siteId.getAgency() + " " + variableId);
				continue;
			}
			favorites.add(favoriteData);
		} while(favoritesC.moveToNext());
		
		return favorites;
	}
}
