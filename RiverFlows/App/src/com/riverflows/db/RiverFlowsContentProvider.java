package com.riverflows.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.riverflows.data.USState;

public class RiverFlowsContentProvider extends ContentProvider {
	
	public static final Uri CONTENT_URI = 
        Uri.parse("content://com.riverflows");

	public static final String sitesPathPrefix = "/sites/";
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if(uri.getPath().startsWith(sitesPathPrefix)) {
			if(selection.equals("state")) {
				USState state = USState.getUSStateText(selectionArgs[1]);
				
				RiverGaugesDb helper = new RiverGaugesDb(getContext());
				SQLiteDatabase db = helper.getReadableDatabase();
//				return db.query(SitesDaoImpl.NAME, new String[] { ID, SITE_ID, SITE_NAME, AGENCY, TIME_FOUND, LAST_READING, LAST_READING_TIME, LAST_READING_VAR, SUPPORTED_VARS },
//						STATE + " = ?", new String[] {state.getAbbrev()}, null, null, SITE_NAME + " COLLATE NOCASE");
			}
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
