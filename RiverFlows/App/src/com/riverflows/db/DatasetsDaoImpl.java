package com.riverflows.db;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.riverflows.data.Series;
import com.riverflows.wsclient.DataSourceController;

public class DatasetsDaoImpl {
	private static final String TAG = DatasetsDaoImpl.class.getSimpleName();
	
	static final String NAME = "datasets";
	
	static final String ID = "id";
	
	static final String SITE_ID = "siteId";
	
	static final String VARIABLE = "variable";
	
	static final String TIMESTAMP = "timestamp";

    static final String CREATE_SQL = "CREATE TABLE " + NAME
		+ " ( " + ID + " INTEGER PRIMARY KEY,"
		+ SITE_ID + " INTEGER,"
		+ VARIABLE + " TEXT,"
		+ TIMESTAMP + " REAL);";
    
    /**
     * Save a series of readings for a specific site, replacing any existing series associated with
     * the same variable.
     * @param ctx
     * @param siteId
     * @param s
     */
    public static void saveDataset(Context ctx, int siteId, Series s) {
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		
		try {
			
			Cursor c = db.query(NAME, new String[]{ID}, SITE_ID + " = ? AND " + VARIABLE + " = ?",
					new String[]{ siteId + "", s.getVariable().getId()},
					null, null, null);
			
			Boolean oneRow = null;
			
			while(c.moveToNext()) {
				oneRow = (oneRow == null) ? true : false;
				
				int datasetId = c.getInt(0);
				
		    	//delete existing dataset
				db.delete(NAME,ID + " = ?", new String[]{ datasetId + "" });
				
				ReadingsDaoImpl.deleteDatasetReadings(db,datasetId);
			}
			c.close();
			
			if(oneRow != null && !oneRow) {
				Log.w(TAG,"multiple datasets for " + siteId + " " + s.getVariable().getId());
			}
	    	
			ContentValues datasetRow = new ContentValues(3);
			datasetRow.put(SITE_ID, siteId);
			datasetRow.put(VARIABLE, s.getVariable().getId());
			datasetRow.put(TIMESTAMP, new Date().getTime());
			
	    	//save dataset row
			int insertId = (int)db.insert(NAME, null, datasetRow);
			
	    	//save readings
			ReadingsDaoImpl.saveReadings(ctx, insertId, s.getReadings());
		} catch(Throwable t) {
			throw new RuntimeException(t);
		} finally {
			db.close();
		}
    }
    
    public static void deleteExpiredDatasets(Context ctx) {
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		
		try {
		} catch(Throwable t) {
			throw new RuntimeException(t);
		} finally {
			db.close();
		}
    }
    
    public static Series getDataset(Context ctx, int siteId, String variable) {
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
    	
		try {
	    	//get dataset row
			Cursor c = db.rawQuery("SELECT " + NAME + "." + ID + ", " + VARIABLE + ", " + TIMESTAMP
					+ ", " + SitesDaoImpl.AGENCY + " FROM " + NAME + " JOIN " + SitesDaoImpl.NAME + " ON ("
					+ NAME + "." + SITE_ID + " = " + SitesDaoImpl.NAME + "." + SitesDaoImpl.ID + ") WHERE "
					+ NAME + "." + SITE_ID + " = ? AND " + VARIABLE + " = ?",
					new String[]{"" + siteId, variable});
			
			if(c.getCount() > 0) {
				if(c.getCount() > 1) {
					Log.e(TAG,"more than one cached dataset for " + siteId + " " + variable);
				}
				
				c.moveToFirst();
				
				Series result = new Series();
				
				String variableId = c.getString(1);
				
				String agency = c.getString(3);
				
				result.setVariable(DataSourceController.getVariable(agency, variableId));
				
				int datasetId = c.getInt(0);
				
		    	//get readings
				result.setReadings(ReadingsDaoImpl.getReadings(ctx, datasetId));
				c.close();
				return result;
			}
		} catch(Throwable t) {
			throw new RuntimeException(t);
		} finally {
			db.close();
		}
    	return null;
    }
}
