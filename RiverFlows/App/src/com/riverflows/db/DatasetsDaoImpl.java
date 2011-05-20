package com.riverflows.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.riverflows.data.CachedDataset;
import com.riverflows.data.Series;
import com.riverflows.data.SiteData;
import com.riverflows.wsclient.DataSourceController;

public class DatasetsDaoImpl {
	private static final String TAG = DatasetsDaoImpl.class.getSimpleName();
	
	static final String NAME = "datasets";
	
	static final String ID = "id";
	
	/**
	 * references SitesDaoImpl.ID
	 */
	static final String SITE_ID = "siteId";
	
	static final String VARIABLE = "variable";
	
	static final String TIMESTAMP = "timestamp";
	
	static final String DATA_INFO = "dataInfo";

    static final String CREATE_SQL = "CREATE TABLE " + NAME
		+ " ( " + ID + " INTEGER PRIMARY KEY,"
		+ SITE_ID + " INTEGER,"
		+ VARIABLE + " TEXT,"
		+ TIMESTAMP + " REAL,"
		+ DATA_INFO + " TEXT );";
    
    /**
     * Save a series of readings for a specific site, replacing any existing series associated with
     * the same variable.
     * @param ctx
     * @param siteId
     * @param s
     */
    public static void saveDataset(Context ctx, int siteId, Series s, String dataInfo) {
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
				
				long startTime = System.currentTimeMillis();
				
				ReadingsDaoImpl.deleteDatasetReadings(db,datasetId);
				
				Log.d(TAG, "deleted old dataset readings in " + (System.currentTimeMillis() - startTime) + "ms");
			}
			c.close();
			
			if(oneRow != null && !oneRow) {
				Log.w(TAG,"multiple datasets for " + siteId + " " + s.getVariable().getId());
			}
	    	
			ContentValues datasetRow = new ContentValues(3);
			datasetRow.put(SITE_ID, siteId);
			datasetRow.put(VARIABLE, s.getVariable().getId());
			datasetRow.put(TIMESTAMP, new Date().getTime());
			datasetRow.put(DATA_INFO, dataInfo);
			
	    	//save dataset row
			int insertId = (int)db.insert(NAME, null, datasetRow);
			

			long startTime = System.currentTimeMillis();
			
	    	//save readings
			ReadingsDaoImpl.saveReadings(ctx, insertId, s.getReadings());
			
			Log.d(TAG, "saved new dataset readings in " + (System.currentTimeMillis() - startTime) + "ms");
			
		} catch(Throwable t) {
			throw new RuntimeException(t);
		} finally {
			db.close();
		}
    }
    
    public static void saveDatasets(Context ctx, SiteData data) {

		//cache datasets for later reuse
		for(Series currentSeries: data.getDatasets().values()) {
			saveDataset(ctx, data.getSite().getSiteId().getPrimaryKey(), currentSeries, data.getDataInfo());
		}
    }
    
    public static void deleteExpiredDatasets(Context ctx) {
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		
		//2 days
		Date expirationDate = new Date(System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000));
		
		try {
			//get expired datasets
			Cursor c = db.query(NAME, new String[]{ ID }, TIMESTAMP + " < ?",
					new String[]{"" + expirationDate.getTime()},null,null,null);
			
			if(c.getCount() > 0) {
				//delete readings for each
				while(c.moveToNext()) {
					int datasetId = c.getInt(0);
					Log.i(TAG, "deleting dataset " + datasetId);
					ReadingsDaoImpl.deleteDatasetReadings(db, datasetId);
				}
			}
			c.close();
			
			//delete expired datasets
			db.delete(NAME, TIMESTAMP + " < ?", new String[]{"" + expirationDate.getTime()});
		} catch(Throwable t) {
			throw new RuntimeException(t);
		} finally {
			db.close();
		}
    }
    
    public static CachedDataset getDataset(Context ctx, int siteId, String variable) {
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
    	
		try {
	    	//get dataset row
			Cursor c = db.rawQuery("SELECT "
					+ NAME + "." + ID + ", "
					+ NAME + "." + DATA_INFO + ", "
					+ VARIABLE + ", "
					+ TIMESTAMP + ", "
					+ SitesDaoImpl.AGENCY
					+ " FROM " + NAME + " JOIN " + SitesDaoImpl.NAME + " ON ("
					+ NAME + "." + SITE_ID + " = " + SitesDaoImpl.NAME + "." + SitesDaoImpl.ID + ") WHERE "
					+ NAME + "." + SITE_ID + " = ? AND " + VARIABLE + " = ?",
					new String[]{"" + siteId, variable});
			
			if(c.getCount() > 0) {
				if(c.getCount() > 1) {
					Log.e(TAG,"more than one cached dataset for " + siteId + " " + variable);
				}
				
				c.moveToFirst();
				
				Series series = new Series();
				
				String dataInfo = c.getString(1);
				
				String variableId = c.getString(2);
				
				long timestamp = c.getLong(3);
				
				String agency = c.getString(4);
				
				series.setVariable(DataSourceController.getVariable(agency, variableId));
				
				int datasetId = c.getInt(0);
				
		    	//get readings
				series.setReadings(ReadingsDaoImpl.getReadings(ctx, datasetId));
				c.close();
				return new CachedDataset(dataInfo, series, new Date(timestamp));
			}
			c.close();
		} catch(Throwable t) {
			throw new RuntimeException(t);
		} finally {
			db.close();
		}
    	return null;
    }

    public static List<Series> getDatasets(Context ctx, int siteId) {

		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
    	
		try {
	    	//get dataset row
			Cursor c = db.rawQuery("SELECT " + NAME + "." + ID + ", " + VARIABLE + ", " + TIMESTAMP
					+ ", " + SitesDaoImpl.AGENCY + " FROM " + NAME + " JOIN " + SitesDaoImpl.NAME + " ON ("
					+ NAME + "." + SITE_ID + " = " + SitesDaoImpl.NAME + "." + SitesDaoImpl.ID + ") WHERE "
					+ NAME + "." + SITE_ID + " = ?",
					new String[]{"" + siteId});
			
			List<Series> result = new ArrayList<Series>();
			
			if(c.getCount() > 0) {
				
				while(c.moveToNext()) {
					
					Series currentSeries = new Series();
					
					String variableId = c.getString(1);
					
					String agency = c.getString(3);
					
					currentSeries.setVariable(DataSourceController.getVariable(agency, variableId));
					
					int datasetId = c.getInt(0);
					
			    	//get readings
					currentSeries.setReadings(ReadingsDaoImpl.getReadings(ctx, datasetId));
					c.close();
				}
			}
			return result;
		} catch(Throwable t) {
			throw new RuntimeException(t);
		} finally {
			db.close();
		}
    }
}
