package com.riverflows.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UsgsCsvDataSource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class RiverGaugesDb extends SQLiteOpenHelper {
	
	public static final String DB_NAME = "RiverFlows";
	public static final int DB_VERSION = 7;
	
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
