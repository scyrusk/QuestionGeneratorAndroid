package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

/**
 * Presently only meant to add facts. All other functionalities are either unimplemented or flakey.
 * @author sauvikd
 *
 */
public class MyAuthProvider extends ContentProvider {
	private KBDbAdapter mDbHelper;
	private static final String AUTHORITY = "com.cmuchimps.myauth.MyAuthProvider";
	private static final String SUBSCRIPTIONS_BASE_PATH = "subscriptions";
	private static final String FACTS_BASE_PATH = "facts";
	private static final String TAGS_BASE_PATH = "tags";
	private static final String METAS_BASE_PATH = "metas";
	
	public static final String[] FACTS_PROJECTION = { "sup" };
	public static final String[] SUBSCRIPTIONS_PROJECTION = { };
	public static final int SUBSCRIPTIONS = 1;
	public static final int SUBSCRIPTIONS_ID = 2;
	public static final int SUBSCRIPTIONS_DUE = 3;
	public static final int FACTS = 4;
	public static final int FACTS_ID = 5;
	public static final int TAGS = 6;
	public static final int METAS = 7;
	
	public static final Uri SUBSCRIPTIONS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + SUBSCRIPTIONS_BASE_PATH);
	public static final Uri SUBSCRIPTIONS_DUE_URI = Uri.parse("content://" + AUTHORITY + "/" + SUBSCRIPTIONS_BASE_PATH + "/due");
	public static final Uri FACTS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + FACTS_BASE_PATH);
	public static final Uri TAGS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TAGS_BASE_PATH);
	public static final Uri METAS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" +  METAS_BASE_PATH);
	
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
	        + "/mt-subscription";
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
	        + "/mt-subscription";
	
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, SUBSCRIPTIONS_BASE_PATH, SUBSCRIPTIONS);
		sURIMatcher.addURI(AUTHORITY, SUBSCRIPTIONS_BASE_PATH + "/#", SUBSCRIPTIONS_ID);
		sURIMatcher.addURI(AUTHORITY, SUBSCRIPTIONS_BASE_PATH + "/due", SUBSCRIPTIONS_DUE);
		sURIMatcher.addURI(AUTHORITY, FACTS_BASE_PATH, FACTS);
		sURIMatcher.addURI(AUTHORITY, FACTS_BASE_PATH + "/#", FACTS_ID);
		sURIMatcher.addURI(AUTHORITY, TAGS_BASE_PATH, TAGS);
		sURIMatcher.addURI(AUTHORITY, METAS_BASE_PATH, METAS);
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		int uriType = sURIMatcher.match(uri);
		Uri mNewUri;
		switch(uriType) {
		case FACTS:
			//separate tags and metas
			String timestamp = values.getAsString("timestamp"), dayOfWeek = values.getAsString("dayOfWeek");
			long _fact_id = mDbHelper.createFact(timestamp, dayOfWeek);
			mNewUri = Uri.parse(FACTS_CONTENT_URI + "/" + _fact_id);
			break;
		case SUBSCRIPTIONS:
			String subskey = values.getAsString("subskey"), class_name = values.getAsString("class_name");
			long poll_interval = values.getAsLong("poll_interval"), last_update = values.getAsLong("last_update"); 
			long _subs_id = mDbHelper.createSubscription(subskey, poll_interval, last_update, class_name);
			mNewUri = Uri.parse(FACTS_CONTENT_URI + "/" + _subs_id);
			break;
		case TAGS:
			String tag_class = values.getAsString("tag_class"),subclass = values.getAsString("subclass"),subvalue = (values.containsKey("subvalue") ? values.getAsString("subvalue") : ""),tag_idtype = (values.getAsString("idtype"));
			long tag_idval = values.getAsLong("idval");
			long _tags_id = mDbHelper.createTag(tag_class, subclass, subvalue, tag_idtype, tag_idval);
			mNewUri = Uri.parse(TAGS_CONTENT_URI + "/" + _tags_id);
			break;
		case METAS:
			String name = values.getAsString("name"), value = (values.containsKey("value") ? values.getAsString("value") : ""), meta_idtype = values.getAsString("idtype");
			long meta_idval = values.getAsLong("idval");
			long _metas_id = mDbHelper.createMeta(name, value, meta_idtype, meta_idval);
			mNewUri = Uri.parse(METAS_CONTENT_URI + "/" + _metas_id);
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI.");
		}
		return mNewUri;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		mDbHelper = new KBDbAdapter(getContext());
		mDbHelper.open();
		return true;
	}
	/**
	 * Note, for facts, right now this doesn't consider relations at all so it's pretty useless.
	 * Will have to change to make it useful by running a giant JOIN query on facts, tags etc.
	 * Note: MUST call cursor.getDatabase().close() after use.
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		System.out.println("Content provider query URI : " + uri.toString());
		int uriType = sURIMatcher.match(uri);
		Cursor c = null;
		
		switch (uriType) {
		case FACTS:
			c = mDbHelper.fetchAllFacts(projection,selection,selectionArgs,sortOrder);
			break;
		case FACTS_ID:
			c = mDbHelper.fetchFact(Long.parseLong(uri.getLastPathSegment()), projection);
			break;
		case SUBSCRIPTIONS:
			c = mDbHelper.fetchAllSubscriptions(projection,selection,selectionArgs,sortOrder);
			break;
		case SUBSCRIPTIONS_ID:
			c = mDbHelper.fetchSubscription(Long.parseLong(uri.getLastPathSegment()), projection);
			break;
		case SUBSCRIPTIONS_DUE:
			c = mDbHelper.fetchDueSubscriptions();
			break;
		default:
			throw new IllegalArgumentException("Unknown URI.");
		}
		
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * Presently only meant to update last_update time of subscriptions
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		int uriType = sURIMatcher.match(uri);
		
		int retVal = 0;
		
		switch (uriType) {
		case FACTS:
			//TODO: have to make some kind of complex function that recursively updates tags and metas, but 
			//do not need to do that right now
			retVal = 0;
			break;
		case SUBSCRIPTIONS:
			retVal = mDbHelper.updateSubscriptionTime(values.getAsString("subskey"), values.getAsLong("last_update"));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI.");
		}
		
		//mDbHelper.close();
		
		return retVal;
	}

}
