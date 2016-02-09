package com.mrfoh.paystackcordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.Callback;

public class Paystack extends CordovaPlugin {
	 
	 private String secret;
	 
	 private final OkHttpClient client = new OkHttpClient();
	 
	 private final String initializeUrl = "https://api.paystack.co/transaction/initialize";
	 
	 private final String verifyUrl = "https://api.paystack.co/transaction/verify";

	 public static final String ACTION_INIT = "initialize";

	 public static final String ACTION_VERIFY = "verify";

	
	 public Paystack() {
	 }
	 
	 @Override
     public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		try {
			this.secret = args.getString(0);
		    if (ACTION_INIT.equals(action)) { 
		    	this.initializeTransaction(args.getString(1), args.getString(2), args.getString(3), callbackContext);
		   	}

		   	else if (ACTION_VERIFY.equals(action)) {
		   		this.verifyTransaction(args.getString(1), callbackContext);
		   	}

		    callbackContext.error("Invalid action");
		    return false;
		} catch(Exception e) {

		    System.err.println("Exception: " + e.getMessage());
		    callbackContext.error(e.getMessage());
		    return false;
		} 
	 }
	 

	private void initializeTransaction(String reference, String email, String amount, CallbackContext callbackContext) throws IOException, JSONException{
		 
		MediaType json = MediaType.parse("application/json; charset=utf-8");
		String authorization = "Bearer "+this.secret;

		JSONObject requestBody = new JSONObject();
		requestBody.put("reference", reference);
		requestBody.put("email", email);
		requestBody.put("amount", amount);

		RequestBody body = RequestBody.create(json, requestBody.toString());
		Request request = new Request.Builder()
			.url(this.initializeUrl)
			.addHeader("Authorization", authorization)
			.addHeader("Content-Type", "application/json")
			.post(body)
			.build();

		Response response = this.client.newCall(request).execute();

		if(!response.isSuccessful()) {
			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT, 0));
			throw new IOException("Request Error "+ response);
		}

		if(response.code() == 200) {
			JSONObject serverResponse = new JSONObject(response.body().string());
			JSONObject data = serverResponse.getJSONObject("data");
			String authorizationUrl = data.getString("authorization_url");

			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, authorizationUrl));
		}
	}
	 
	private void verifyTransaction(String reference, CallbackContext callbackContext) throws JSONException {
		 
		MediaType json = MediaType.parse("application/json; charset=utf-8");
		String authorization = "Bearer "+this.secret;
		String requestUrl = this.verifyUrl+"/"+reference;

		Request request = new Request.Builder().
			url(requestUrl).
			addHeader("Authorization", authorization).
			addHeader("Content-Type", "application/json").
			build();

		this.client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Request request, IOException throwable) {
				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT, 0));
				throwable.printStackTrace();
			}

			@Override
			public void onResponse(Response response) throws IOException {
				if(!response.isSuccessful()) {
					callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT, 0));
					throw new IOException("Request Error "+ response);
				}

				if(response.code() == 200) {
					JSONObject serverResponse = new JSONObject(response.body().string());
					callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, serverResponse));
				}
			}
		});
	}
}