package com.riverflows.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.DataSourceController;

/**
 * Sites Table- for caching information on gauge sites.
 * @author robin
 *
 */
public class SitesDaoImpl {
	
	private static final String TAG = SitesDaoImpl.class.getSimpleName();
	
	static final String NAME = "sites";
	
	static final String ID = "id";

	/**
	 * Name of the site (String)
	 */
	static final String SITE_NAME = "siteName";
	
	/**
	 * string specifying the agency that maintains this site.
	 */
	static final String AGENCY = "agency";
	
	/**
	 * string identifying the site within the agency
	 */
	static final String SITE_ID = "siteId";
	
	/**
	 * float identifying the longitude coordinate of the station
	 */
	static final String LONGITUDE = "longitude";
	
	/**
	 * float identifying the longitude coordinate of the station
	 */
	static final String LATITUDE = "latitude";

    /**
     * The timestamp for when the site was (re)loaded
     * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
     */
    static final String TIME_FOUND = "timeFound";
    
    /**
     * Postal abbreviation (string) for the US state in which this gauge resides.
     */
    static final String STATE = "state";
    
    /**
     * last reading of the preferred variable from this site
     */
    static final String LAST_READING = "lastReading";
    
    /**
     * date when the LAST_READING column was updated
     */
    static final String LAST_READING_TIME = "lastReadingTime";
    
    /**
     * string identifier for the variable that LAST_READING refers to.
     */
    static final String LAST_READING_VAR = "lastReadingVariable";
    
    /**
     * space-separated list of agency-specific string identifiers for the variables supported by this site
     */
    static final String SUPPORTED_VARS = "supportedVariables";

	
    static final String CREATE_SQL = "CREATE TABLE " + NAME
		+ " ( " + ID + " INTEGER PRIMARY KEY,"
		+ SITE_ID + " TEXT,"
		+ SITE_NAME + " TEXT,"
		+ AGENCY + " TEXT,"
		+ TIME_FOUND + " INTEGER,"
		+ STATE + " TEXT,"
		+ LATITUDE + " REAL,"
		+ LONGITUDE + " REAL,"
		+ LAST_READING + " REAL,"
		+ LAST_READING_TIME + " REAL,"
		+ LAST_READING_VAR + " TEXT,"
		+ SUPPORTED_VARS + " TEXT );";
    

    /**
     * @param ctx
     * @param state the state that the resultset should be limited to
     * @param staleDate unless EVERY record in the resultset has a {@link #TIME_FOUND} date after staleDate, null will be returned, signaling that the resultset needs to be refreshed.
     * @return the sites in the given state, or an empty list if none, or null if the staleDate check fails
     */
	public static List<SiteData> getSitesInState(Context ctx, USState state, Date staleDate) {
		
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
		
		List<SiteData> result = null;
		
		try {
			Cursor c = db.query(NAME, new String[] { ID, SITE_ID, SITE_NAME, AGENCY, TIME_FOUND, LAST_READING, LAST_READING_TIME, LAST_READING_VAR, SUPPORTED_VARS },
					STATE + " = ?", new String[] {state.getAbbrev()}, null, null, SITE_NAME + " COLLATE NOCASE");
			
			result = new ArrayList<SiteData>(c.getCount());
			
			if(c.getCount() == 0) {
				c.close();
				db.close();
				return result;
			}
			c.moveToFirst();
			
			do {
				//if there's a stale record in the set, return null
				if(staleDate != null && c.getLong(4) <= staleDate.getTime()) {
					c.close();
					db.close();
					return null;
				}
				
				Site site = new Site();
				SiteId siteId = new SiteId(c.getString(3), c.getString(1));
				siteId.setPrimaryKey(c.getInt(0));
				site.setSiteId(siteId);
				
				site.setName(c.getString(2));
				
				site.setSupportedVariables(DataSourceController.getVariablesFromString(c.getString(3), c.getString(8)));
				
				if(site.getSupportedVariables().length == 0) {
					if(Log.isLoggable(TAG, Log.INFO)) {
						Log.i(TAG, site.getSiteId() + " has no variables that are supported by this version of RiverFlows.  Ignoring...");
					}
					continue;
				}
				
				SiteData data = new SiteData();
				data.setSite(site);
				
				if(!c.isNull(5)) {
					Series s = new Series();
					s.setVariable(DataSourceController.getVariable(site.getAgency(), c.getString(7)));
					Reading lastReading = new Reading();
					lastReading.setDate(new Date(c.getLong(6)));
					lastReading.setValue(c.getDouble(5));
					s.setReadings(Collections.singletonList(lastReading));
					
					data.getDatasets().put(s.getVariable().getCommonVariable(), s);
				}
				
				result.add(data);
			} while(c.moveToNext());

			c.close();
		} finally {	
			db.close();
		}
		
		return result;
	}
	
	public static boolean hasStaleReading(SiteData siteData) {

		//30 minutes ago
		Date staleReadingDate = new Date(System.currentTimeMillis() - (30 * 60 * 1000));

		//check whether any of the readings are expired
		try {
			Series firstDataset = siteData.getDatasets().values().iterator().next();
			if(firstDataset.getReadings().get(0).getDate().before(staleReadingDate)) {
				return true;
			}
		} catch(NoSuchElementException nsee) {
			if(Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "site has no datasets: " + siteData.getSite().getSiteId());
			}
		}
		return false;
	}
	
	public static List<SiteData> getSites(Context ctx, List<SiteId> siteIds) {
		if(siteIds.size() == 0) {
			return new ArrayList<SiteData>();
		}
		
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
		
		List<SiteData> result = null;
		
		try {
			StringBuilder inClause = new StringBuilder("(");
			String[] inClauseParams = new String[siteIds.size()];
			
			for(int a = 0; a < siteIds.size(); a++) {
				inClause.append("?,");
				inClauseParams[a] = siteIds.get(a).toString();
			}
			//chop off trailing comma
			inClause.setLength(inClause.length() - 1);
			inClause.append(")");
			
			
			Cursor c = db.query(NAME, new String[] { SITE_ID, SITE_NAME, AGENCY, TIME_FOUND, LAST_READING, LAST_READING_TIME, LAST_READING_VAR, SUPPORTED_VARS },
					"(" + AGENCY + " || '/' || " + SITE_ID + ") in " + inClause, inClauseParams, null, null, SITE_NAME);
			
			result = new ArrayList<SiteData>(c.getCount());
			
			if(c.getCount() == 0) {
				c.close();
				db.close();
				return result;
			}
			c.moveToFirst();
			
			do {
				
				Site site = new Site();
				site.setSiteId(new SiteId(c.getString(2), c.getString(0)));
				site.setName(c.getString(1));
				
				String supportedVarsStr = c.getString(7);
				
				site.setSupportedVariables(DataSourceController.getVariablesFromString(site.getAgency(), supportedVarsStr));
				
				if(site.getSupportedVariables().length == 0) {
					if(Log.isLoggable(TAG, Log.INFO)) {
						Log.i(TAG, site.getSiteId() + " has no variables that are supported by this version of RiverFlows.  Ignoring...");
					}
					continue;
				}
				
				SiteData data = new SiteData();
				data.setSite(site);
				
				if(!c.isNull(4)) {
					Series s = new Series();
					s.setVariable(DataSourceController.getVariable(site.getAgency(), c.getString(6)));
					Reading lastReading = new Reading();
					lastReading.setDate(new Date(c.getLong(5)));
					lastReading.setValue(c.getDouble(4));
					s.setReadings(Collections.singletonList(lastReading));
					
					data.getDatasets().put(s.getVariable().getCommonVariable(), s);
				}
				
				result.add(data);
			} while(c.moveToNext());
			c.close();
		} finally {
			db.close();
		}
		
		return result;
	}
	
	public static void saveSites(Context ctx, USState state, List<SiteData> sites) {

		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		
		try {
			//first, delete all sites for this state, if there are any
			db.delete(SitesDaoImpl.NAME, SitesDaoImpl.STATE + " = ?", 
					new String[]{ state.getAbbrev() });
			
			hSaveSites(db, sites);
		} finally {
			db.close();
		}
	}
	
	public static void saveSites(Context ctx, List<SiteData> sites) {

		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		
		try {
			//first, delete all sites, if there are any
			db.delete(SitesDaoImpl.NAME, "1", new String[]{ });
			
			hSaveSites(db, sites);
		} finally {
			db.close();
		}
	}
	
	private static void hSaveSites(SQLiteDatabase writeableDb, List<SiteData> sites) {
		
		long readingTimestamp = System.currentTimeMillis();
		
		//insert the refreshed sites
		for(SiteData currentData: sites) {
			Site currentSite = currentData.getSite();
			
			ContentValues favoriteValues = new ContentValues(6);
			favoriteValues.put(SITE_ID, currentSite.getId());
			favoriteValues.put(AGENCY,currentSite.getAgency());
			favoriteValues.put(SITE_NAME,currentSite.getName());
			favoriteValues.put(TIME_FOUND, System.currentTimeMillis());
			favoriteValues.put(STATE, currentData.getSite().getState().getAbbrev());
			favoriteValues.put(LATITUDE, currentSite.getLatitude());
			favoriteValues.put(LONGITUDE, currentSite.getLongitude());
			
			StringBuilder supportedVars = new StringBuilder();
			for(Variable v: currentSite.getSupportedVariables()) {
				supportedVars.append(v.getId() + " ");
			}
			
			favoriteValues.put(SUPPORTED_VARS, supportedVars.toString().trim());
			
			Series preferredSeries = DataSourceController.getPreferredSeries(currentData);
			
			if(preferredSeries != null && preferredSeries.getReadings().size() > 0) {
				
				Reading lastReading = preferredSeries.getReadings().get(preferredSeries.getReadings().size() - 1);
				
				favoriteValues.put(LAST_READING, lastReading.getValue());
				
				//use time reading stored rather than the time reading taken in order to prevent
				// all the sites in a state from being refreshed excessively due to one of them
				// having a long interval between readings
				favoriteValues.put(LAST_READING_TIME, readingTimestamp);
				favoriteValues.put(LAST_READING_VAR, preferredSeries.getVariable().getId());
			}
			
			writeableDb.insert(SitesDaoImpl.NAME,null, favoriteValues);
		}
	}
}