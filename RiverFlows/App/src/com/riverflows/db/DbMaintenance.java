package com.riverflows.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DbMaintenance {
	
	/**
	 * @param ctx
	 * @return the version that the database was upgraded from, or null if no upgrade took place
	 */
	public static Integer upgradeIfNecessary(Context ctx) {
		RiverGaugesDb helper = new RiverGaugesDb(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		db.close();
		
		return helper.getUpgradedFromVersion();
	}
}
