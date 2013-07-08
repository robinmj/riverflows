package com.riverflows.db;

import java.io.File;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.riverflows.Home;
import com.riverflows.data.CachedDataset;

public class DatasetsDaoImpl {
	private static final String TAG = Home.TAG;
	
	static final String NAME = "datasets";
	
	static final String ID = "id";
	
	static final String FILE = "file";
	
	static final String TIMESTAMP = "timestamp";
	
	static final String URL = "url";

    static final String CREATE_SQL = "CREATE TABLE " + NAME
		+ " ( " + ID + " INTEGER PRIMARY KEY,"
		+ FILE + " TEXT,"
		+ TIMESTAMP + " REAL,"
		+ URL + " TEXT );";
    
    public static int saveDataset(Context ctx, String url, String cacheFileName) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			ContentValues datasetRow = new ContentValues(3);
			datasetRow.put(FILE, cacheFileName);
			datasetRow.put(TIMESTAMP, new Date().getTime());
			datasetRow.put(URL, url);
			
	    	//save dataset row
			return (int)db.insert(NAME, null, datasetRow);
		}
    }
    
    public static void deleteExpiredDatasets(Context ctx, File dataDir) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			//2 days
			Date expirationDate = new Date(System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000));
			
			db.beginTransaction();
			
			try {
				//get expired datasets
				Cursor c = db.query(NAME, new String[]{ ID, FILE }, TIMESTAMP + " < ?",
						new String[]{"" + expirationDate.getTime()},null,null,null);
				
				if(c.getCount() > 0) {
					//delete readings for each
					while(c.moveToNext()) {
						int datasetId = c.getInt(0);
						Log.i(TAG, "deleting dataset " + datasetId);
						File datasetFile = new File(dataDir, c.getString(1));
						Log.i(TAG, "deleting dataset file: " + datasetFile);
						if(!datasetFile.delete()) {
							Log.e(TAG, "failed to delete dataset file: " + datasetFile);
						}
					}
				}
				c.close();
				
				//delete expired datasets
				db.delete(NAME, TIMESTAMP + " < ?", new String[]{"" + expirationDate.getTime()});
			
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}
    }
    
    public static CachedDataset getDataset(Context ctx, String url) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
    	
    	//get dataset row
		Cursor c = db.rawQuery("SELECT "
				+ NAME + "." + URL + ", "
				+ FILE + ", "
				+ TIMESTAMP + " FROM " + NAME + " WHERE "
				+ URL + " = ? ORDER BY " + TIMESTAMP + " DESC",
				new String[]{"" + url});
		
		if(c.getCount() > 0) {
			if(c.getCount() > 1) {
				Log.e(TAG,"more than one cached dataset for " + url);
			}
			
			c.moveToFirst();
			
			String fileName = c.getString(1);
			
			long timestamp = c.getLong(2);
			c.close();
			return new CachedDataset(url, fileName, new Date(timestamp));
		}
		c.close();
    	return null;
    }
    

    /**
     * Delete cache entrie(s) for a given URL
     * @param ctx
     * @param url
     * @return
     */
    public static CachedDataset deleteDatasets(Context ctx, String url) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		
		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
    	
			db.delete(NAME, URL + " = ?", new String[]{"" + url});
		}
    	return null;
    }
    
    public static void updateDatasetTimestamp(Context ctx, String url, String fileName) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);

		synchronized(RiverGaugesDb.class) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			ContentValues datasetRow = new ContentValues(1);
			datasetRow.put(TIMESTAMP, new Date().getTime());
			
			db.update(NAME, datasetRow, URL + " = ? AND " + FILE + " = ?", new String[]{ url, fileName });
		}
    }
}
