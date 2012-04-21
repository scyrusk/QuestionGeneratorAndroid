package com.cmuchimps.myauth;

import java.lang.reflect.Modifier;

import android.app.Activity;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.Browser;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.MediaStore;

public class KnowledgeTranslatorWrapper extends Service {
	private KnowledgeTranslatorWrapper thisObj;
	
	public KnowledgeTranslatorWrapper() {
		thisObj = this;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		(new UpdaterTask()).execute(intent.getStringArrayExtra("dueSubs"));
		return Service.START_FLAG_REDELIVERY;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private class UpdaterTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			for (int i = 0; i < params.length; i++) {
				String s = params[i];
				try {
					System.out.println("com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + s + "KnowledgeSubscription");
					Class c = Class.forName("com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + s + "KnowledgeSubscription");
					if (!(Modifier.isStatic(c.getModifiers()) || Modifier.isAbstract(c.getModifiers()))) {
						KnowledgeSubscription ks = (KnowledgeSubscription) c.getDeclaredConstructor(new Class[] { KnowledgeTranslatorWrapper.class }).newInstance(new Object[] { thisObj });
						c.getMethod("poll", null).invoke(ks, null);
						System.out.println("Successfully polled " + s);
					}
				} catch (Throwable e) {
					System.out.println("Failed to update class " + s + "KnowledgeSubscription");
					e.printStackTrace();
				}
			}
			return null;
		}		
	}
	
	public abstract class KnowledgeSubscription {
		public static final boolean IGNORE_DST = true;
		//protected String db_key;
		
		public KnowledgeSubscription() {
			
		}
		
		protected void reset_update_time(String db_key) {
			//update last update time for this subscription
			/*if (db_key == null || db_key.equalsIgnoreCase("")) {
				System.out.println("Failed to reset because of empty db_key");
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
	
	
	public class CommunicationKnowledgeSubscription extends KnowledgeSubscription {
		protected String db_key = "Communication";
		

		@Override
		public void poll() {
			// TODO Auto-generated method stub
			//things like SMS contacts, outgoing calls etc.
			//Cursor c = Browser.getAllVisitedUrls(mCtx.getContentResolver());
			//c.moveToFirst();
			
			/*
			 * Schema info:
			 * date => long (milliseconds since epoch), number => string, duration => long (seconds)
			 * type => incoming(1)/outgoing(2)/missed(3), cached_name => string
			 */
			Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[] { Calls.DATE, Calls.NUMBER, Calls.DURATION, Calls.TYPE, Calls.CACHED_NAME}, null, null, null);
			if (c == null) System.out.println(db_key + " Calls.CONTENT_URI returned null cursor; check the URI");
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add fact about calls
					ContentValues cv = new ContentValues();
					//vals.put stuff for facts here
					//getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					c.moveToNext();
				}
				c.close();
			}
			
			
			c = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[] {}, null, null, null);
			if (c == null) System.out.println(db_key + " SMS uri returned null cursor; check the URI");
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add fact about SMS in inbox
					ContentValues cv = new ContentValues();
					//vals.put stuff for facts here
					//getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					c.moveToNext();
				}
				c.close();
			}
			
			reset_update_time(db_key);
		}
		
	}
	
	public class MediaKnowledgeSubscription extends KnowledgeSubscription {
		protected String db_key = "Media";

		@Override
		public void poll() {
			// TODO Auto-generated method stub
			Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE, MediaStore.MediaColumns.MIME_TYPE}, null, null, null);
			//need to change this to know when user last accessed, not added
			if (c == null) System.out.println(db_key + " returned null cursor; check the URI");
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					
					c.moveToNext();
				}
				c.close();
			}
			
			reset_update_time(db_key);
		}
	}
	
	public class InternetBrowsingKnowledgeSubscription extends KnowledgeSubscription {
		protected String db_key = "InternetBrowsing";

		@Override
		public void poll() {
			// TODO Auto-generated method stub
			/*
			 * Schema info:
			 * _id => long, search => string?, date => long (milliseconds since epoch)
			 */
			Cursor c = getContentResolver().query(Browser.SEARCHES_URI, Browser.SEARCHES_PROJECTION, null, null, null);
			if (c == null) System.out.println(db_key + " BROWSER.SEARCHES_URI returned null cursor; check the URI");
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add facts about searching history
					//basically, get everything where date is greater than last update time and add it as a fact
					ContentValues cv = new ContentValues();
					//vals.put stuff for facts here
					//getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					c.moveToNext();
				}
				c.close();
			}
			

			/* schema info for history
			 * _id => long, url => string, visits => int
			 * date => long (milliseconds since epoch), bookmark => int, title => string
			 * favicon => ?, thumbnail => ?, touch_icon => ?
			 * user_entered => ?
			 */
			c = getContentResolver().query(Browser.BOOKMARKS_URI, Browser.HISTORY_PROJECTION, null, null, null);
			if (c == null) System.out.println(db_key + " BOOKMARKS_URI returned null cursor; check the URI");
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					//add facts about browsing history
					//basically, get everything where date is greater than last update time and add it as a fact
					ContentValues cv = new ContentValues();
					//vals.put stuff for facts here
					//getContentResolver().insert(MyAuthProvider.FACTS_CONTENT_URI, cv);
					c.moveToNext();
				}
				c.close();
			}
			
			
			reset_update_time(db_key);
		}
	}
	
	public class UserDictionaryKnowledgeSubscription extends KnowledgeSubscription {
		protected String db_key = "UserDictionary";

		@Override
		public void poll() {
			// TODO Auto-generated method stub
			
			reset_update_time(db_key);
		}
	}
	
	public class ContactKnowledgeSubscription extends KnowledgeSubscription {
		protected String db_key = "Contact";
		
		@Override
		public void poll() {
			
			reset_update_time(db_key);
		}
	}
	
	public class CalendarKnowledgeSubscription extends KnowledgeSubscription {
		protected String db_key = "Calendar";
		
		@Override
		public void poll() {
			
			reset_update_time(db_key);
		}
	}


	
}
