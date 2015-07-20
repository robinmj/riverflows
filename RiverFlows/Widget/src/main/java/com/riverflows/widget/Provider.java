package com.riverflows.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.CDECDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.USACEDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.Utils;

public class Provider extends AppWidgetProvider {
	
	public static final String TAG = "RiverFlows-Widget";
	
	public static final String ACTION_UPDATE_WIDGET = "com.riverflows.widget.UPDATE";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
        Log.d(Provider.TAG, "onUpdate " + appWidgetIds.length);

        // update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {

            // Here we setup the intent which points to the StackViewService which will
            // provide the views for this collection.
            Intent intent = new Intent(context, Service.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            // When intents are compared, the extras are ignored, so we need to embed the extras
            // into the data so that the extras will not be ignored.
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.main);
            rv.setRemoteAdapter(android.R.id.list, intent);

            // The empty view is displayed when the collection has no items. It should be a sibling
            // of the collection view.
            rv.setEmptyView(android.R.id.list, R.id.empty_message_area);

            // Here we setup the a pending intent template. Individuals items of a collection
            // cannot setup their own pending intents, instead, the collection as a whole can
            // setup a pending intent template, and the individual items can set a fillInIntent
            // to create unique before on an item to item basis.
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setData(Uri.parse(viewIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(android.R.id.list, pendingIntent);

            showReloadButton(context,rv);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
	}


	static Intent getUpdateIntent(Context ctx) {
		Intent updateIntent = new Intent(ctx,Provider.class);
		updateIntent.setAction(ACTION_UPDATE_WIDGET);
		return updateIntent;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if(ACTION_UPDATE_WIDGET.equals(intent.getAction())) {

			Log.d(TAG,"received update intent");
	        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.notifyAppWidgetViewDataChanged(Service.getWidgetIds(), android.R.id.list);
		}

        super.onReceive(context, intent);
    }

	private void showErrorMessage(Context context, RemoteViews views, String message, String buttonText, PendingIntent buttonIntent) {

        views.setViewVisibility(R.id.spinner, View.GONE);
		views.setTextViewText(R.id.empty_message, message);
    	views.setCharSequence(R.id.empty_message_button, "setText",buttonText);
    	views.setOnClickPendingIntent(R.id.empty_message_button, buttonIntent);
    	views.setViewVisibility(R.id.empty_message_button, View.VISIBLE);
    	views.setViewVisibility(R.id.empty_message_area, View.VISIBLE);
	}

	private void showReloadButton(Context context, RemoteViews views) {
        PendingIntent reloadPendingIntent = PendingIntent.getBroadcast(context, 0, Provider.getUpdateIntent(context), 0);

        views.setOnClickPendingIntent(R.id.reload_button, reloadPendingIntent);
        views.setViewVisibility(R.id.reload_button, View.VISIBLE);
	}
}
