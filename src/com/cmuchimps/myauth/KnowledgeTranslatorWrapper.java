package com.cmuchimps.myauth;

import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Browser;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.MediaStore;
import android.provider.UserDictionary;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

public class KnowledgeTranslatorWrapper extends Service {
	private WakeLock mWakeLock; //need a WakeLock so the system doesn't kill the service
	private KnowledgeTranslatorWrapper thisObj;
	
	public KnowledgeTranslatorWrapper() {
		thisObj = this;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KTWWakeLock");
        mWakeLock.acquire();
        
		if (intent.hasExtra("dueSubs")) {
			(new UpdaterTask()).execute(intent.getStringArrayExtra("dueSubs"));
		} else {
			Cursor c = getContentResolver().query(MyAuthProvider.SUBSCRIPTIONS_DUE_URI, null, null, null, null);
			if (c.getCount() > 0) {
				String[] dueSubs = new String[c.getCount()];
				int counter = 0;
				c.moveToFirst();
				while (!c.isAfterLast()) {
					dueSubs[counter++] = c.getString(c.getColumnIndex("subskey"));
					c.moveToNext();
				}
				(new UpdaterTask()).execute(dueSubs);
			} else {
				//Log.d("KnowledgeTranslatorWrapper", "No subscriptions due for an update at " + System.currentTimeMillis() + ".");
			}
			c.close();
		}
		
		return Service.START_FLAG_REDELIVERY;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
     * In onDestroy() we release our wake lock. This ensures that whenever the
     * Service stops (killed for resources, stopSelf() called, etc.), the wake
     * lock will be released.
     */
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
    }
    
	/**
	 * 
	 * @author sauvikd
	 *
	 */
	private class UpdaterTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			for (int i = 0; i < params.length; i++) {
				String s = params[i];
				try {
					//Log.d("KnowledgeTranslatorWrapper", "com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + s + "KnowledgeSubscription");
					Class c = Class.forName("com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + s + "KnowledgeSubscription");
					if (!(Modifier.isStatic(c.getModifiers()) || Modifier.isAbstract(c.getModifiers()))) {
						KnowledgeSubscription ks = (KnowledgeSubscription) c.getDeclaredConstructor(new Class[] { KnowledgeTranslatorWrapper.class }).newInstance(new Object[] { thisObj });
						c.getMethod("poll", null).invoke(ks, null);
						//Log.d("KnowledgeTranslatorWrapper", "Successfully polled " + s);
					}
				} catch (Throwable e) {
					//Log.d("KnowledgeTranslatorWrapper", "Failed to update class " + s + "KnowledgeSubscription because of " + e.getMessage());
					e.printStackTrace();
				}
			}
			return null;
		}
		
		@Override
        protected void onPostExecute(Void result) {
            stopSelf();
        }
	}
	
	
	public abstract class KnowledgeSubscription {
		public static final boolean IGNORE_DST = true;
		//public static final String db_key;
		
		public KnowledgeSubscription() {
			
		}
		
		protected void reset_update_time(String db_key) {
			//update last update time for this subscription
			/*if (db_key == null || db_key.equalsIgnoreCase("")) {
				Log.d("KnowledgeTranslatorWrapper", "Failed to reset because of empty db_key");
			} else {
				mDbHelper.updateSubscriptionTime(db_key, System.currentTimeMillis());
			}*/
			ContentValues cv = new ContentValues();
			cv.put("subskey", db_key);
			cv.put("last_update", System.currentTimeMillis());
			getContentResolver().update(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI, cv, null, null);
		}
		
		public abstract void poll();
	}
	
	/**
	 * Persistent (contacts etc.) and dynamic (recent communication) store.
	 * @author sauvikd
	 *
	 */
	public class CommunicationKnowledgeSubscription extends KnowledgeSubscription {
		public static final String db_key = "Communication";
		
		@Override
		public void poll() {
			//Log.d("KnowledgeTranslatorWrapper", "Entering communication knowledge subscription");
			// TODO Auto-generated method stub
			//things like SMS contacts, outgoing calls etc.
			//Cursor c = Browser.getAllVisitedUrls(mCtx.getContentResolver());
			//c.moveToFirst();
			//Toast.makeText(getApplicationContext(), "Entering Communication Knowlede Subscription", Toast.LENGTH_LONG).show();
			Cursor update_c = getContentResolver().query(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI, new String[] { "subskey", "last_update" }, "subskey = '" + db_key + "'", null, null);
			long last_updated = System.currentTimeMillis() - (3 * UtilityFuncs.DAY_TO_MILLIS);
			
			if (update_c.getCount() > 0) {
				update_c.moveToFirst();
				long temp = update_c.getLong(update_c.getColumnIndex("last_update"));
				last_updated = (temp < last_updated ? last_updated : temp); //either make lower limit 3 days ago or last_updated, whichever is closer to the current time
			} else {
				//Log.d("KnowledgeTranslatorWrapper", "Could not find subscription with db_key " + db_key + " through MyAuthProvider query.");
			}
			update_c.close();
			
			/*
			 * Schema info:
			 * date => long (milliseconds since epoch), number => string, duration => long (seconds)
			 * type => incoming(1)/outgoing(2)/missed(3), cached_name => string
			 */
			
			Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[] { Calls.DATE, Calls.NUMBER, Calls.DURATION, Calls.TYPE, Calls.CACHED_NAME}, Calls.DATE + " > " + last_updated, null, null);
			/*if (c == null) Log.d("KnowledgeTranslatorWrapper", db_key + " Calls.CONTENT_URI returned null cursor; check the URI");
			else {
				if (c.getCount() == 0) {
					Log.d("KnowledgeTranslatorWrapper", "CallLogs query returns empty cursor.");
				}
			}*/
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add fact about calls
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(c.getLong(c.getColumnIndex(Calls.DATE)));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//add person:User tag
					cv = new ContentValues();
					cv.put("tag_class", "Person");
					cv.put("subclass", "User");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add phonecall:outgoing/missed/incoming
					int type = c.getInt(c.getColumnIndex(Calls.TYPE));
					cv = new ContentValues();
					cv.put("tag_class", "Phonecall");
					cv.put("subclass", (type == Calls.INCOMING_TYPE ? "Incoming" : (type == Calls.MISSED_TYPE ? "Missed" : "Outgoing")));
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add person:Contact or person:UnknownNumber
					String name = c.getString(c.getColumnIndex(Calls.CACHED_NAME));
					cv = new ContentValues();
					if (name != null && !name.equalsIgnoreCase("")) { //have the name, so person:Contact
						cv.put("tag_class", "Person");
						cv.put("subclass", "Contact");
						cv.put("subvalue", name);
						cv.put("idtype", "factsid");
						cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
						getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					} else { //don't have the name, so person:UnknownNumber
						cv.put("tag_class", "Person");
						cv.put("subclass", "UnknownNumber");
						cv.put("subvalue", c.getString(c.getColumnIndex(Calls.NUMBER)));
						cv.put("idtype", "factsid");
						cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
						getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					}
					//add phonecall:duration
					cv = new ContentValues();
					cv.put("tag_class", "Phonecall");
					cv.put("subclass", "Duration");
					cv.put("subvalue", c.getLong(c.getColumnIndex(Calls.DURATION))); //in seconds
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//vals.put stuff for facts here
					//getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					c.moveToNext();
				}
			}
			if (c != null) c.close();
			
			//Cursor people = getContentResolver().query(People.CONTENT_URI, new String[] { People.NAME, People.NUMBER }, null, null, null);
			
			c = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[] { "date", "person", "subject", "body", "address" }, "date > " + last_updated, null, null);
			/*if (c == null) Log.d("KnowledgeTranslatorWrapper",db_key + " SMS uri returned null cursor; check the URI");
			else {
				if (c.getCount() == 0) {
					Log.d("KnowledgeTranslatorWrapper", "SMS query returns empty cursor.");
				}
			}*/
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add fact about SMS in inbox
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(c.getLong(c.getColumnIndex("date")));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//add person:User tag
					cv = new ContentValues();
					cv.put("tag_class", "Person");
					cv.put("subclass", "User");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add smsmessage:incoming
					cv = new ContentValues();
					cv.put("tag_class","SmsMessage");
					cv.put("subclass","Incoming");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add smsmessage:subject
					if (c.getString(c.getColumnIndex("subject")) != null) {
						cv = new ContentValues();
						cv.put("tag_class", "SmsMessage");
						cv.put("subclass", "Subject");
						cv.put("subvalue", c.getString(c.getColumnIndex("subject")));
						cv.put("idtype", "factsid");
						cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
						getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					}
					if (c.getString(c.getColumnIndex("body")) != null) {
						cv = new ContentValues();
						cv.put("tag_class","SmsMessage");
						cv.put("subclass","Body");
						cv.put("subvalue", c.getString(c.getColumnIndex("body")));
						cv.put("idtype", "factsid");
						cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
						getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					}
					//add person:Contact
					//need to query another content provider to resolve phone number => id
					cv = new ContentValues();
					cv.put("tag_class","Person");
					Uri temp = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(c.getString(c.getColumnIndex("address"))));
					Cursor people = getContentResolver().query(temp, new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
					if (people.getCount() > 0) {
						people.moveToFirst();
						cv.put("subclass","Contact");
						cv.put("subvalue",people.getString(people.getColumnIndex(PhoneLookup.DISPLAY_NAME)));
					} else {
						cv.put("subclass", "UnknownNumber");
						cv.put("subvalue", c.getString(c.getColumnIndex("address")));
					}
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					people.close();
					//vals.put stuff for facts here
					//getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					c.moveToNext();
				}
			}
			if (c != null) c.close();
			
			c = getContentResolver().query(Uri.parse("content://sms/sent"), new String[] { "date", "person", "subject", "body", "address" }, "date > " + last_updated, null, null);
			/*if (c == null) Log.d("KnowledgeTranslatorWrapper", db_key + " SMS sent uri returned null cursor; check the URI");
			else {
				if (c.getCount() == 0) {
					Log.d("KnowledgeTranslatorWrapper", "SMS sent query returns empty cursor.");
				}
			}*/
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(c.getLong(c.getColumnIndex("date")));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//add person:User tag
					cv = new ContentValues();
					cv.put("tag_class", "Person");
					cv.put("subclass", "User");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add smsmessage:outgoing
					cv = new ContentValues();
					cv.put("tag_class","SmsMessage");
					cv.put("subclass","Outgoing");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add smsmessage:subject
					if (c.getString(c.getColumnIndex("subject")) != null) {
						cv = new ContentValues();
						cv.put("tag_class", "SmsMessage");
						cv.put("subclass", "Subject");
						cv.put("subvalue", c.getString(c.getColumnIndex("subject")));
						cv.put("idtype", "factsid");
						cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
						getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					}
					if (c.getString(c.getColumnIndex("body")) != null) {
						cv = new ContentValues();
						cv.put("tag_class","SmsMessage");
						cv.put("subclass","Body");
						cv.put("subvalue", c.getString(c.getColumnIndex("body")));
						cv.put("idtype", "factsid");
						cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
						getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					}
					//add person:Contact or person:UnknownNumber
					//need to query another content provider to resolve phone number => id
					cv = new ContentValues();
					cv.put("tag_class","Person");
					Uri temp = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(c.getString(c.getColumnIndex("address"))));
					Cursor people = getContentResolver().query(temp, new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
					if (people.getCount() > 0) {
						people.moveToFirst();
						cv.put("subclass","Contact");
						cv.put("subvalue",people.getString(people.getColumnIndex(PhoneLookup.DISPLAY_NAME)));
					} else {
						cv.put("subclass", "UnknownNumber");
						cv.put("subvalue", c.getString(c.getColumnIndex("address")));
					}
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					people.close();
					c.moveToNext();
				}
			}
			if (c != null) c.close();
			
			reset_update_time(db_key);
		}
		
	}
	
	/**
	 * Persistent (files in system) and Dynamic Store (files recently used)
	 * @author sauvikd
	 *
	 */
	public class MediaKnowledgeSubscription extends KnowledgeSubscription {
		public static final String db_key = "Media";

		@Override
		public void poll() {
			// TODO Auto-generated method stub
			/*Cursor update_c = getContentResolver().query(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI, new String[] { "subskey", "last_update" }, "subskey = '" + db_key + "'", null, null);
			long last_updated = System.currentTimeMillis() - (3 * UtilityFuncs.DAY_TO_MILLIS);
			if (update_c.getCount() > 0) {
				update_c.moveToFirst();
				//long temp = update_c.getLong(update_c.getColumnIndex("last_update"));
				//last_updated = (temp < last_updated ? last_updated : temp); //either make lower limit 3 days ago or last_updated, whichever is closer to the current time
				last_updated = update_c.getLong(update_c.getColumnIndex("last_update"));
			} else {
				//Log.d("KnowledgeTranslatorWrapper", "Could not find subscription with db_key " + db_key + " through MyAuthProvider query.");
			}
			update_c.close();
			
			Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
					new String[] { 
						MediaStore.MediaColumns.DATE_ADDED,
						MediaStore.Audio.AudioColumns.TITLE,
						MediaStore.Audio.AudioColumns.ARTIST,
						MediaStore.Audio.AudioColumns.IS_MUSIC }, 
					MediaStore.MediaColumns.DATE_ADDED + " > " + last_updated + " AND " +
							MediaStore.Audio.AudioColumns.IS_MUSIC + " > 0", null, null);
			//need to change this to know when user last accessed, not added
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add persistent fact
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(c.getLong(c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "persistent");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//audio:songtitle
					cv = new ContentValues();
					cv.put("tag_class", "Audio");
					cv.put("subclass", "SongTitle");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//audio:artist
					cv = new ContentValues();
					cv.put("tag_class", "Audio");
					cv.put("subclass", "Artist");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					
					//add dynamic fact
					cv = new ContentValues();
					date = new Date(c.getLong(c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//audio:songtitle
					cv = new ContentValues();
					cv.put("tag_class", "Audio");
					cv.put("subclass", "SongTitle");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//audio:artist
					cv = new ContentValues();
					cv.put("tag_class", "Audio");
					cv.put("subclass", "Artist");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					c.moveToNext();
				}
			}
			if (c != null) c.close();
			
			c = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
					new String[] {
						MediaStore.MediaColumns.DATE_ADDED,
						MediaStore.Video.VideoColumns.TITLE,
						MediaStore.Video.VideoColumns.LATITUDE,
						MediaStore.Video.VideoColumns.LONGITUDE,
						MediaStore.Video.VideoColumns.DATE_TAKEN
			}, MediaStore.MediaColumns.DATE_ADDED + " > " + last_updated, null, null);

			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(c.getLong(c.getColumnIndex(MediaStore.Video.VideoColumns.DATE_TAKEN)));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//video:title
					cv = new ContentValues();
					cv.put("tag_class", "Video");
					cv.put("subclass", "Title");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(c.getColumnIndex(MediaStore.Video.VideoColumns.TITLE)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//audio:artist
					cv = new ContentValues();
					cv.put("tag_class", "Video");
					cv.put("subclass", "Location");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					StringBuffer locVal = new StringBuffer(""+c.getDouble(c.getColumnIndex(MediaStore.Video.VideoColumns.LATITUDE)));
					locVal.append("," + c.getDouble(c.getColumnIndex(MediaStore.Video.VideoColumns.LONGITUDE)));
					cv.put("subvalue", locVal.toString());
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					c.moveToNext();
				}
			}
			if (c != null) c.close();
			
			c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
					new String[] { 
					   MediaStore.MediaColumns.DATE_ADDED,
					   MediaStore.Images.ImageColumns.DATE_TAKEN, 
	         		   MediaStore.Images.ImageColumns.TITLE,
	         		   MediaStore.Images.ImageColumns.LATITUDE,
	         		   MediaStore.Images.ImageColumns.LONGITUDE }, 
         		   MediaStore.MediaColumns.DATE_ADDED + " > " + last_updated, null, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(c.getLong(c.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//video:title
					cv = new ContentValues();
					cv.put("tag_class", "Image");
					cv.put("subclass", "Title");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns.TITLE)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//audio:artist
					cv = new ContentValues();
					cv.put("tag_class", "Image");
					cv.put("subclass", "Location");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					StringBuffer locVal = new StringBuffer(""+c.getDouble(c.getColumnIndex(MediaStore.Images.ImageColumns.LATITUDE)));
					locVal.append("," + c.getDouble(c.getColumnIndex(MediaStore.Images.ImageColumns.LONGITUDE)));
					cv.put("subvalue", locVal.toString());
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					c.moveToNext();
				}
			}
			if (c != null) c.close();*/
			
			reset_update_time(db_key);
		}
	}
	
	/**
	 * Dynamic store
	 * @author sauvikd
	 *
	 */
	public class InternetBrowsingKnowledgeSubscription extends KnowledgeSubscription {
		public static final String db_key = "InternetBrowsing";

		@Override
		public void poll() {
			// TODO Auto-generated method stub
			/*
			 * Schema info:
			 * _id => long, search => string?, date => long (milliseconds since epoch)
			 */
			Cursor update_c = getContentResolver().query(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI, new String[] { "subskey", "last_update" }, "subskey = '" + db_key + "'", null, null);
			long last_updated = System.currentTimeMillis() - (3 * UtilityFuncs.DAY_TO_MILLIS);
			if (update_c.getCount() > 0) {
				update_c.moveToFirst();
				long temp = update_c.getLong(update_c.getColumnIndex("last_update"));
				last_updated = (temp < last_updated ? last_updated : temp); //either make lower limit 3 days ago or last_updated, whichever is closer to the current time
			} /*else {
				Log.d("KnowledgeTranslatorWrapper", "Could not find subscription with db_key " + db_key + " through MyAuthProvider query.");
			}*/
			update_c.close();
			
			Cursor c = getContentResolver().query(Browser.SEARCHES_URI, Browser.SEARCHES_PROJECTION, "date > " + last_updated, null, null);
			/*if (c == null) Log.d("KnowledgeTranslatorWrapper", db_key + " BROWSER.SEARCHES_URI returned null cursor; check the URI");
			else {
				if (c.getCount() == 0) {
					Log.d("KnowledgeTranslatorWrapper", "InternetBrowsing Searches query returns empty cursor.");
				}
			}*/
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add facts about searching history
					//basically, get everything where date is greater than last update time and add it as a fact
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(c.getLong(Browser.SEARCHES_PROJECTION_DATE_INDEX));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//add person:User tag
					cv = new ContentValues();
					cv.put("tag_class", "Person");
					cv.put("subclass", "User");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add internet-action:searched
					cv = new ContentValues();
					cv.put("tag_class", "Internet");
					cv.put("subclass", "Search");
					cv.put("subvalue",c.getString(Browser.SEARCHES_PROJECTION_SEARCH_INDEX));
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add metas here
					//nothing to add to metas for now
					c.moveToNext();
				}
			}
			if (c != null) c.close();

			/* schema info for history
			 * _id => long, url => string, visits => int
			 * date => long (milliseconds since epoch), bookmark => int, title => string
			 * favicon => ?, thumbnail => ?, touch_icon => ?
			 * user_entered => ?
			 */
			c = getContentResolver().query(Browser.BOOKMARKS_URI, Browser.HISTORY_PROJECTION, "date > " + last_updated, null, null);
			/*if (c == null) Log.d("KnowledgeTranslatorWrapper", db_key + " BOOKMARKS_URI returned null cursor; check the URI");
			else {
				if (c.getCount() == 0) {
					Log.d("KnowledgeTranslatorWrapper", "Browser Bookmarks query returns empty cursor.");
				}
			}*/
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add facts about browsing history
					//basically, get everything where date is greater than last update time and add it as a fact
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(c.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//add person:User tag
					cv = new ContentValues();
					cv.put("tag_class", "Person");
					cv.put("subclass", "User");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//internet:visited-site-url
					cv = new ContentValues();
					cv.put("tag_class", "Internet");
					cv.put("subclass", "Visited-Site-Url");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", UtilityFuncs.getURLHost(c.getString(Browser.HISTORY_PROJECTION_URL_INDEX)));
					/*try {
						String tmpURL = c.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
						if (!tmpURL.startsWith("http://")) tmpURL = "http://"+tmpURL;
						String subval = new URL(tmpURL).getHost();
						cv.put("subvalue", subval);
					} catch (MalformedURLException e) {
						cv.put("subvalue", c.getString(Browser.HISTORY_PROJECTION_URL_INDEX));
					}*/
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//internet:visited-site-title
					cv = new ContentValues();
					cv.put("tag_class", "Internet");
					cv.put("subclass", "Visited-Site-Title");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//no metas here
					c.moveToNext();
				}
			}
			if (c != null) c.close();
			
			reset_update_time(db_key);
		}
	}
	
	/**
	 * Persistent store
	 * @author sauvikd
	 *
	 */
	public class UserDictionaryKnowledgeSubscription extends KnowledgeSubscription {
		public static final String db_key = "UserDictionary";

		@Override
		public void poll() {
			// TODO Auto-generated method stub
			/*
			 * Schema info:
			 * _id => long, search => string?, date => long (milliseconds since epoch)
			 */
			/*Cursor update_c = getContentResolver().query(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI, new String[] { "subskey", "last_update" }, "subskey = '" + db_key + "'", null, null);
			long last_updated = System.currentTimeMillis() - (3 * UtilityFuncs.DAY_TO_MILLIS);
			if (update_c.getCount() > 0) {
				update_c.moveToFirst();
				//long temp = update_c.getLong(update_c.getColumnIndex("last_update"));
				//last_updated = (temp < last_updated ? last_updated : temp); //either make lower limit 3 days ago or last_updated, whichever is closer to the current time
				last_updated = update_c.getLong(update_c.getColumnIndex("last_update"));
			} else {
				Log.d("KnowledgeTranslatorWrapper", "Could not find subscription with db_key " + db_key + " through MyAuthProvider query.");
			}
			update_c.close();
			
			Cursor c = getContentResolver().query(UserDictionary.Words.CONTENT_URI, 
					new String[] { UserDictionary.Words.WORD, UserDictionary.Words.FREQUENCY }, null, null, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add data to persistent dictionary here
					//add facts about browsing history
					//basically, get everything where date is greater than last update time and add it as a fact
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(System.currentTimeMillis());
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "persistent");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//add person:User tag
					cv = new ContentValues();
					cv.put("tag_class", "Person");
					cv.put("subclass", "User");
					cv.put("idtype", "factsid");
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					//userdictionary:word
					cv = new ContentValues();
					cv.put("tag_class", "UserDictionary");
					cv.put("subclass", "Word");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(c.getColumnIndex(UserDictionary.Words.WORD)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//userdictionary:frequency
					cv = new ContentValues();
					cv.put("tag_class", "UserDictionary");
					cv.put("subclass", "Frequency");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getInt(c.getColumnIndex(UserDictionary.Words.FREQUENCY)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					c.moveToNext();
				}
			}
			if (c != null) c.close();*/
			
			reset_update_time(db_key);
		}
	}
	
	/**
	 * Persistent store
	 * @author sauvikd
	 *
	 */
	public class ContactKnowledgeSubscription extends KnowledgeSubscription {
		public static final String db_key = "Contact";
		
		@Override
		public void poll() {
			Cursor update_c = getContentResolver().query(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI, new String[] { "subskey", "last_update" }, "subskey = '" + db_key + "'", null, null);
			long last_updated = System.currentTimeMillis() - (3 * UtilityFuncs.DAY_TO_MILLIS);
			if (update_c.getCount() > 0) {
				update_c.moveToFirst();
				//long temp = update_c.getLong(update_c.getColumnIndex("last_update"));
				//last_updated = (temp < last_updated ? last_updated : temp); //either make lower limit 3 days ago or last_updated, whichever is closer to the current time
				last_updated = update_c.getLong(update_c.getColumnIndex("last_update"));
			} else {
				//Log.d("KnowledgeTranslatorWrapper", "Could not find subscription with db_key " + db_key + " through MyAuthProvider query.");
			}
			update_c.close();
			
			Cursor c = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, 
					new String[] { ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.LAST_TIME_CONTACTED }, 
					null, null, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add data to persistent dictionary here
					//add facts about browsing history
					//basically, get everything where date is greater than last update time and add it as a fact
					/*ContentValues cv = new ContentValues();
					String timestamp, dayOfWeek;
					Date date = new Date(System.currentTimeMillis());
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "persistent");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//add person:User tag
					cv = new ContentValues();
					cv.put("tag_class", "Person");
					cv.put("subclass", "User");
					cv.put("idtype", "factsid");
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					//Contact:Name
					cv = new ContentValues();
					cv.put("tag_class", "Contact");
					cv.put("subclass", "Name");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//Contact:Last_Time_Contacted
					cv = new ContentValues();
					cv.put("tag_class", "Contact");
					cv.put("subclass", "Last_Time_Contacted");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					cv.put("subvalue", c.getInt(c.getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED)));
					//Contact:
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);*/
					c.moveToNext();
				}
			}
			if (c!=null) c.close();
			
			reset_update_time(db_key);
		}
	}
	
	/**
	 * Dynamic store. Doesn't work with Android 2.3 apparently. Only android 4.0+
	 * @author sauvikd
	 *
	 */
	public class CalendarKnowledgeSubscription extends KnowledgeSubscription {
		public static final String db_key = "Calendar";
		
		@Override
		public void poll() {
			//Cursor c = getContentResolver().query(CalendarContract, projection, selection, selectionArgs, sortOrder)
			reset_update_time(db_key);
		}
	}
	
	/**
	 * Dynamic store...
	 * @author sauvikd
	 *
	 */
	public class ApplicationUseKnowledgeSubscription extends KnowledgeSubscription {
		public static final String db_key = "ApplicationUse";
		
		@Override
		public void poll() {
			ActivityManager am = (ActivityManager)(getApplicationContext().getSystemService(ACTIVITY_SERVICE));
			PackageManager pm = (PackageManager)(getApplicationContext().getPackageManager());
			List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
			ApplicationInfo ai;
	        try {
	            ai = pm.getApplicationInfo(am.getRunningTasks(1).get(0).baseActivity.getPackageName(), 0);
	        } catch (Exception e) {
	            ai = null;
	        }
	        String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
	        ContentValues cv = new ContentValues();
			String timestamp,dayOfWeek;
			Date date = new Date(System.currentTimeMillis());
			timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
			dayOfWeek = (String) DateFormat.format("EEEE", date);
			cv.put("timestamp", timestamp);
			cv.put("dayOfWeek", dayOfWeek);
			cv.put("persistence", "dynamic");
			Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
			//add person:User tag
			cv = new ContentValues();
			cv.put("tag_class", "Person");
			cv.put("subclass", "User");
			cv.put("idtype", "factsid");
			cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
			getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
			//add application. for now, can't find subclass from market api
			//Log.d("KnowledgeTranslatorWrapper", "Adding fact for " + applicationName);
			cv = new ContentValues();
			cv.put("tag_class", "Application");
			cv.put("subclass", "General-Use");
			cv.put("subvalue", applicationName);
			cv.put("idtype", "factsid");
			cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
			getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
			reset_update_time(db_key);
		}
	}
	
	/**
	 * Dynamic store...
	 * @author sauvikd
	 *
	 */
	public class LocationKnowledgeSubscription extends KnowledgeSubscription {
		public static final String db_key = "Location";

		@Override
		public void poll() {
			// TODO Auto-generated method stub
			Cursor update_c = getContentResolver().query(MyAuthProvider.SUBSCRIPTIONS_CONTENT_URI, new String[] { "subskey", "last_update" }, "subskey = '" + db_key + "'", null, null);
			long last_updated = System.currentTimeMillis() - 10l * UtilityFuncs.MIN_TO_MILLIS;
			if (update_c.getCount() > 0) {
				update_c.moveToFirst();
				last_updated = update_c.getLong(update_c.getColumnIndex("last_update"));
			} else {
				//Log.d("KnowledgeTranslatorWrapper", "Could not find subscription with db_key " + db_key + " through MyAuthProvider query.");
			}
			update_c.close();
			
			LocationManager lm = (LocationManager)(getApplicationContext().getSystemService(LOCATION_SERVICE));
			Location l = null;
			long bestTime = 0l;
			float bestAccuracy = Float.MAX_VALUE;
			long minTime = last_updated;
			
			List<String> matchingProviders = lm.getAllProviders();
			for (String provider: matchingProviders) {
				Location location = lm.getLastKnownLocation(provider);
				if (location != null) {
					float accuracy = location.getAccuracy();
					long time = location.getTime();

					if ((time > minTime && accuracy < bestAccuracy)) {
						l = location;
						bestAccuracy = accuracy;
						bestTime = time;
					}
					else if (time < minTime && 
							bestAccuracy == Float.MAX_VALUE && time > bestTime){
						l = location;
						bestTime = time;
					}
				}
			}
			
			if (l != null) {
				if ((l.getTime() > System.currentTimeMillis() - (10*UtilityFuncs.MIN_TO_MILLIS)) && l.getAccuracy() < 2000.0f) {
					ContentValues cv = new ContentValues();
					String timestamp,dayOfWeek;
					Date date = new Date(l.getTime());
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					cv.put("timestamp", timestamp);
					cv.put("dayOfWeek", dayOfWeek);
					cv.put("persistence", "dynamic");
					Uri factsUri = getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					//add person:User tag
					cv = new ContentValues();
					cv.put("tag_class", "Person");
					cv.put("subclass", "User");
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add location:Provider tag
					cv = new ContentValues();
					cv.put("tag_class","Location");
					cv.put("subclass","Provider");
					cv.put("subvalue", l.getProvider());
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add location:Geopoint tag
					cv = new ContentValues();
					cv.put("tag_class","Location");
					cv.put("subclass","Geopoint");
					cv.put("subvalue", l.getLatitude() + "," + l.getLongitude());
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
					//add location:Accuracy tag
					cv = new ContentValues();
					cv.put("tag_class","Location");
					cv.put("subclass","Accuracy");
					cv.put("subvalue", l.getAccuracy());
					cv.put("idtype", "factsid");
					cv.put("idval", Long.parseLong(factsUri.getLastPathSegment()));
					getContentResolver().insert(MyAuthProvider.TAGS_CONTENT_URI, cv);
				} else {
					//Toast.makeText(getApplicationContext(), "Last location time: " + l.getTime(), Toast.LENGTH_LONG).show();
					//Log.d("KnowledgeTranslatorWrapper", "Last location time: " + DateFormat.format("yyyy-MM-dd kk:mm:ss",l.getTime()));
					//Log.d("KnowledgeTranslatorWrapper", "Last known location accuracy: " + l.getAccuracy());
				}
			}
			reset_update_time(db_key);
		}
	}
	
}
