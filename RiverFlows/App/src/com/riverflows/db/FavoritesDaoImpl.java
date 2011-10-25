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

import com.riverflows.Home;
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
	
	private static final String TAG = Home.TAG;
	
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

    /**
     * The sort order
     * <P>Type: INTEGER (0 comes first)</P>
     */
    static final String ORDER = "`order`";

	
    static final String CREATE_SQL = "CREATE TABLE " + NAME
		+ " ( " + ID + " INTEGER PRIMARY KEY,"
		+ SITE_ID + " TEXT,"
		+ SITE_NAME + " TEXT,"
		+ AGENCY + " TEXT,"
		+ VARIABLE + " TEXT,"
		+ CREATED_DATE + " INTEGER,"
		+ LAST_VIEWED + " INTEGER,"
		+ LATITUDE + " REAL,"
		+ LONGITUDE + " REAL,"
		+ ORDER + " INTEGER );";
    

	public static List<Favorite> getFavorites(Context ctx, SiteId siteId, String variableId) {
		
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
	
		String sql = "SELECT " + NAME + "." + TextUtils.join("," + NAME + ".", new String[]{ID, SITE_ID, AGENCY,  VARIABLE, ORDER, SITE_NAME, CREATED_DATE }) + ","
		 + SitesDaoImpl.NAME + "."
		 + TextUtils.join(", " + SitesDaoImpl.NAME + ".", new String[]{ SitesDaoImpl.ID, SitesDaoImpl.SUPPORTED_VARS, SitesDaoImpl.STATE, SitesDaoImpl.SITE_NAME })
		 + " FROM " + NAME + " JOIN " + SitesDaoImpl.NAME
		 + " ON (" + NAME + "." + SITE_ID + " = " + SitesDaoImpl.NAME + "." + SitesDaoImpl.SITE_ID + ")";
		
		String whereClause = "";
		List<String> whereClauseParams = new ArrayList<String>(3);
		if(siteId != null) {
			whereClause += SitesDaoImpl.NAME + "." + SitesDaoImpl.AGENCY + " = ? AND " + SitesDaoImpl.NAME + "." + SitesDaoImpl.SITE_ID + " = ? ";
			whereClauseParams.add(siteId.getAgency());
			whereClauseParams.add(siteId.getId());
		}
		
		if(variableId != null) {
			if(whereClause.length() > 0) {
				whereClause += " AND ";
			}
			whereClause += NAME + "." + VARIABLE + " = ? ";
			whereClauseParams.add(variableId);
		}
		
		if(whereClause.length() > 0) {
			whereClause = " WHERE " + whereClause;
		}
		
		String orderClause = " ORDER BY " + ORDER;
		
		String[] whereClauseParamsA = new String[whereClauseParams.size()];
		whereClauseParams.toArray(whereClauseParamsA);
		
		Cursor c = db.rawQuery(sql + whereClause + orderClause, whereClauseParamsA);
		
		List<Favorite> result = new ArrayList<Favorite>(c.getCount());
		
		if(c.getCount() == 0) {
			c.close();
			db.close();
			return result;
		}
		c.moveToFirst();
		
		do {
			Site newStation = new Site();
			newStation.setSiteId(new SiteId(c.getString(2), c.getString(1), c.getInt(7)));
			if(Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "favorite: " + c.getString(1));
			newStation.setSupportedVariables(DataSourceController.getVariablesFromString(c.getString(2), c.getString(8)));
			
			String favoriteVarId = c.getString(3);

			newStation.setName(c.getString(10));
			newStation.setState(USState.valueOf(c.getString(9)));
			
			Favorite newFavorite = new Favorite(newStation, favoriteVarId);

			newFavorite.setName(c.getString(5));
			newFavorite.setCreationDate(new Date(c.getLong(6)));
			newFavorite.setId(c.getInt(0));
			newFavorite.setOrder(c.getInt(4));
			result.add(newFavorite);
		} while(c.moveToNext());
		
		c.close();
		return result;
	}
	
	public static Favorite getFavorite(Context ctx, int primaryKey) {

		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
	
		String sql = "SELECT " + NAME + "." + TextUtils.join("," + NAME + ".", new String[]{ID, SITE_ID, AGENCY,  VARIABLE, ORDER, SITE_NAME, CREATED_DATE }) + ","
		 + SitesDaoImpl.NAME + "."
		 + TextUtils.join(", " + SitesDaoImpl.NAME + ".", new String[]{ SitesDaoImpl.ID, SitesDaoImpl.SUPPORTED_VARS, SitesDaoImpl.STATE, SitesDaoImpl.SITE_NAME })
		 + " FROM " + NAME + " JOIN " + SitesDaoImpl.NAME
		 + " ON (" + NAME + "." + SITE_ID + " = " + SitesDaoImpl.NAME + "." + SitesDaoImpl.SITE_ID + ") WHERE " + NAME + "." + ID + " = ?";
		
		Cursor c = db.rawQuery(sql, new String[]{ primaryKey + "" });
		
		if(c.getCount() == 0) {
			c.close();
			db.close();
			return null;
		}
		c.moveToFirst();
		
		Site newStation = new Site();
		newStation.setSiteId(new SiteId(c.getString(2), c.getString(1), c.getInt(7)));
		if(Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "favorite: " + c.getString(1));
		newStation.setSupportedVariables(DataSourceController.getVariablesFromString(c.getString(2), c.getString(8)));
		
		String favoriteVarId = c.getString(3);

		newStation.setName(c.getString(10));
		newStation.setState(USState.valueOf(c.getString(9)));
		
		Favorite favorite = new Favorite(newStation, favoriteVarId);
		favorite.setName(c.getString(5));
		favorite.setCreationDate(new Date(c.getLong(6)));
		favorite.setId(c.getInt(0));
		favorite.setOrder(c.getInt(4));
		
		c.close();
		return favorite;
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
		favoriteValues.put(SITE_NAME,favorite.getName());
		favoriteValues.put(AGENCY,favoriteSite.getAgency());
		favoriteValues.put(CREATED_DATE, System.currentTimeMillis());
		favoriteValues.put(LAST_VIEWED, System.currentTimeMillis());
		favoriteValues.put(LATITUDE, favoriteSite.getLatitude());
		favoriteValues.put(LONGITUDE, favoriteSite.getLongitude());
		favoriteValues.put(VARIABLE, favorite.getVariable());
		favoriteValues.put(ORDER, favorite.getVariable());

		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			db.insert(FavoritesDaoImpl.NAME,null, favoriteValues);
		}
	}
	
	public static void updateFavorite(Context ctx, Favorite favorite) {
		if(favorite.getId() == null) {
			throw new NullPointerException();
		}
		
		Site favoriteSite = favorite.getSite();
		
		ContentValues favoriteValues = new ContentValues(1);
		favoriteValues.put(SITE_ID, favoriteSite.getId());
		favoriteValues.put(SITE_NAME,favorite.getName());
		favoriteValues.put(AGENCY,favoriteSite.getAgency());
		favoriteValues.put(LAST_VIEWED, System.currentTimeMillis());
		favoriteValues.put(LATITUDE, favoriteSite.getLatitude());
		favoriteValues.put(LONGITUDE, favoriteSite.getLongitude());
		favoriteValues.put(VARIABLE, favorite.getVariable());
		favoriteValues.put(ORDER, favorite.getOrder());

		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.update(FavoritesDaoImpl.NAME, favoriteValues,ID + " = ?", new String[]{ favorite.getId().toString() });
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