package com.riverflows.db;

import android.content.Context;

public class DbMaintenance {
	
	/**
	 * @param ctx
	 * @return the version that the database was upgraded from, or null if no upgrade took place
	 */
	public static Integer upgradeIfNecessary(Context ctx) {
		RiverGaugesDb helper = RiverGaugesDb.getHelper(ctx);
		synchronized(RiverGaugesDb.class) {
			helper.getWritableDatabase();
		}
		
		return helper.getUpgradedFromVersion();
	}
}
