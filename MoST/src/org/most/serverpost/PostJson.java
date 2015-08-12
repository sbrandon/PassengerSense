package org.most.serverpost;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;
import org.most.persistence.DBAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PostJson {
	
	private final static String TAG = PostJson.class.getSimpleName();
	
	private static PostJson _postJson;
	
	private Context context;
	private JSONObject jsonPost;
	private String uriPost;
	private String measurement_uri;
	private String location_uri;
	
	public static synchronized PostJson getInstance(Context context){
		if(_postJson == null){
			_postJson = new PostJson(context);
			Log.i(TAG, "Successfully created new instance of PostJson");
		}
		return _postJson;
	}
	
	private PostJson(Context context){
		this.context = context;
		SharedPreferences preferences = context.getSharedPreferences("com.passengersense", Context.MODE_PRIVATE);
		String token = preferences.getString("apiToken", "000");
		measurement_uri = "http://134.226.36.48:8080/sitewhere/api/assignments/" + token + "/measurements?updateState=true";
		location_uri = "http://134.226.36.48:8080/sitewhere/api/assignments/" + token + "/locations?updateState=true";
	}
	
	public synchronized void postJsonMeasurement(JSONObject measurements, String pipelineName, String meta){
		try{
			JSONObject json = new JSONObject();
			JSONObject metadata = new JSONObject();
			metadata.put("PipelineName", pipelineName);
			metadata.put("imei", getDeviceImei(context));
			if(pipelineName.equals("GROUND_TRUTH")){
				metadata.put("busRoute", meta);
			}
			json.put("measurements", measurements);
			json.put("eventDate", getTimeStamp());
			json.put("metadata", metadata);
			jsonPost = json;
			uriPost = measurement_uri;
			logJson(json);
			asyncPost();
			Log.e(TAG, json.toString());
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		}
	}
	
	public synchronized void postJsonLocation(JSONObject measurements, String pipelineName, String meta){
		try{
			JSONObject metadata = new JSONObject();
			metadata.put("imei", getDeviceImei(context));
			metadata.put("PipelineName", pipelineName);
			measurements.put("eventDate", getTimeStamp());
			measurements.put("elevation", 0);
			measurements.put("metadata", metadata);
			jsonPost = measurements;
			uriPost = location_uri;
			logJson(measurements);
			asyncPost();
			Log.e(TAG, measurements.toString());
		}catch(JSONException e){
			Log.e(TAG, e.toString());
		}
	}
	
	private void asyncPost(){
		Runnable task = new Runnable() {
			public void run() {
				post(jsonPost, uriPost);
			}
		};
		new Thread(task, "MoST DB flush").start();
	}
	
	private void post(JSONObject json, String uri){
		try{
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpost = new HttpPost(uri);
			httpost.addHeader(new BasicHeader("Content-Type", "application/json"));
	    	StringEntity se = new StringEntity(json.toString());
	    	httpost.setEntity(se);
	    	ResponseHandler response = new BasicResponseHandler();
	    	String result = httpclient.execute(httpost, response);
	    	Log.e(TAG, result);
		}catch(IOException e){
			Log.e(TAG, e.toString());
		}
	}
	
	private void logJson(JSONObject json){
		ContentValues cv = new ContentValues();
		cv.put("json", json.toString());
		DBAdapter adapter = DBAdapter.getInstance(context);
		adapter.open();
		adapter.storeData("JSON_LOG", cv, true);
		adapter.close();
	}
	
	private String getTimeStamp(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'+0000'");
		String timeStamp = sdf.format(new Date());
		return timeStamp;
	}
	
	//Return the devices IMEI number.
	private String getDeviceImei(Context context){
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();
		return imei;
	}
}
