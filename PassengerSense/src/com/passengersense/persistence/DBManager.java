package com.passengersense.persistence;

import org.most.persistence.DBAdapter;

import android.content.ContentValues;
import android.content.Context;

public class DBManager {
	
	public static void storeData(Context context, String table, ContentValues data){
		DBAdapter adapter = DBAdapter.getInstance(context);
		adapter.open();
		adapter.storeData(table, data, true);
		adapter.close();
	}
	
}
