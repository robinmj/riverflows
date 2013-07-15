package com.riverflows.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class RiverGaugesDb extends SQLiteOpenHelper {

	public static final String DB_NAME = "RiverFlows";
	public static final int DB_VERSION = 8;

	private Integer upgradedFromVersion = null;

	private static RiverGaugesDb helper;

	private RiverGaugesDb(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	public static RiverGaugesDb getHelper(Context context) {
		if(helper == null) {
			helper = new RiverGaugesDb(context);
		}
		return helper;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(FavoritesDaoImpl.CREATE_SQL);
		db.execSQL(DatasetsDaoImpl.CREATE_SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int fromVersion, int toVersion) {
		upgradedFromVersion = fromVersion;

		if(fromVersion < 3) {

			String addNewCol = "ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.AGENCY + " TEXT;";
			db.execSQL(addNewCol);

			addNewCol = "ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.VARIABLE + " TEXT;";
			db.execSQL(addNewCol);

			//up to this point, USGS was the only agency
			ContentValues agencyValue = new ContentValues();
			agencyValue.put(FavoritesDaoImpl.AGENCY, "USGS");

			db.update(FavoritesDaoImpl.NAME, agencyValue, null, null);
		}
		if(fromVersion < 4) {
			db.execSQL(DatasetsDaoImpl.CREATE_SQL);
			//db.execSQL(ReadingsDaoImpl.CREATE_SQL);
		}
		if(fromVersion < 5) {
			db.execSQL("DROP TABLE IF EXISTS " + DatasetsDaoImpl.NAME + ";");
			db.execSQL(DatasetsDaoImpl.CREATE_SQL);
			db.execSQL("DROP TABLE IF EXISTS readings;");
		}
		if(fromVersion < 6) {
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.ORDER + " INTEGER;");
		}
		if(fromVersion < 7) {
			db.execSQL("ALTER TABLE favorites ADD COLUMN name TEXT;");
			db.execSQL("ALTER TABLE favorites ADD COLUMN state TEXT;");
			db.execSQL("ALTER TABLE favorites ADD COLUMN supportedVariables TEXT;");

			if(fromVersion > 1) {
				String sql = "SELECT favorites.id, favorites.siteName, sites.siteName, sites.state, sites.supportedVariables" +
						" FROM favorites JOIN sites ON (favorites.siteId = sites.siteId" +
						" AND favorites.agency = sites.agency)";

				Cursor c = db.rawQuery(sql, null);

				ContentValues[] valuesToUpdate = new ContentValues[c.getCount()];
				int[] favoriteIds = new int[c.getCount()];

				if(c.getCount() == 0) {
					c.close();
				} else {
					c.moveToFirst();

					do {

						favoriteIds[c.getPosition()] = c.getInt(0);

						valuesToUpdate[c.getPosition()] = new ContentValues();
						valuesToUpdate[c.getPosition()].put("siteName", c.getString(2));
						valuesToUpdate[c.getPosition()].put("name", c.getString(1));
						valuesToUpdate[c.getPosition()].put("state", c.getString(3));
						valuesToUpdate[c.getPosition()].put("supportedVariables", c.getString(4));
					} while(c.moveToNext());

					c.close();
				}

				for(int a = 0; a < favoriteIds.length; a++) {
					db.update("favorites", valuesToUpdate[a], "id = ?", new String[] { favoriteIds[a] + ""} );
				}
			}

			db.execSQL("DROP TABLE IF EXISTS sites");
		}
		if(fromVersion < 8) {
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.DEST_NAME + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.DESCRIPTION + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.DEST_USER_ID + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.VISUAL_GAUGE_LATITUDE + " REAL;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.VISUAL_GAUGE_LONGITUDE + " REAL;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.DEST_CREATED_AT + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.DEST_UPDATED_AT + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.FACET_DESCRIPTION + " TEXT;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.DESTINATION_FACET_ID + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.FACET_USER_ID + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.TOO_LOW + " REAL;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.LOW + " REAL;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.MED + " REAL;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.HIGH + " REAL;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.HIGH_PLUS + " REAL;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.LOW_DIFFICULTY + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.MED_DIFFICULTY + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.HIGH_DIFFICULTY + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.FACET_CREATED_AT + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.FACET_UPDATED_AT + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.FACET_TYPE + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.LOW_PORT_DIFFICULTY + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.MED_PORT_DIFFICULTY + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.HIGH_PORT_DIFFICULTY + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.QUALITY_LOW + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.QUALITY_MED + " INTEGER;");
			db.execSQL("ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.QUALITY_HIGH + " INTEGER;");
		}
	}

	public Integer getUpgradedFromVersion() {
		return upgradedFromVersion;
	}
	
	public static void closeHelper() {
		if(helper == null) {
			return;
		}
		helper.close();
		helper = null;
	}
}
