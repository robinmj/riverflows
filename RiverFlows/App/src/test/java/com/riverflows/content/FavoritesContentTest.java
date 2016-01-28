package com.riverflows.content;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.riverflows.MockWsClient;
import com.riverflows.RobinSession;
import com.riverflows.RobolectricGradleTestRunner;
import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.SiteData;
import com.riverflows.factory.DestinationFacetFactory;
import com.riverflows.factory.SiteDataFactory;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import roboguice.RoboGuice;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by robin on 1/18/16.
 */
@RunWith(RobolectricGradleTestRunner.class)
public class FavoritesContentTest {
	private MockWsClient wsClient = new MockWsClient();
	private RobinSession robinSession = new RobinSession();
	Favorites contentProvider = new Favorites();

	private ContentResolver mContentResolver;
	private ShadowContentResolver mShadowContentResolver;

	@Before
	public void setup() {

		RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, wsClient, robinSession);

		mContentResolver = RuntimeEnvironment.application.getContentResolver();
		mShadowContentResolver = shadowOf(mContentResolver);
		contentProvider.onCreate();
		contentProvider.attachInfo(RuntimeEnvironment.application, null);
		ShadowContentResolver.registerProvider("com.riverflows.content.favorites", contentProvider);
	}

	@Test
	public void shouldReturnFavorites() throws Exception {

		SiteData apalachicolaData = SiteDataFactory.getApalachicolaData();

		List<FavoriteData> mockData = new ArrayList<FavoriteData>();
		final Favorite clearCreekFav = new Favorite(DestinationFacetFactory.getClearCreekKayak());
		final Favorite fountainCreekFav = new Favorite(DestinationFacetFactory.getFountainCreekKayak());
		final Favorite apalachicolaFav = new Favorite(apalachicolaData.getSite(), UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT.getId());

		SiteData clearCreekData = SiteDataFactory.getClearCreekData();
		FavoriteData clearCreekFavData = new FavoriteData(clearCreekFav,clearCreekData, CODWRDataSource.VTYPE_STREAMFLOW_CFS);
		mockData.add(clearCreekFavData);

		SiteData fountainCreekData = SiteDataFactory.getFountainCreekData();
		FavoriteData fountainCreekFavData = new FavoriteData(fountainCreekFav, fountainCreekData, UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT);
		mockData.add(fountainCreekFavData);

		mockData.add(new FavoriteData(apalachicolaFav, apalachicolaData, UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT));

		when(wsClient.dsControllerMock.getFavoriteData(anyList(), anyBoolean())).thenReturn(mockData);

		Cursor cursor = mShadowContentResolver.query(Uri.parse("content://com.riverflows.content.favorites?uLimit=5&version=1"), null, null, null,null);

		assertThat("has results", cursor.moveToFirst());

		assertThat(cursor.getString(0), equalTo(clearCreekFav.getSite().getId()));
		assertThat(cursor.getString(1), equalTo("CODWR"));
		assertThat(cursor.getString(2), equalTo("Terrible Destination"));
		assertThat(cursor.getString(3), equalTo("DISCHRG"));

		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2011, Calendar.MARCH, 7, 13, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);

		assertThat(cursor.getLong(4), equalTo(cal.getTimeInMillis()));
		assertThat(cursor.getDouble(5), equalTo(0.0d));
		assertThat(cursor.getString(6), equalTo("B"));

		assertThat("has fountain creek info", cursor.moveToNext());

		assertThat(cursor.getString(0), equalTo(fountainCreekFav.getSite().getId()));
		assertThat(cursor.getString(1), equalTo("USGS"));
		assertThat(cursor.getString(2), equalTo("Excellent Destination"));
		assertThat(cursor.getString(3), equalTo("00065"));

		cal.set(2011, Calendar.MARCH, 7, 13, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);

		//assertThat(cursor.getLong(4), equalTo(cal.getTimeInMillis()));
		//assertThat(cursor.getDouble(5), equalTo(50.0d));
		assertThat(cursor.getLong(4), equalTo(0l));
		assertThat(cursor.getDouble(5), equalTo(0.0d));
		assertThat(cursor.getString(6), nullValue());

		assertThat("has apalachicola info", cursor.moveToNext());

		assertThat(cursor.getString(0), equalTo(apalachicolaFav.getSite().getId()));
		assertThat(cursor.getString(1), equalTo("USGS"));
		assertThat(cursor.getString(2), equalTo("APALACHICOLA R.AB CHIPOLA CONR WEWAHITCHKA,FLA"));
		assertThat(cursor.getString(3), equalTo("00065"));

		cal.set(2015, 9, 18, 10, 15, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.setTimeZone(TimeZone.getTimeZone("GMT-05:00"));

		assertThat(cursor.getLong(4), equalTo(cal.getTimeInMillis()));
		assertThat(cursor.getDouble(5), equalTo(14.58d));
		assertThat(cursor.getString(6), nullValue());

	}
}
