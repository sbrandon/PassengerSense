package com.passengersense;

import org.json.JSONException;
import org.json.JSONObject;
import org.most.serverpost.PostJson;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class HelpActivity extends Activity{
	
	Context context;
	SharedPreferences preferences;
	Button callButton;
	Button textButton;
	EditText apiToken;
	Button confirmToken;
	Button testToken;
	private String token;
	private final String TAG = "HelpActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		context = getApplicationContext();
		callButton = (Button) findViewById(R.id.call_stephen_button);
		textButton = (Button) findViewById(R.id.text_stephen_button);
		apiToken = (EditText) findViewById(R.id.api_token);
		confirmToken = (Button) findViewById(R.id.confirm_token);
		testToken = (Button) findViewById(R.id.test_token_button);
		preferences = getSharedPreferences("com.passengersense", MODE_PRIVATE);
		token = preferences.getString("apiToken", "");
		if(!token.equals("")){
			apiToken.setText(token);
		}
		callButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				callStephen();
			}
		});
		textButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				textStephen();
			}
		});
		confirmToken.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				confirmToken();
			}
		});
		testToken.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				testToken();
			}
		});
	}
	
	public void callStephen(){
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:00353876275737"));
		startActivity(callIntent);
	}
	
	public void textStephen(){
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("sms:00353876275737")));
	}
	
	public void confirmToken(){
		token = apiToken.getText().toString();
		Editor editor = preferences.edit();
		editor.putString("apiToken", token);
		editor.commit();
		makeToast("New API Token Saved");
	}
	
	public void testToken(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("API-TEST", 000);
			PostJson postJson = PostJson.getInstance(context);
			postJson.postJsonMeasurement(obj, "API-TEST", null);
			makeToast("Test Sent - Check Server");
		}catch(JSONException e){
			Log.e(TAG, e.toString());
		}
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
			//Do Nothing
		}
		return super.onOptionsItemSelected(item);
	}
	
	//Make a toast.
	public void makeToast(String toast){
		Toast.makeText(HelpActivity.this, toast, Toast.LENGTH_LONG).show();
	}
	
}
