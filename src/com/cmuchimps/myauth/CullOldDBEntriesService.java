package com.cmuchimps.myauth;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

public class CullOldDBEntriesService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Log.d("myAuth", "Getting to service.");
		KBDbAdapter db = new KBDbAdapter(getApplicationContext());
		(new CullingOldFactsTask()).execute(db);
		return Service.START_FLAG_REDELIVERY;
	}
	
	public class CullingOldFactsTask extends AsyncTask<KBDbAdapter, Void, Void> {
		public final static int CULL_THRESHOLD_IN_DAYS = 3;
		@Override
		protected Void doInBackground(KBDbAdapter... params) {
			try {
				for (KBDbAdapter dbHelper : params) {
					//Log.d("myAuth", "Culling stuff");
					dbHelper.open().cullOldFacts("*", CULL_THRESHOLD_IN_DAYS);
					dbHelper.close();
				}
			} catch (Exception e) {
				Log.d("myAuth", "Error occurred while culling database.");
				for (StackTraceElement sle : e.getStackTrace()) 
					Log.d("myAuth", sle.toString());
			}
			return null;
		}

	}
}
