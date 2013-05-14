package com.riverflows.content;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.riverflows.Home;
import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.ValueConverter;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.RiverGaugesDb;
import com.riverflows.wsclient.DataSourceController;

public class Favorites extends ContentProvider {
	
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
		SharedPreferences settings = getContext().getSharedPreferences(Home.PREFS_FILE, Context.MODE_PRIVATE);
    	String tempUnit = settings.getString(Home.PREF_TEMP_UNIT, null);
    	
    	unitConversionMap = CommonVariable.temperatureConversionMap(tempUnit);
    	
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
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
			
			Map<SiteId,SiteData> allSiteDataMap = new HashMap<SiteId,SiteData>();
			
			try {
				Map<SiteId,SiteData> siteDataMap = DataSourceController.getSiteData(favorites, true);
				
				for(SiteData currentData: siteDataMap.values()) {
					
					//convert °C to °F if that setting is enabled
					Map<CommonVariable,Series> datasets = currentData.getDatasets();
					for(Series dataset: datasets.values()) {
						ValueConverter.convertIfNecessary(unitConversionMap, dataset);
					}
					
					allSiteDataMap.put(currentData.getSite().getSiteId(), currentData);
				}
			} catch(UnknownHostException uhe) {
				result.getExtras().putInt(EXTRA_ERROR_CODE, ERROR_UNKNOWN_HOST);
				return result;
			} catch(IOException ioe) {
				Log.e(TAG, "", ioe);
				result.getExtras().putInt(EXTRA_ERROR_CODE, ERROR_NETWORK);
				return result;
			}
			
			List<SiteData> favoriteSiteData = expandDatasets(favorites, allSiteDataMap);
			
			for(SiteData siteData : favoriteSiteData) {
				try {
					MatrixCursor.RowBuilder row = result.newRow();
					
					row.add(siteData.getSite().getSiteId().getId());
					row.add(siteData.getSite().getSiteId().getAgency());
					row.add(siteData.getSite().getName());
					
					Iterator<Series> siteDataSeries = siteData.getDatasets().values().iterator();
					
					if(siteDataSeries.hasNext()) {
						Series series = siteDataSeries.next();
						
						row.add(series.getVariable().getId());
						
						Reading lastReading = series.getLastObservation();
						if(lastReading != null) {
							row.add(lastReading.getDate().getTime());
							row.add(lastReading.getValue());
							row.add(lastReading.getQualifiers());
							row.add(series.getVariable().getCommonVariable().getUnit());
						}
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
	
	//TODO need a datatype that contains both the Favorite and SiteData so this is no longer necessary
	// this code is cut-n-pasted from the Favorites activity to the Favorites content provider
	private List<SiteData> expandDatasets(List<Favorite> favorites, Map<SiteId,SiteData> siteDataMap) {
		ArrayList<SiteData> expandedDatasets = new ArrayList<SiteData>(favorites.size());
		
		//build a list of SiteData objects corresponding to the list of favorites
		for(Favorite favorite: favorites) {
			SiteData current = siteDataMap.get(favorite.getSite().getSiteId());
			
			if(current == null) {
				continue;
			}
			
			Variable favoriteVar = DataSourceController.getVariable(favorite.getSite().getAgency(), favorite.getVariable());
			
			if(favoriteVar == null) {
				throw new NullPointerException("could not find variable: " + favorite.getSite().getAgency() + " " + favorite.getVariable());
			}

			//use custom name if one is defined
			if(favorite.getName() != null) {
				current.getSite().setName(favorite.getName());
			}
			
			if(current.getDatasets().size() <= 1) {
				expandedDatasets.add(current);
				continue;
			}
			
			//use the dataset for this favorite's variable
			Series dataset = current.getDatasets().get(favoriteVar.getCommonVariable());
			SiteData expandedDataset = new SiteData();
			expandedDataset.setSite(current.getSite());
			expandedDataset.getDatasets().put(favoriteVar.getCommonVariable(), dataset);
			expandedDatasets.add(expandedDataset);
		}
		return expandedDatasets;
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
