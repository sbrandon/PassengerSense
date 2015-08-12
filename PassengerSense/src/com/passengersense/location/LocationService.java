package com.passengersense.location;

import org.json.JSONException;
import org.json.JSONObject;
import org.most.serverpost.PostJson;

import com.passengersense.MainActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationService {

	private final String TAG = "LocationService";
	private Context context;
	private MainActivity parent;
	LocationManager locationManager;
	LocationListener locationListener;
	
	public LocationService(MainActivity parent, Context context){
		this.parent = parent;
		this.context = context;
	}
	
	public void getLocationUpdates(){
		locationManager = (LocationManager) parent.getSystemService(Context.LOCATION_SERVICE);
	
		locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		    	addToDB(location);
		    }
	
		    public void onProviderEnabled(String provider) {}
	
		    public void onProviderDisabled(String provider) {}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
				//TODO Something if GPS is unavailable
			}

		  };

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 5, locationListener);
	}
	
	public void addToDB(Location location){
		/*
		long unixTime = System.currentTimeMillis() / 1000L;
		ContentValues values = new ContentValues();
		values.put("timestamp", unixTime);
		values.put("longitude", location.getLongitude());
		values.put("latitude", location.getLatitude());
		values.put("accuracy", 0);
		values.put("provider", "GPS");
		DBManager.storeData(context, "LOCATION", values);
		*/
		postLocation(location);
	}
	
	public void postLocation(Location location){
		try{
			JSONObject obj = new JSONObject();
			obj.put("elevation", location.getAltitude());
			obj.put("longitude", location.getLongitude());
			obj.put("latitude", location.getLatitude());
			PostJson postJson = PostJson.getInstance(context);
			postJson.postJsonLocation(obj, "LOCATION", null);
		}catch(JSONException e){
			Log.e(TAG, e.toString());
		}
	}
	
	public void stopLocationUpdates(){
		locationManager.removeUpdates(locationListener);
	}
	
}
