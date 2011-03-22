package com.riverflows.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.riverflows.wsclient.UsgsCsvDataSource;


class RiverGaugesDb extends SQLiteOpenHelper {
	
	public static final String DB_NAME = "RiverFlows";
	public static final int DB_VERSION = 4;
	
	private Integer upgradedFromVersion = null;

	public RiverGaugesDb(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(FavoritesDaoImpl.CREATE_SQL);
		db.execSQL(SitesDaoImpl.CREATE_SQL);
		db.execSQL(DatasetsDaoImpl.CREATE_SQL);
		db.execSQL(ReadingsDaoImpl.CREATE_SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int fromVersion, int toVersion) {
		upgradedFromVersion = fromVersion;
		
		if(fromVersion < 2) {
			String addNewCol = "ALTER TABLE " + SitesDaoImpl.NAME + " ADD COLUMN " + SitesDaoImpl.AGENCY + " TEXT;";
			db.execSQL(addNewCol);

			//up to this point, USGS was the only agency
			ContentValues agencyValue = new ContentValues();
			agencyValue.put(SitesDaoImpl.AGENCY, "USGS");
			db.update(SitesDaoImpl.NAME, agencyValue, null, null);
			
			addNewCol = "ALTER TABLE " + SitesDaoImpl.NAME + " ADD COLUMN " + SitesDaoImpl.LAST_READING + " REAL;";
			db.execSQL(addNewCol);
			
			addNewCol = "ALTER TABLE " + SitesDaoImpl.NAME + " ADD COLUMN " + SitesDaoImpl.LAST_READING_TIME + " REAL;";
			db.execSQL(addNewCol);
			
			addNewCol = "ALTER TABLE " + SitesDaoImpl.NAME + " ADD COLUMN " + SitesDaoImpl.LAST_READING_VAR + " TEXT;";
			db.execSQL(addNewCol);
		}
		if(fromVersion < 3) {

			String addNewCol = "ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.AGENCY + " TEXT;";
			db.execSQL(addNewCol);
			
			addNewCol = "ALTER TABLE " + FavoritesDaoImpl.NAME + " ADD COLUMN " + FavoritesDaoImpl.VARIABLE + " TEXT;";
			db.execSQL(addNewCol);
			
			//up to this point, USGS was the only agency
			ContentValues agencyValue = new ContentValues();
			agencyValue.put(FavoritesDaoImpl.AGENCY, "USGS");
			
			db.update(FavoritesDaoImpl.NAME, agencyValue, null, null);
			
			
			String deleteSitesWhereClause = null;
			
			Cursor favoriteSiteIds = db.query(FavoritesDaoImpl.NAME, new String[]{FavoritesDaoImpl.SITE_ID}, null, null, null, null, null);
			
			if(favoriteSiteIds.getCount() > 0) {
				StringBuilder clause = new StringBuilder(SitesDaoImpl.SITE_ID + " NOT IN ('");
				favoriteSiteIds.moveToFirst();
				do {
					clause.append(favoriteSiteIds.getString(0));
					clause.append("','");
				} while(favoriteSiteIds.moveToNext());
				
				int len = clause.length();
				clause.delete(len - 2, len);
				clause.append(")");
				deleteSitesWhereClause = clause.toString();
			}
			
			favoriteSiteIds.close();
			
			//delete everything in the sites table that is not referred to by a favorite rather than attempt to initialize supportedVariables
			db.delete(SitesDaoImpl.NAME, deleteSitesWhereClause, null);
			
			addNewCol = "ALTER TABLE " + SitesDaoImpl.NAME + " ADD COLUMN " + SitesDaoImpl.SUPPORTED_VARS + " TEXT;";
			
			db.execSQL(addNewCol);
			
			//favorites will stop showing up if the sites they are associated with don't have supported variables
			String setFavoriteVars = "UPDATE " + SitesDaoImpl.NAME + " SET " + SitesDaoImpl.SUPPORTED_VARS + " = '"
				+ UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS.getId() + " " + UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT.getId() + "', " + SitesDaoImpl.TIME_FOUND + " = 0";
			db.execSQL(setFavoriteVars);
		}
		if(fromVersion < 4) {
			db.execSQL(DatasetsDaoImpl.CREATE_SQL);
			db.execSQL(ReadingsDaoImpl.CREATE_SQL);
		}
	}

	public Integer getUpgradedFromVersion() {
		return upgradedFromVersion;
	}

}
