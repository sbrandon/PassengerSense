package com.passengersense;

import org.json.JSONException;
import org.json.JSONObject;
import org.most.MoSTService;
import org.most.pipeline.Pipeline;
import org.most.serverpost.PostJson;

import com.passengersense.location.LocationService;
import com.passengersense.wifiping.ScanWifiNetwork;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	Button startButton;
	Button stopButton;
	Button groundTruthButton;
	ScanWifiNetwork scan;
	EditText userRoute;
	TextView instructions;
	ConnectivityManager connManager;
	NetworkInfo mWifi;
	private LocationService locationService;
	private Context context;
	private String route = "";
	private int occupancy = 1;
	private final String TAG = "MainActivity";
	private static final String htmlInstructions = "&#8226; Once on board the bus connect to 'Dublin Bus_Wi-Fi'.<br/>&#8226; Count the passengers on board the bus, upstairs and downstairs.<br/>&#8226; Click start and enter the bus route you are on and how many passengers you counted in the last step.<br/>&#8226; Where possible update the passenger count as you see it change.";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();
		connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		instructions = (TextView) findViewById(R.id.instructions);
		instructions.setText(Html.fromHtml(htmlInstructions));
		startButton = (Button) findViewById(R.id.start);
		stopButton = (Button) findViewById(R.id.stop);
		groundTruthButton = (Button) findViewById(R.id.ground_truth_button);
		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				makeToast("Sensing Started");
				startButton.setEnabled(false);
				sendStartSignal();
				showGroundTruthDialog();
				//Start MoST Sensing Library
				Intent i = new Intent(MainActivity.this, MoSTService.class);
				i.setAction(MoSTService.START);
				i.putExtra(MoSTService.KEY_PIPELINE_TYPE, Pipeline.Type.BLUETOOTH.toInt());
				startService(i);
				i.putExtra(MoSTService.KEY_PIPELINE_TYPE, Pipeline.Type.CELL.toInt());
				startService(i);
				//Start WiFi Scanning
				if (mWifi.isConnected()) {
					scan = new ScanWifiNetwork(context);
					scan.execute();
				}
				//Start Location Service
				locationService = new LocationService(MainActivity.this, context);
				locationService.getLocationUpdates();
			}
		});
		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				stopScan();
			}
		});
		groundTruthButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showGroundTruthDialog();
			}
		});
	}
	
	//Stop the MoST service.
	public void stopScan(){
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Stop the Scan?");
		builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
		    	//Stop MoST
		    	Intent i = new Intent(MainActivity.this, MoSTService.class);
				i.setAction(MoSTService.STOP);
				i.putExtra(MoSTService.KEY_PIPELINE_TYPE, Pipeline.Type.BLUETOOTH.toInt());
				startService(i);
				i.putExtra(MoSTService.KEY_PIPELINE_TYPE, Pipeline.Type.CELL.toInt());
				startService(i);
				//Stop WiFi Ping
		    	if(scan != null){
					scan.setRunning(false);
			    	scan.cancel(true);
		    	}
		    	//Stop Location Service
		    	if(locationService != null){
		    		locationService.stopLocationUpdates();
		    	}
		        dialog.dismiss();
		        startButton.setEnabled(true);
		        sendStopSignal();
		        //Reset Default Values For Next Scan
		        route = "";
		        occupancy = 1;
		        makeToast("Sensing Stopped");
		    }
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
		         dialog.dismiss();
		    }
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	public void sendStartSignal(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("Start-Signal", 0);
			PostJson postJson = PostJson.getInstance(context);
			postJson.postJsonMeasurement(obj, "START_SIGNAL", null);
		}catch(JSONException e){
			Log.e(TAG, e.toString());
		}
	}
	
	public void sendStopSignal(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("Stop-Signal", 0);
			PostJson postJson = PostJson.getInstance(context);
			postJson.postJsonMeasurement(obj, "STOP_SIGNAL", null);
		}catch(JSONException e){
			Log.e(TAG, e.toString());
		}
	}
	
	//Store ground truth in phones database
	public void storeGroundTruth(int groundTruth, String busRoute){
		/*
		long unixTime = System.currentTimeMillis() / 1000L;
		scanDetails = new ContentValues();
		scanDetails.put("timestamp", unixTime);
		scanDetails.put("groundTruth", groundTruth);
		scanDetails.put("busroute", busRoute);
		DBManager.storeData(context, "scan", scanDetails);
		*/
		postGroundTruth(groundTruth, busRoute);
	}
	
	public void postGroundTruth(int groundTruth, String busRoute){
		try{
			JSONObject obj = new JSONObject();
			obj.put("groundTruth", groundTruth);
			PostJson postJson = PostJson.getInstance(context);
			postJson.postJsonMeasurement(obj, "GROUND_TRUTH", busRoute);
		}catch(JSONException e){
			Log.e(TAG, e.toString());
		}
	}
	
	//Show the dialog where user inputs ground truth and bus occupancy.
	@SuppressLint("InflateParams")
	public void showGroundTruthDialog(){
		LayoutInflater inflater = LayoutInflater.from(context);
		final View groundTruthDialog = inflater.inflate(R.layout.ground_truth_dialog, null);
		Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
		alertDialogBuilder.setView(groundTruthDialog);
		final EditText groundTruth = (EditText) groundTruthDialog.findViewById(R.id.ground_truth_input);
		final EditText busRoute = (EditText) groundTruthDialog.findViewById(R.id.bus_route);
		busRoute.setImeOptions(EditorInfo.IME_ACTION_DONE);
		final Button busFull = (Button) groundTruthDialog.findViewById(R.id.bus_full);
		final Button plus = (Button) groundTruthDialog.findViewById(R.id.plus);
		final Button minus = (Button) groundTruthDialog.findViewById(R.id.minus);
		final Button submit = (Button) groundTruthDialog.findViewById(R.id.submit_ground_truth);
		if(!route.equals("")){
			busRoute.setText(route);
			final TextView label = (TextView) groundTruthDialog.findViewById(R.id.bus_route_label);
			label.setVisibility(View.INVISIBLE);
			busRoute.setVisibility(View.INVISIBLE);
		}
		groundTruth.setText(Integer.toString(occupancy));
		alertDialogBuilder.setCancelable(false);
		final AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
		busFull.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				groundTruth.setText(Integer.toString(76));
			}
		});
		plus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int value = Integer.parseInt(groundTruth.getText().toString());
				value ++;
				groundTruth.setText(Integer.toString(value));
			}
		});
		minus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int value = Integer.parseInt(groundTruth.getText().toString());
				value --;
				groundTruth.setText(Integer.toString(value));
			}
		});
		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(busRoute.getVisibility() == View.INVISIBLE){
					storeGroundTruth(Integer.parseInt(groundTruth.getText().toString()), route);
					occupancy = Integer.parseInt(groundTruth.getText().toString());
					alertDialog.dismiss();
				}
				else{
					if(busRoute.getText().toString().equals("")){
						busRoute.setError("Please enter Bus Route!");
					}
					else{
						route = busRoute.getText().toString();
						occupancy = Integer.parseInt(groundTruth.getText().toString());
						storeGroundTruth(Integer.parseInt(groundTruth.getText().toString()), busRoute.getText().toString());
						alertDialog.dismiss();
					}
				}
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId())
		{
		case R.id.help:
			Intent helpIntent = new Intent(this, HelpActivity.class);
			startActivity(helpIntent);
            return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	//Make a toast.
	public void makeToast(String toast){
		Toast.makeText(MainActivity.this, toast, Toast.LENGTH_LONG).show();
	}
	
}
