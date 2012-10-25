package com.cmuchimps.myauth;

import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class UploaderService extends Service {
	private WakeLock mWakeLock;
	public static final String BROADCAST_ACTION = "com.cmuchimps.myauth.uploadupdate";
	private ServerCommunicator mSC;
	private Intent mIntent;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mIntent = new Intent(BROADCAST_ACTION);
	}
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Log.d("UploaderService","Beginning to start service to upload packets...");
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UploaderServiceWakeLock");
        mWakeLock.acquire();
        mSC = new ServerCommunicator(getApplicationContext());
        
        if (mSC.hasQueuedPackets() && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
        	(new UploaderTask()).execute(mSC);
        } else {
        	stopSelf();
        }

		return Service.START_FLAG_REDELIVERY;
	}
	
	private class UploaderTask extends AsyncTask<ServerCommunicator,Void,Void> {
		@Override
		protected Void doInBackground(ServerCommunicator... params) {
			try {
				//Log.d("UploaderTask","Entered uploader task...sending packets");
				params[0].sendQueuedPackets();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//Log.d("UploaderService", "Exception in background uploader task");
				e.printStackTrace();
			}
			return null;
		}
        
        @Override
        protected void onPostExecute(Void result) {
        	sendBroadcast(mIntent);
        	stopSelf();
        }
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mWakeLock.release();
	}
}
