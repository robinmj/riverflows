package com.riverflows.content;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.inject.Inject;
import com.riverflows.Home;
import com.riverflows.data.Favorite;
import com.riverflows.data.FavoriteData;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.SiteId;
import com.riverflows.data.ValueConverter;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.RiverGaugesDb;
import com.riverflows.wsclient.DataSourceController;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.content.RoboContentProvider;

public class Favorites extends RoboContentProvider {
	
	public static final int VERSION = 1;
	
	
	public static final int ERROR_BAD_REQUEST = -1;
	public static final int ERROR_UNSUPPORTED_PROTOCOL_VERSION = -2;
	public static final int ERROR_UNKNOWN_HOST = -3;
	public static final int ERROR_NETWORK = -4;
	public static final int ERROR_OTHER = -5;
	
	public static final String TAG = Home.TAG;
	
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_AGENCY = "agency";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_VARIABLE_ID = "variableId";
	public static final String COLUMN_LAST_READING_DATE = "lastReadingDate";
	public static final String COLUMN_LAST_READING_VALUE = "lastReadingValue";
	public static final String COLUMN_LAST_READING_QUALIFIERS = "lastReadingQualifiers";
	public static final String COLUMN_UNIT = "unit";

	public static final String EXTRA_PRODUCT = "product";
	/** This refers to the version of this ContentProvider, not RiverFlows */
	public static final String EXTRA_VERSION = "version";
	/**
	 * negative error code -> fatal error
	 * error code of 0 ->  success
	 * positive error code -> non-fatal error
	 */
	public static final String EXTRA_ERROR_CODE = "errorCode";
	
	/**
	 * Last result that should be returned.
	 */
	public static final String URI_PARAM_ULIMIT = "uLimit";

	/**
	 * This refers to the version of this ContentProvider, not RiverFlows.
	 */
	public static final String URI_PARAM_VERSION = "version";
	
	public static final String[] ALL_COLUMNS = new String[]{
		COLUMN_ID,
		COLUMN_AGENCY,
		COLUMN_NAME,
		COLUMN_VARIABLE_ID,
		COLUMN_LAST_READING_DATE,
		COLUMN_LAST_READING_VALUE,
		COLUMN_LAST_READING_QUALIFIERS,
		COLUMN_UNIT
		};

	public static final Uri CONTENT_URI = 
        Uri.parse("content://com.riverflows.content.favorites");

	@Inject
	private DataSourceController dataSourceController;

	private Map<CommonVariable, CommonVariable> unitConversionMap;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return -1;
	}

	@Override
	public String getType(Uri uri) {
		if(!CONTENT_URI.equals(uri)) {
			return null;
		}
		return "vnd.android.cursor.dir/vnd.riverflows.favorite";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {

		RoboGuice.setUseAnnotationDatabases(false);

		super.onCreate();
		SharedPreferences settings = getContext().getSharedPreferences(Home.PREFS_FILE, Context.MODE_PRIVATE);
    	String tempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);
    	
    	unitConversionMap = CommonVariable.temperatureConversionMap(tempUnit);
    	
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if(System.currentTimeMillis() % 10l == 0) {
			EasyTracker.getInstance().setContext(getContext());
			EasyTracker.getTracker().sendEvent(getClass().getCanonicalName(), "query", "" + uri, null);
		}
		
		if(!uri.getAuthority().equals(CONTENT_URI.getAuthority())) {
			return null;
		}

		FavoritesCursor result = new FavoritesCursor(ALL_COLUMNS);
		
		try {
			
			SiteId siteId = null;
			String variable = null;
			Integer uLimit = null;
			
			//TODO define the product name elsewhere
			result.getExtras().putString(EXTRA_PRODUCT, "RiverFlows Lite");
			result.getExtras().putInt(EXTRA_VERSION, VERSION);

			//exit with an error if a newer version of the provider is requested
			String versionStr = uri.getQueryParameter(URI_PARAM_VERSION);
			if(Integer.parseInt(versionStr) > VERSION) {
				result.getExtras().putInt(EXTRA_ERROR_CODE, ERROR_UNSUPPORTED_PROTOCOL_VERSION);
				return result;
			}
			
			List<String> pathSegments = uri.getPathSegments();
			if(pathSegments.size() > 0) {
				if(pathSegments.size() != 3) {
					Log.e(TAG, "incomplete uri: " + uri + " pathSegments=" + pathSegments.size());
					result.getExtras().putInt(EXTRA_ERROR_CODE, ERROR_BAD_REQUEST);
					return result;
				}
				siteId = new SiteId(pathSegments.get(0), pathSegments.get(1));
				variable = pathSegments.get(2);
			} else {
				String uLimitStr = uri.getQueryParameter(URI_PARAM_ULIMIT);
				if(uLimitStr != null) {
					uLimit = new Integer(uLimitStr);
				}
			}
			
			// Sometime down the line, perhaps a lazy-loading Cursor can be returned instead?
			List<Favorite> favorites = FavoritesDaoImpl.getFavorites(getContext(), siteId, variable);
			
			if(uLimit != null) {
				favorites = favorites.subList(0, (favorites.size() < uLimit ? favorites.size() : uLimit));
			}

            List<FavoriteData> favoriteDataList = null;
			
			try {
				favoriteDataList = this.dataSourceController.getFavoriteData(favorites, true);
			} catch(UnknownHostException uhe) {
				result.getExtras().putInt(EXTRA_ERROR_CODE, ERROR_UNKNOWN_HOST);
				return result;
			} catch(IOException ioe) {
				Log.e(TAG, "", ioe);
				result.getExtras().putInt(EXTRA_ERROR_CODE, ERROR_NETWORK);
				return result;
			}

            for(FavoriteData currentData: favoriteDataList) {

                //convert °C to °F if that setting is enabled
                Map<CommonVariable,Series> datasets = currentData.getSiteData().getDatasets();
                for(Series dataset: datasets.values()) {
                    ValueConverter.convertIfNecessary(unitConversionMap, dataset);
                }

				try {
					MatrixCursor.RowBuilder row = result.newRow();
					
					row.add(currentData.getSiteData().getSite().getSiteId().getId());
					row.add(currentData.getSiteData().getSite().getSiteId().getAgency());
					row.add(currentData.getName());

                    Series series = currentData.getSeries();

                    row.add(series.getVariable().getId());

                    Reading lastReading = series.getLastObservation();
                    if(lastReading != null) {
                        row.add(lastReading.getDate().getTime());
                        row.add(lastReading.getValue());
                        row.add(lastReading.getQualifiers());
                        row.add(series.getVariable().getCommonVariable().getUnit());
                    }
				} catch(NullPointerException npe) {
					Log.e(TAG, "",npe);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "",e);
			result.getExtras().putInt(EXTRA_ERROR_CODE, ERROR_OTHER);
			return result;
		}
		
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return -1;
	}

	@Override
	protected void finalize() throws Throwable {
		RiverGaugesDb.closeHelper();
	}
	
	private class FavoritesCursor extends MatrixCursor {
		private Bundle extras = new Bundle();
		
		public FavoritesCursor(String[] columnNames, int initialCapacity) {
			super(columnNames, initialCapacity);
		}

		public FavoritesCursor(String[] columnNames) {
			super(columnNames);
		}

		@Override
		public Bundle getExtras() {
			return this.extras;
		}
	}
}
