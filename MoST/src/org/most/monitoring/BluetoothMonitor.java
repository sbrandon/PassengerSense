package org.most.monitoring;

import java.util.HashSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.most.serverpost.PostJson;

import android.content.Context;
import android.util.Log;

public class BluetoothMonitor {

	private final String TAG = "BluetoothMonitor";
	private HashSet<String> hosts = new HashSet<String>();
	private HashSet<String> previousHosts = new HashSet<String>();
	private int scanNo = 0;
	private Context context;
	
	public BluetoothMonitor(Context context){
		this.context = context;
	}
	
	public void addHost(String mac){
		hosts.add(mac);
	}
	
	public void reviewScan(){
		int newHosts = 0;
		int totalHosts = hosts.size();
		if(scanNo > 0){
			for(String string : hosts){
				if(!previousHosts.contains(string)){
					newHosts++;
				}
			}
		}
		if(newHosts > 0 || totalHosts < previousHosts.size() || scanNo == 0){
    		postResults(totalHosts, newHosts);
    	}
		previousHosts.clear();
		previousHosts = new HashSet<String>(hosts);
		hosts.clear();
    	scanNo++;
	}
	
	public void postResults(int totalHosts, int newHosts){
		JSONObject obj = new JSONObject();
    	try {
			obj.put("TotalHostsBluetooth", totalHosts);
			obj.put("NewHostsBluetooth", newHosts);
			PostJson postJson = PostJson.getInstance(context);
			postJson.postJsonMeasurement(obj, "BLUETOOTH", null);
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		}
	}
	
}