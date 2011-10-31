package com.riverflows.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.Utils;

public class Provider extends AppWidgetProvider {
	
	public static final String TAG = "RiverFlows-Widget";

	private static final SimpleDateFormat lastReadingDateFmt = new SimpleDateFormat("h:mm aa");
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		context.startService(new Intent(context, UpdateService.class));
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
	
	public static class UpdateService extends Service {
		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}
		
		@Override
		public void onStart(Intent intent, int startId) {
			RemoteViews views = buildRemoteViews(this);
			
			if(views == null) {
				return;
			}
			
            ComponentName thisWidget = new ComponentName(this, Provider.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            appWidgetManager.updateAppWidget(thisWidget, views);
		}
		
		public RemoteViews buildRemoteViews(Context context) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
            views.setViewVisibility(R.id.spinner, View.VISIBLE);
        	views.setViewVisibility(R.id.empty_message_area, View.GONE);
        	views.setViewVisibility(R.id.reload_instructions, View.GONE);
            //views.setViewVisibility(R.id.reload_button, View.GONE);
            
            int favoriteCount = 5;
            
            for(int a = 0; a < favoriteCount; a++) {
                views.setViewVisibility(getFavoriteViewId(a), View.GONE);
            }
	        
	        List<SiteData> favorites = null;
	        try {
	        	favorites = getFavorites(context);
	        } catch(LoadFailedException lfe) {
	        	switch(lfe.getErrorCode()) {
	        	case -2:
	        		return showUnsupportedProtocolError(context, views);
	        	}
	        	Log.w(TAG,"load failed. errorCode: " + lfe.getErrorCode());
	        	return null;
	        } catch(Exception e) {
	        	Log.e(TAG, "load failed", e);
	        	return null;
	        }

            views.setViewVisibility(R.id.spinner, View.GONE);
	        
	        if(favorites == null) {
	        	Log.w(TAG,"RiverFlows not installed");
	        	views.setTextViewText(R.id.empty_message, "The RiverFlows app must be installed in order to use this widget");
	        	views.setCharSequence(R.id.empty_message_button, "setText","Install RiverFlows");
	        	Intent appDetailsIntent = new Intent(Intent.ACTION_VIEW, 
	        			Uri.parse("market://details?id=com.riverflows"));
	        	/*Intent appDetailsIntent = new Intent(Intent.ACTION_VIEW, 
	        			Uri.parse("https://market.android.com/details?id=com.riverflows"));*/
	        	PendingIntent appDetailsPendingIntent = PendingIntent.getActivity(context, 0, appDetailsIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
	        	views.setOnClickPendingIntent(R.id.empty_message_button, appDetailsPendingIntent);
	        	views.setViewVisibility(R.id.empty_message_area, View.VISIBLE);
	        	return views;
	        }
	        
	        if(favorites.size() == 0) {
	        	Log.w(TAG,"no favorites defined");
	        	
//Would prefer to do it this way, but the link doesn't work for some reason
//	    		SpannableString instructions = new SpannableString("If you select your favorite gauge sites, they will appear here.");
//	    		instructions.setSpan(new URLSpan("riverflows://help/favorites.html"), 7, 39, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

//	        	views.setViewVisibility(R.id.empty_message_button, View.GONE);
//	        	views.setTextViewText(R.id.empty_message, instructions);
	        	
	        	views.setTextViewText(R.id.empty_message, "Your first 5 favorite sites from the RiverFlows app will appear here.");
	        	views.setCharSequence(R.id.empty_message_button, "setText", "Instructions For Selecting Favorites");
	        	
	        	Intent favoritesHelpIntent = new Intent(Intent.ACTION_VIEW,
	        			Uri.parse("riverflows://help/favorites.html"));
	        	PendingIntent favoritesHelpPendingIntent = PendingIntent.getActivity(context, 0, favoritesHelpIntent, 0);
	        	views.setOnClickPendingIntent(R.id.empty_message_button, favoritesHelpPendingIntent);
	        	views.setViewVisibility(R.id.empty_message_area, View.VISIBLE);
	        	views.setViewVisibility(R.id.reload_instructions, View.VISIBLE);
	        	return views;
	        }
            
            for(int a = 0; a < favoriteCount && a < favorites.size(); a++) {
            	Log.d(TAG, "drawing favorite " + favorites.get(a).getSite().getName());

            	//com.riverflows.ViewChart.GAUGE_SCHEME
                Intent intent = new Intent(Intent.ACTION_VIEW,Uri.fromParts("gauge",
                		favorites.get(a).getSite().getSiteId().toString(),
                		favorites.get(a).getDatasets().values().iterator().next().getVariable().getId()));
                //intent.putExtra(ViewChart.KEY_SITE, favorites.get(a).getSite());
                //intent.putExtra(ViewChart.KEY_VARIABLE, favorites.get(a).getDatasets().values().iterator().next().getVariable());
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                
                views.setOnClickPendingIntent(getFavoriteViewId(a), pendingIntent);

	        	views.setTextViewText(getTextViewId(a), favorites.get(a).getSite().getName());

	            //display the last reading for this site, if present
	            Series flowSeries = DataSourceController.getPreferredSeries(favorites.get(a));
	        	Reading lastReading = getLastReading(flowSeries);
	        	
	        	Log.d(TAG, "last reading time: " + (lastReading == null ? "null" : lastReading.getDate()));

	        	//only show the reading if it is less than 6 hours old
        		if(lastReading != null && lastReading.getValue() != null &&
        				(lastReading.getDate().getTime() + (6 * 60 * 60 * 1000)) > System.currentTimeMillis()) {
		
		        	views.setTextViewText(getSubtextViewId(a), getLastReadingText(lastReading, flowSeries.getVariable().getUnit()));
		        	
		        	views.setTextViewText(getTimestampViewId(a), getLastReadingTimestamp(lastReading));
        		}
            	
                String siteAgency = favorites.get(a).getSite().getAgency();
                Integer agencyIconResId = getAgencyIconResId(siteAgency);
                if(agencyIconResId != null) {
                	views.setImageViewResource(getAgencyIconViewId(a), agencyIconResId);
                } else {
                	Log.e(TAG, "no icon for agency: " + siteAgency);
                	views.setViewVisibility(getAgencyIconViewId(a), View.GONE);
                }

                views.setViewVisibility(getFavoriteViewId(a), View.VISIBLE);
            }
            
            /*
            Intent reloadIntent = new Intent(context,UpdateService.class);
            
            PendingIntent reloadPendingIntent = PendingIntent.getActivity(context, 0, reloadIntent, 0);
            
            views.setOnClickPendingIntent(R.id.reload_button, reloadPendingIntent);
            views.setViewVisibility(R.id.reload_button, View.VISIBLE);
            */
            
            return views;
		}
		
		private RemoteViews showUnsupportedProtocolError(Context context, RemoteViews views) {
			views.setViewVisibility(R.id.spinner, View.GONE);
	        
        	Log.w(TAG,"riverFlows out of date");
        	views.setTextViewText(R.id.empty_message, "The RiverFlows app is out-of-date for this version of the widget.");
        	views.setCharSequence(R.id.empty_message_button, "setText","Update RiverFlows");
        	Intent appDetailsIntent = new Intent(Intent.ACTION_VIEW, 
        			Uri.parse("market://details?id=com.riverflows"));
        	PendingIntent appDetailsPendingIntent = PendingIntent.getActivity(context, 0, appDetailsIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
        	views.setOnClickPendingIntent(R.id.empty_message_button, appDetailsPendingIntent);
        	views.setViewVisibility(R.id.empty_message_area, View.VISIBLE);
        	
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
		
		private int getFavoriteViewId(int index) {
			switch(index) {
			case 0:
				return R.id.favorite_0;
			case 1:
				return R.id.favorite_1;
			case 2:
				return R.id.favorite_2;
			case 3:
				return R.id.favorite_3;
			case 4:
				return R.id.favorite_4;
			}
			throw new IllegalArgumentException();
		}
		
		private int getTextViewId(int index) {
			switch(index) {
			case 0:
				return R.id.favorite_name_0;
			case 1:
				return R.id.favorite_name_1;
			case 2:
				return R.id.favorite_name_2;
			case 3:
				return R.id.favorite_name_3;
			case 4:
				return R.id.favorite_name_4;
			}
			throw new IllegalArgumentException();
		}
		
		private int getSubtextViewId(int index) {
			switch(index) {
			case 0:
				return R.id.subtext_0;
			case 1:
				return R.id.subtext_1;
			case 2:
				return R.id.subtext_2;
			case 3:
				return R.id.subtext_3;
			case 4:
				return R.id.subtext_4;
			}
			throw new IllegalArgumentException();
		}
		
		private int getTimestampViewId(int index) {
			switch(index) {
			case 0:
				return R.id.timestamp_0;
			case 1:
				return R.id.timestamp_1;
			case 2:
				return R.id.timestamp_2;
			case 3:
				return R.id.timestamp_3;
			case 4:
				return R.id.timestamp_4;
			}
			throw new IllegalArgumentException();
		}
		
		private int getAgencyIconViewId(int index) {
			switch(index) {
			case 0:
				return R.id.agency_icon_0;
			case 1:
				return R.id.agency_icon_1;
			case 2:
				return R.id.agency_icon_2;
			case 3:
				return R.id.agency_icon_3;
			case 4:
				return R.id.agency_icon_4;
			}
			throw new IllegalArgumentException();
		}

		public static Integer getAgencyIconResId(String siteAgency) {
			if(UsgsCsvDataSource.AGENCY.equals(siteAgency)) {
	        	return R.drawable.usgs;
	        } else if(AHPSXmlDataSource.AGENCY.equals(siteAgency)) {
	        	return R.drawable.ahps;
	        } else if(CODWRDataSource.AGENCY.equals(siteAgency)) {
	        	return R.drawable.codwr;
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

        	favoritesC.close();
			
			return favorites;
		}
	}
}
