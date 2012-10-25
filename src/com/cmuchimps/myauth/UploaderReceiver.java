package com.cmuchimps.myauth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UploaderReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		//Log.d("UploaderReceiver", "Attempting to upload packets to server");
		Intent uploader = new Intent(context,UploaderService.class);
		context.startService(uploader);
	}
}