package com.riverflows.widget;

import android.appwidget.AppWidgetManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLog;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by robin on 1/3/16.
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class ServiceTest {
	private ContentResolver mContentResolver;
	private ShadowContentResolver mShadowContentResolver;
	private ContentProvider mProvider = mock(ContentProvider.class);
	private Service widgetService;
	private RemoteViewsService.RemoteViewsFactory remoteViewsFactory;

	@Before
	public void setup() {

		System.setProperty("robolectric.logging", "stdout");
		ShadowLog.setupLogging();

		Log.v(WidgetViewsFactory.TAG, "Robolectric application context: " + RuntimeEnvironment.application);

		mContentResolver = RuntimeEnvironment.application.getContentResolver();

		Log.v(WidgetViewsFactory.TAG, "Robolectric ContentResolver: " + mContentResolver);

		mShadowContentResolver = shadowOf(mContentResolver);
		mShadowContentResolver.registerProvider("com.riverflows.content.favorites", mProvider);

		Log.v(WidgetViewsFactory.TAG, "Robolectric Shadow ContentResolver: " + mShadowContentResolver);

		widgetService = Robolectric.setupService(Service.class);

		Intent widgetIntent = new Intent("show");
		widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 3);

		remoteViewsFactory = widgetService.onGetViewFactory(widgetIntent);
	}

	@Test
	public void testService() {

		MatrixCursor mockCursor = new MatrixCursor(new String[]{ "id", "agency", "name",
				"variableId", "lastReadingDate", "lastReadingValue", "lastReadingQualifiers", "unit",
				"favoriteId", "destFacetId", "tooHighLevel", "highLevel", "medLevel", "lowLevel"}, 1);

		MatrixCursor.RowBuilder row = mockCursor.newRow();

		row.add(123); //0
		row.add("USACE"); //1
		row.add("Site Name"); //2

		row.add("FLOW"); //3

		row.add(System.currentTimeMillis()); //4
		row.add(720.0d); //5
		row.add(null); //6
		row.add("cfs"); //7
		row.add(56); //8
		row.add(null); //9
		row.add(null); //10
		row.add(null); //11
		row.add(null); //12
		row.add(null); //13

		remoteViewsFactory.onDataSetChanged();

		when(mProvider.query(any(Uri.class), isNull(String[].class), isNull(String.class),
				isNull(String[].class), isNull(String.class)))
				.thenReturn(mockCursor);

		//Cursor favoritesC = RuntimeEnvironment.application.getContentResolver().query(Uri.parse("content://com.riverflows.content.favorites?uLimit=5&version=1"), null, null, null, null);

		//assertNotNull(favoritesC);

		RemoteViews views0 = remoteViewsFactory.getViewAt(0);
	}
}
