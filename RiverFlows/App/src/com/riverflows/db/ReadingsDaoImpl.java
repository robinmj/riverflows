package com.riverflows.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.riverflows.data.Reading;

public class ReadingsDaoImpl {
	static final String NAME = "readings";
	
	static final String ID = "id";
	
	static final String DATASET_ID = "datasetId";
	
	static final String TIME = "time";
	
	static final String VALUE = "value";
	
	static final String QUALIFIERS = "qualifiers";

    static final String CREATE_SQL = "CREATE TABLE " + NAME
		+ " ( " + ID + " INTEGER PRIMARY KEY,"
		+ DATASET_ID + " INTEGER,"
		+ TIME + " REAL,"
		+ VALUE + " REAL,"
		+ QUALIFIERS + " TEXT);";
    
    public static List<Reading> getReadings(Context ctx, long datasetId) {

		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
    	
    	try {
    		Cursor c = db.query(NAME, new String[]{ DATASET_ID, TIME, VALUE, QUALIFIERS }, DATASET_ID + " = ?", new String[]{ datasetId + ""}, null, null, TIME);
    		
    		List<Reading> readings = new ArrayList<Reading>(c.getCount());
    		
    		 while(c.moveToNext()) {
    			Reading r = new Reading();
    			r.setDate(new Date(c.getLong(1)));
    			r.setValue(c.getDouble(2));
    			r.setQualifiers(c.getString(3));
    			readings.add(r);
    		}
    		
    		c.close();
    		return readings;
    	} catch(Throwable t) {
			throw new RuntimeException(t);
    	} finally {
    		db.close();
    	}
    }
    
    public static void deleteDatasetReadings(SQLiteDatabase db, int datasetId) {
    	//delete existing dataset
		db.delete(NAME, DATASET_ID + " = ?", new String[]{ datasetId + "" });
    }
    
    public static void saveReadings(Context ctx, int datasetId, List<Reading> readings) {
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
    	
    	try {
    		for(Reading r: readings) {
        		ContentValues readingRow = new ContentValues(3);
        		readingRow.put(DATASET_ID, datasetId);
    			readingRow.put(TIME, r.getDate().getTime());
    			readingRow.put(VALUE, r.getValue());
    			readingRow.put(QUALIFIERS, r.getQualifiers());
        		
        		db.insert(NAME, null, readingRow);
    		}
    	} catch(Throwable t) {
			throw new RuntimeException(t);
    	} finally {
    		db.close();
    	}
    }
}
