package com.riverflows.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.riverflows.data.Favorite;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.RiverGaugesDb;
import com.riverflows.wsclient.DataSourceController;

public class Favorites extends ContentProvider {
	
	public static final String TAG = "FavoritesContentProvider";
	
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_AGENCY = "agency";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_VARIABLE_ID = "variableId";
	public static final String COLUMN_LAST_READING_DATE = "lastReadingDate";
	public static final String COLUMN_LAST_READING_VALUE = "lastReadingValue";
	public static final String COLUMN_LAST_READING_QUALIFIERS = "lastReadingQualifiers";
	
	public static final String[] ALL_COLUMNS = new String[]{
		COLUMN_ID,
		COLUMN_AGENCY,
		COLUMN_NAME,
		COLUMN_VARIABLE_ID,
		COLUMN_LAST_READING_DATE,
		COLUMN_LAST_READING_VALUE,
		COLUMN_LAST_READING_QUALIFIERS
		};

	public static final Uri CONTENT_URI = 
        Uri.parse("content://com.riverflows.content.favorites");

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
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if(uri.equals(CONTENT_URI)) {
			List<Favorite> favorites = FavoritesDaoImpl.getFavorites(getContext());
			
			//migrate any favorites without a variable set
			//checkForOldFavorites(favorites);
			
			Map<SiteId,SiteData> allSiteDataMap = new HashMap<SiteId,SiteData>();
			
			try {
				Map<SiteId,SiteData> siteDataMap = DataSourceController.getSiteData(favorites, true);
				
				for(SiteData currentData: siteDataMap.values()) {
					allSiteDataMap.put(currentData.getSite().getSiteId(), currentData);
				}
			} catch(IOException ioe) {
				Log.e(TAG, "", ioe);
				return null;
			}
			
			List<SiteData> favoriteSiteData = expandDatasets(favorites, allSiteDataMap);
			
			MatrixCursor result = new MatrixCursor(ALL_COLUMNS);
			
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
						
						Reading lastReading = DataSourceController.getLastObservation(series);
						if(lastReading != null) {
							row.add(lastReading.getDate());
							row.add(lastReading.getValue());
							row.add(lastReading.getQualifiers());
						}
					}
				} catch(NullPointerException npe) {
					Log.e(TAG, "",npe);
				}
			}
			
			return result;
		}
		return null;
	}
	
	private List<SiteData> expandDatasets(List<Favorite> favorites, Map<SiteId,SiteData> siteDataMap) {
		ArrayList<SiteData> expandedDatasets = new ArrayList<SiteData>(favorites.size());
		
		//build a list of SiteData objects corresponding to the list of favorites
		for(Favorite favorite: favorites) {
			SiteData current = siteDataMap.get(favorite.getSite().getSiteId());
			
			Variable favoriteVar = DataSourceController.getVariable(favorite.getSite().getAgency(), favorite.getVariable());
			
			if(favoriteVar == null) {
				throw new NullPointerException("could not find variable: " + favorite.getSite().getAgency() + " " + favorite.getVariable());
			}
			
			if(current == null) {
				//failed to get data for this site- create a placeholder item
				current = new SiteData();
				current.setSite(favorite.getSite());
				
				Series nullSeries = new Series();
				nullSeries.setVariable(favoriteVar);
				
				Reading placeHolderReading = new Reading();
				placeHolderReading.setDate(new Date());
				placeHolderReading.setQualifiers("Datasource Down");
				
				nullSeries.setReadings(Collections.singletonList(placeHolderReading));
				nullSeries.setSourceUrl("");
				
				current.getDatasets().put(favoriteVar.getCommonVariable(), nullSeries);
				
				expandedDatasets.add(current);
				continue;
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
}
