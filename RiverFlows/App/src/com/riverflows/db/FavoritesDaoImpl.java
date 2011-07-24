package com.riverflows.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.DataSourceController;

/**
 * Favorites Table
 * @author robin
 *
 */
public class FavoritesDaoImpl {
	
	private static final String TAG = FavoritesDaoImpl.class.getSimpleName();
	
	static final String NAME = "favorites";
	
	static final String ID = "id";

	/**
	 * Name of the site (String)
	 */
	static final String SITE_NAME = "siteName";
	
	/**
	 * unique string identifying the measuring site
	 */
	static final String SITE_ID = "siteId";

	/**
	 * string specifying the agency that maintains this site.
	 */
	static final String AGENCY = "agency";
	
	/**
	 * agency-specific ID for the variable that is to be displayed first
	 */
	static final String VARIABLE = "variable";
	
	/**
	 * float identifying the longitude coordinate of the station
	 */
	static final String LONGITUDE = "longitude";
	
	/**
	 * float identifying the longitude coordinate of the station
	 */
	static final String LATITUDE = "latitude";

    /**
     * The timestamp for when the favorite site was last viewed
     * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
     */
    static final String LAST_VIEWED = "lastViewed";

    /**
     * The timestamp for when the favorite site was created
     * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
     */
    static final String CREATED_DATE = "created";

	
    static final String CREATE_SQL = "CREATE TABLE " + NAME
		+ " ( " + ID + " INTEGER PRIMARY KEY,"
		+ SITE_ID + " TEXT,"
		+ SITE_NAME + " TEXT,"
		+ AGENCY + " TEXT,"
		+ VARIABLE + " TEXT,"
		+ CREATED_DATE + " INTEGER,"
		+ LAST_VIEWED + " INTEGER,"
		+ LATITUDE + " REAL,"
		+ LONGITUDE + " REAL );";
    

	public static List<Favorite> getFavorites(Context ctx) {
		
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
	
		String sql = "SELECT " + NAME + "." + TextUtils.join("," + NAME + ".", new String[]{SITE_ID, AGENCY,  VARIABLE}) + ","
		 + SitesDaoImpl.NAME + "."
		 + TextUtils.join(", " + SitesDaoImpl.NAME + ".", new String[]{ SitesDaoImpl.ID, SitesDaoImpl.SITE_NAME, SitesDaoImpl.SUPPORTED_VARS, SitesDaoImpl.STATE })
		 + " FROM " + NAME + " JOIN " + SitesDaoImpl.NAME
		 + " ON (" + NAME + "." + SITE_ID + " = " + SitesDaoImpl.NAME + "." + SitesDaoImpl.SITE_ID + ")";
		
		Cursor c = db.rawQuery(sql, null);
		
		List<Favorite> result = new ArrayList<Favorite>(c.getCount());
		
		if(c.getCount() == 0) {
			c.close();
			db.close();
			return result;
		}
		c.moveToFirst();
		
		do {
			Site newStation = new Site();
			newStation.setSiteId(new SiteId(c.getString(1), c.getString(0), c.getInt(3)));
			if(Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "favorite: " + c.getString(0));
			newStation.setSupportedVariables(DataSourceController.getVariablesFromString(c.getString(1), c.getString(5)));
			
			String variableId = c.getString(2);

			newStation.setName(c.getString(4));
			newStation.setState(USState.valueOf(c.getString(6)));
			
			Favorite newFavorite = new Favorite(newStation, variableId);
			result.add(newFavorite);
		} while(c.moveToNext());
		
		c.close();
		return result;
	}
	
	public static boolean hasFavorites(Context ctx) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
		
		Cursor c = db.query(NAME, new String[]{SITE_ID},
				null, null, null, null, null);
		
		boolean result = (c != null && c.getCount() > 0);
		c.close();
		return result;
	}
	
	public static boolean hasNewFavorites(Context ctx, Date lastLoaded) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
		
		Cursor c = db.query(NAME, new String[]{SITE_ID},
				CREATED_DATE + " > ?", new String[]{lastLoaded.getTime() + ""}, null, null, null);
		
		boolean result = (c != null && c.getCount() > 0);
		c.close();
		return result;
	}
	
	public static boolean isFavorite(Context ctx, SiteId siteId, Variable var) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
		
		String whereClause = SITE_ID + " = ? AND "
		+ AGENCY + " = ?";
		
		String[] parameters = null;
		
		if(var != null) {
			whereClause = whereClause + " AND " + VARIABLE + " = ?";
			parameters = new String[]{ siteId.getId(), siteId.getAgency(), var.getId() };
		} else {
			parameters = new String[]{ siteId.getId(), siteId.getAgency() };
		}
    	
    	Cursor c = db.query(NAME, new String[]{ SITE_ID }, whereClause, parameters, null, null, null );
    	
    	boolean result = !(c == null || c.getCount() == 0);

		c.close();
    	return result;
	}
	
	public static void updateLastViewedTime(Context ctx, SiteId siteId) {

    	//update LAST_VIEWED time
    	
    	ContentValues favorite = new ContentValues(1);
		favorite.put(LAST_VIEWED, System.currentTimeMillis());
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = RiverGaugesDb.getHelper(ctx).getWritableDatabase();
	    	
			db.update(FavoritesDaoImpl.NAME, favorite, SITE_ID + " = ? AND "
				+ AGENCY + " = ?", new String[]{ siteId.getId(), siteId.getAgency() });
		}
    }
	
	public static void createFavorite(Context ctx, Favorite favorite) {
		Site favoriteSite = favorite.getSite();
		
		ContentValues favoriteValues = new ContentValues(1);
		favoriteValues.put(SITE_ID, favoriteSite.getId());
		favoriteValues.put(SITE_NAME,favoriteSite.getName());
		favoriteValues.put(AGENCY,favoriteSite.getAgency());
		favoriteValues.put(CREATED_DATE, System.currentTimeMillis());
		favoriteValues.put(LAST_VIEWED, System.currentTimeMillis());
		favoriteValues.put(LATITUDE, favoriteSite.getLatitude());
		favoriteValues.put(LONGITUDE, favoriteSite.getLongitude());
		favoriteValues.put(VARIABLE, favorite.getVariable());

		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			db.insert(FavoritesDaoImpl.NAME,null, favoriteValues);
		}
	}
	
	public static void deleteFavorite(Context ctx, SiteId siteId, Variable var) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			if(var != null) {
				db.delete(FavoritesDaoImpl.NAME,  SITE_ID + " = ? AND "
					+ AGENCY + " = ? AND " + VARIABLE + " = ? ", 
					new String[]{ siteId.getId(), siteId.getAgency(), var.getId() });
			} else {
				db.delete(FavoritesDaoImpl.NAME,  SITE_ID + " = ? AND "
						+ AGENCY + " = ?", 
						new String[]{ siteId.getId(), siteId.getAgency() });
			}
		}
	}
}