package com.cmuchimps.myauth;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

public class LocationChangedReceiver extends BroadcastReceiver {
	public static final String TAG = "LCReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		//Log.d(TAG, "LocationChangedReceiver has triggered");
		Bundle b = intent.getExtras();
		Location l = (Location)b.get(LocationManager.KEY_LOCATION_CHANGED);
		
		Cursor c = context.getContentResolver().query(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI,
				MyAuthProvider.SUBSCRIPTIONS_PROJECTION,
				"subskey='" + KnowledgeTranslatorWrapper.LocationKnowledgeSubscription.db_key + "'", 
				null, null);
		
		long last_updated = 0l;
		if (c!=null) {
			if (c.getCount() > 0) {
				c.moveToFirst();
				last_updated = c.getLong(c.getColumnIndex("last_update"));
			}
			c.close();
		}
		
		if (l != null) {
			if (l.getTime() > last_updated && l.getAccuracy() < 2000.0f) {
				ContentValues cv = new ContentValues();
				String timestamp,dayOfWeek;
				Date date = new Date(l.getTime());
				timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
				dayOfWeek = (String) DateFormat.format("EEEE", date);
				cv.put("timestamp", timestamp);
				cv.put("dayOfWeek", dayOfWeek);
				cv.put("persistence", "dynamic");
				Uri factsUri = context.getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
				//add person:User tag
				cv = new ContentValues();
				cv.put("tag_class", "Person");
				cv.put("subclass", "User");
				cv.put("idtype", "factsid");
				cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
				context.getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
				//add location:Provider tag
				cv = new ContentValues();
				cv.put("tag_class","Location");
				cv.put("subclass","Provider");
				cv.put("subvalue", l.getProvider());
				cv.put("idtype", "factsid");
				cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
				context.getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
				//add location:Geopoint tag
				cv = new ContentValues();
				cv.put("tag_class","Location");
				cv.put("subclass","Geopoint");
				cv.put("subvalue", l.getLatitude() + "," + l.getLongitude());
				cv.put("idtype", "factsid");
				cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
				context.getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
				//add location:Accuracy tag
				cv = new ContentValues();
				cv.put("tag_class","Location");
				cv.put("subclass","Accuracy");
				cv.put("subvalue", l.getAccuracy());
				cv.put("idtype", "factsid");
				cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
				context.getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
				
				//update last_updated time
				cv = new ContentValues();
				cv.put("last_update", l.getTime());
				cv.put("subskey", KnowledgeTranslatorWrapper.LocationKnowledgeSubscription.db_key);
				context.getContentResolver().update(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI, cv, null, null);
			} else {
				//Toast.makeText(getApplicationContext(), "Last location time: " + l.getTime(), Toast.LENGTH_LONG).show();
				//Log.d("KnowledgeTranslatorWrapper", "Last location time: " + DateFormat.format("yyyy-MM-dd kk:mm:ss",l.getTime()));
				//Log.d("KnowledgeTranslatorWrapper", "Last known location accuracy: " + l.getAccuracy());
			}
		}
	}

}
