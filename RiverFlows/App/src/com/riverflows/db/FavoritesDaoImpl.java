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
import com.riverflows.data.Destination;
import com.riverflows.data.DestinationFacet;
import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.UserAccount;
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
     * User-specified name for this favorite
     */
    static final String FAVORITE_NAME = "name";

	/**
	 * Postal abbreviation (string) for the US state in which this gauge resides.
	 */
	static final String STATE = "state";

	/**
	 * space-separated list of agency-specific string identifiers for the variables supported by this site
	 */
	static final String SUPPORTED_VARS = "supportedVariables";

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

	/**
	 * corresponds to remote destination field
	 */
	static final String DEST_NAME = "dest_name";

	/**
	 * corresponds to remote destination field
	 */
	static final String DESCRIPTION = "description";

	/**
	 * corresponds to remote destination field
	 */
	static final String DEST_USER_ID = "dest_user_id";

	/**
	 * corresponds to remote destination field
	 */
	static final String VISUAL_GAUGE_LATITUDE = "visual_gauge_latitude";

	/**
	 * corresponds to remote destination field
	 */
	static final String VISUAL_GAUGE_LONGITUDE = "visual_gauge_longitude";

	/**
	 * corresponds to remote destination.created_at field
	 */
	static final String DEST_CREATED_AT = "dest_created_at";

	/**
	 * corresponds to remote destination.updated_at field
	 */
	static final String DEST_UPDATED_AT = "dest_updated_at";

	/**
	 * corresponds to remote destination_facet.description field
	 */
	static final String FACET_DESCRIPTION = "facet_description";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String DESTINATION_FACET_ID = "destination_facet_id";

	/**
	 * corresponds to remote destination_facet.user_id field
	 */
	static final String FACET_USER_ID = "facet_user_id";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String TOO_LOW = "too_low";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String LOW = "low";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String MED = "med";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String HIGH = "high";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String HIGH_PLUS = "high_plus";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String LOW_DIFFICULTY = "low_difficulty";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String MED_DIFFICULTY = "med_difficulty";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String HIGH_DIFFICULTY = "high_difficulty";

	/**
	 * corresponds to remote destination_facet.created_at field
	 */
	static final String FACET_CREATED_AT = "facet_created_at";

	/**
	 * corresponds to remote destination_facet.updated_at field
	 */
	static final String FACET_UPDATED_AT = "facet_updated_at";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String FACET_TYPE = "facet_type";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String LOW_PORT_DIFFICULTY = "low_port_difficulty";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String MED_PORT_DIFFICULTY = "med_port_difficulty";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String HIGH_PORT_DIFFICULTY = "high_port_difficulty";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String QUALITY_LOW = "quality_low";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String QUALITY_MED = "quality_med";

	/**
	 * corresponds to remote destination_facet field
	 */
	static final String QUALITY_HIGH = "quality_high";


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
            + FAVORITE_NAME + " TEXT,"
            + STATE + " TEXT,"
            + SUPPORTED_VARS + " TEXT,"
			+ ORDER + " INTEGER,"
			+ DEST_NAME + " INTEGER,"
			+ DESCRIPTION + " INTEGER,"
			+ DEST_USER_ID + " INTEGER,"
			+ VISUAL_GAUGE_LATITUDE + " REAL,"
			+ VISUAL_GAUGE_LONGITUDE + " REAL,"
			+ DEST_CREATED_AT + " INTEGER,"
			+ DEST_UPDATED_AT + " INTEGER,"
			+ FACET_DESCRIPTION + " TEXT,"
			+ DESTINATION_FACET_ID + " INTEGER,"
			+ FACET_USER_ID + " INTEGER,"
			+ TOO_LOW + " REAL,"
			+ LOW + " REAL,"
			+ MED + " REAL,"
			+ HIGH + " REAL,"
			+ HIGH_PLUS + " REAL,"
			+ LOW_DIFFICULTY + " INTEGER,"
			+ MED_DIFFICULTY + " INTEGER,"
			+ HIGH_DIFFICULTY + " INTEGER,"
			+ FACET_CREATED_AT + " INTEGER,"
			+ FACET_UPDATED_AT + " INTEGER,"
			+ FACET_TYPE + " INTEGER,"
			+ LOW_PORT_DIFFICULTY + " INTEGER,"
			+ MED_PORT_DIFFICULTY + " INTEGER,"
			+ HIGH_PORT_DIFFICULTY + " INTEGER,"
			+ QUALITY_LOW + " INTEGER,"
			+ QUALITY_MED + " INTEGER,"
			+ QUALITY_HIGH + " INTEGER );";

	public static List<Favorite> getFavorites(Context ctx, SiteId siteId, String variableId) {

		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getReadableDatabase();

			String sql = "SELECT " + getAllColumnsStr() + " FROM " + NAME;

			String whereClause = "";
			List<String> whereClauseParams = new ArrayList<String>(3);
			if(siteId != null) {
				whereClause += NAME + "." + AGENCY + " = ? AND " + NAME + "." + SITE_ID + " = ? ";
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
				result.add(extractFavorite(c));
			} while(c.moveToNext());

			c.close();
			return result;
		}
	}

	public static List<Favorite> getLocalFavorites(Context ctx) {

		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getReadableDatabase();

			String sql = "SELECT " + getAllColumnsStr() + " FROM " + NAME;

			String whereClause =  " WHERE " + NAME + "." + DESTINATION_FACET_ID + " IS NULL";

			String orderClause = " ORDER BY " + ORDER;

			Cursor c = db.rawQuery(sql + whereClause + orderClause, null);

			List<Favorite> result = new ArrayList<Favorite>(c.getCount());

			if(c.getCount() == 0) {
				c.close();
				db.close();
				return result;
			}
			c.moveToFirst();

			do {
				result.add(extractFavorite(c));
			} while(c.moveToNext());

			c.close();
			return result;
		}
	}

	public static Favorite getFavorite(Context ctx, int primaryKey) {

		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();

		String sql = "SELECT " + getAllColumnsStr() + " FROM " + NAME + " WHERE " + NAME + "." + ID + " = ?";

		Cursor c = db.rawQuery(sql, new String[]{ primaryKey + "" });

		if(c.getCount() == 0) {
			c.close();
			db.close();
			return null;
		}
		c.moveToFirst();

		if(Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "favorite: " + c.getString(1));

		Favorite favorite = extractFavorite(c);

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

	private static final String[] allColumns = new String[]{ID, SITE_ID, AGENCY, VARIABLE, ORDER,
			SITE_NAME, CREATED_DATE, FAVORITE_NAME, SUPPORTED_VARS, STATE, SUPPORTED_VARS, DEST_NAME, DESCRIPTION,
			DEST_USER_ID , VISUAL_GAUGE_LATITUDE , VISUAL_GAUGE_LONGITUDE , DEST_CREATED_AT , DEST_UPDATED_AT ,
			FACET_DESCRIPTION, DESTINATION_FACET_ID, FACET_USER_ID, TOO_LOW, LOW, MED, HIGH, HIGH_PLUS,
			LOW_DIFFICULTY, MED_DIFFICULTY, HIGH_DIFFICULTY, FACET_CREATED_AT, FACET_UPDATED_AT, FACET_TYPE,
			LOW_PORT_DIFFICULTY, MED_PORT_DIFFICULTY, HIGH_PORT_DIFFICULTY, QUALITY_LOW, QUALITY_MED, QUALITY_HIGH };

	private static String getAllColumnsStr() {
		return NAME + "." + TextUtils.join("," + NAME + ".", allColumns);
	}

	private static int getColumnIndex(String colName) {
		for(int a = 0; a < allColumns.length; a++) {
			if(allColumns[a].equals(colName)) {
				return a;
			}
		}
		throw new IllegalArgumentException("unknown column: " + colName);
	}

	/**
	 * @param c must be obtained from a query whose selection starts with {#getAllColumnsStr}
	 */
	private static Favorite extractFavorite(Cursor c) {

		Site newStation = new Site();
		newStation.setSiteId(new SiteId(c.getString(2), c.getString(1)));
		if(Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "favorite: " + c.getString(1));
		newStation.setSupportedVariables(DataSourceController.getVariablesFromString(c.getString(2), c.getString(8)));

		String favoriteVarId = c.getString(3);

		newStation.setName(c.getString(5));
		newStation.setState(USState.valueOf(c.getString(9)));

		Favorite newFavorite = new Favorite(newStation, favoriteVarId);

		newFavorite.setName(c.getString(7));
		newFavorite.setCreationDate(new Date(c.getLong(6)));
		newFavorite.setId(c.getInt(0));
		newFavorite.setOrder(c.getInt(4));

		DestinationFacet facet = new DestinationFacet();

		Destination dest = new Destination();

		dest.setName(c.getString(getColumnIndex(DEST_NAME)));
		dest.setCreationDate(new Date(c.getLong(getColumnIndex(DEST_CREATED_AT))));
		dest.setDescription(c.getString(getColumnIndex(DESCRIPTION)));
		dest.setSite(newStation);

		dest.setModificationDate(new Date(c.getLong(getColumnIndex(DEST_UPDATED_AT))));

		UserAccount destUser = new UserAccount();
		destUser.setPlaceholderObj(true);
		destUser.setId(c.getInt(getColumnIndex(DEST_USER_ID)));
		dest.setUser(destUser);

		facet.setDestination(dest);
		facet.setDescription(c.getString(getColumnIndex(FACET_DESCRIPTION)));
		facet.setId(c.getInt(getColumnIndex(DESTINATION_FACET_ID)));
		facet.setCreationDate(new Date(c.getLong(getColumnIndex(FACET_CREATED_AT))));
		facet.setModificationDate(new Date(c.getLong(getColumnIndex(FACET_UPDATED_AT))));

		facet.setFacetType(c.getInt(getColumnIndex(FACET_TYPE)));
		facet.setHighPlus(c.getDouble(getColumnIndex(HIGH_PLUS)));
		facet.setHigh(c.getDouble(getColumnIndex(HIGH)));
		facet.setMed(c.getDouble(getColumnIndex(MED)));
		facet.setLow(c.getDouble(getColumnIndex(LOW)));
		facet.setTooLow(c.getDouble(getColumnIndex(TOO_LOW)));
		facet.setHighDifficulty(c.getInt(getColumnIndex(HIGH_DIFFICULTY)));
		facet.setHighPortDifficulty(c.getInt(getColumnIndex(HIGH_PORT_DIFFICULTY)));
		facet.setMedDifficulty(c.getInt(getColumnIndex(MED_DIFFICULTY)));
		facet.setMedPortDifficulty(c.getInt(getColumnIndex(MED_PORT_DIFFICULTY)));
		facet.setLowDifficulty(c.getInt(getColumnIndex(LOW_DIFFICULTY)));
		facet.setLowPortDifficulty(c.getInt(getColumnIndex(LOW_PORT_DIFFICULTY)));
		facet.setQualityHigh(c.getInt(getColumnIndex(QUALITY_HIGH)));
		facet.setQualityMed(c.getInt(getColumnIndex(QUALITY_MED)));
		facet.setQualityLow(c.getInt(getColumnIndex(QUALITY_LOW)));
		facet.setVariable(DataSourceController.getVariable(newStation.getAgency(), favoriteVarId));

		newFavorite.setDestinationFacet(facet);

		return newFavorite;
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

		ContentValues favoriteValues = buildContentValues(favorite);

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

		ContentValues favoriteValues = buildContentValues(favorite);

		DestinationFacet facet = favorite.getDestinationFacet();

		if(facet != null) {

			RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
			synchronized(RiverGaugesDb.class) {
				SQLiteDatabase db = helper.getWritableDatabase();
				db.update(FavoritesDaoImpl.NAME, favoriteValues, DESTINATION_FACET_ID + " = ?", new String[]{facet.getId().toString()});
			}
			return;
		}

		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.update(FavoritesDaoImpl.NAME, favoriteValues, ID + " = ?", new String[]{favorite.getId().toString()});
		}
	}



	public static ContentValues buildContentValues(Favorite favorite) {

		Site favoriteSite = favorite.getSite();

		ContentValues favoriteValues = new ContentValues();
		favoriteValues.put(SITE_ID, favoriteSite.getId());
		favoriteValues.put(SITE_NAME,favoriteSite.getName());
		favoriteValues.put(AGENCY,favoriteSite.getAgency());
		favoriteValues.put(LAST_VIEWED, System.currentTimeMillis());
		favoriteValues.put(LATITUDE, favoriteSite.getLatitude());
		favoriteValues.put(LONGITUDE, favoriteSite.getLongitude());
		favoriteValues.put(VARIABLE, favorite.getVariable());
		favoriteValues.put(FAVORITE_NAME, favorite.getName());
		favoriteValues.put(STATE, favoriteSite.getState().getAbbrev());
		favoriteValues.put(SUPPORTED_VARS, DataSourceController.variablesToString(favoriteSite.getSupportedVariables()));
		favoriteValues.put(ORDER, favorite.getOrder());

		DestinationFacet facet = favorite.getDestinationFacet();

		if(facet == null) {
			return favoriteValues;
		}

		favoriteValues.put(DESTINATION_FACET_ID, facet.getId());

		Destination dest = facet.getDestination();

		favoriteValues.put(DEST_NAME, dest.getName());
		favoriteValues.put(DESCRIPTION, dest.getDescription());

		if(dest.getUser() != null) {
			favoriteValues.put(DEST_USER_ID, dest.getUser().getId());
		}
		//favoriteValues.put(VISUAL_GAUGE_LATITUDE,
		//favoriteValues.put(VISUAL_GAUGE_LONGITUDE,
		favoriteValues.put(DEST_UPDATED_AT, System.currentTimeMillis());
		favoriteValues.put(FACET_DESCRIPTION,facet.getDescription());
		favoriteValues.put(FACET_USER_ID,facet.getUser().getId());
		favoriteValues.put(FACET_TYPE, facet.getFacetType());
		favoriteValues.put(HIGH_PLUS,facet.getHighPlus());
		favoriteValues.put(HIGH,facet.getHigh());
		favoriteValues.put(HIGH_DIFFICULTY,facet.getHighDifficulty());
		favoriteValues.put(HIGH_PORT_DIFFICULTY,facet.getHighPortDifficulty());
		favoriteValues.put(MED,facet.getMed());
		favoriteValues.put(MED_DIFFICULTY, facet.getMedDifficulty());
		favoriteValues.put(MED_PORT_DIFFICULTY,facet.getMedPortDifficulty());
		favoriteValues.put(LOW,facet.getLow());
		favoriteValues.put(LOW_DIFFICULTY,facet.getLowDifficulty());
		favoriteValues.put(LOW_PORT_DIFFICULTY,facet.getLowPortDifficulty());
		favoriteValues.put(TOO_LOW,facet.getTooLow());
		favoriteValues.put(QUALITY_HIGH,facet.getQualityHigh());
		favoriteValues.put(QUALITY_MED,facet.getQualityMed());
		favoriteValues.put(QUALITY_LOW,facet.getQualityLow());
		favoriteValues.put(FACET_UPDATED_AT, System.currentTimeMillis());

		return favoriteValues;
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