package com.cmuchimps.myauth;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.cmuchimps.myauth.DataWrapper.Acond;
import com.cmuchimps.myauth.DataWrapper.Fact;
import com.cmuchimps.myauth.DataWrapper.Meta;
import com.cmuchimps.myauth.DataWrapper.QAT;
import com.cmuchimps.myauth.DataWrapper.Qcond;
import com.cmuchimps.myauth.DataWrapper.Tag;

public class KBDbAdapter {
	private DataWrapper dw;
	public static final String KEY_ROWID = "_id";
	public static final String DATABASE_NAME = "knowledge_base";
	public static final int DATABASE_VERSION = 2;
	private static final String LOG_TAG = "KBDbAdapter";
	private final Context mCtx;
	
	private static final String FACTS_TABLE = "facts";
	private static final String TAGS_TABLE = "tags";
	private static final String META_TABLE = "meta";
	private static final String QAT_TABLE = "qats";
	private static final String QCOND_TABLE = "qconds";
	private static final String ACOND_TABLE = "aconds";
	private static final String TAG_CLASS_TABLE = "tag_class";
	private static final String DESCRIPTIONS_TABLE = "descriptions";
	private static final String SUBSCRIPTIONS_TABLE = "subscriptions";
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String TAG_CLASS_CREATE = 
			"create table " + TAG_CLASS_TABLE + " (_id integer primary key autoincrement, "
			+ "name string NOT NULL);";
	private static final String META_CREATE =
			"create table " + META_TABLE + " (_id integer primary key autoincrement, "
			+ "name string NOT NULL, value string,"
			+ "factsid integer references " + FACTS_TABLE + "(id)," 
			+ "qatid integer references " + QAT_TABLE + "(id));";
	private static final String FACTS_CREATE = 
			"create table " + FACTS_TABLE + " (_id integer primary key autoincrement, "
			+ "timestamp date, "
			+ "dayOfWeek string, " 
			+ "queried string, "
			+ "persistence string);";
	
	
	private static final String DESCRIPTIONS_CREATE = 
			"create table " + DESCRIPTIONS_TABLE + " (_id integer primary key autoincrement, "
			+ "description string NOT NULL, "
			+ "acondsid integer references " + ACOND_TABLE + "(id));";
	private static final String QCOND_CREATE = 
			"create table " + QCOND_TABLE + " (_id integer primary key autoincrement, "
			+ "refnum integer, "
			+ "qatid integer references " + QAT_TABLE + "(id));";
	private static final String ACOND_CREATE = 
			"create table " + ACOND_TABLE + " (_id integer primary key autoincrement, "
			+ "qatid integer references " + QAT_TABLE + "(id));";
	private static final String QAT_CREATE =
			"create table " + QAT_TABLE + " (_id integer primary key autoincrement, qtext string NOT NULL);";
	
	private static final String TAGS_CREATE =
			"create table " + TAGS_TABLE + "(_id integer primary key autoincrement," 
			+ "tag_classid integer references " + TAG_CLASS_TABLE + "(id) NOT NULL,"
			+ " subclass string NOT NULL, "
			+ "subvalue string, "
			+ "factsid integer references " + FACTS_TABLE + "(id),"
			+ "qcondsid integer references " + QCOND_TABLE + "(id)," 
			+ "acondsid integer references " + ACOND_TABLE + "(id));";
	
	private static final String SUBSCRIPTIONS_CREATE = 
			"create table " + SUBSCRIPTIONS_TABLE + "(_id integer primary key autoincrement,"
			+ "subskey string NOT NULL,"
			+ "poll_interval integer NOT NULL,"
			+ "last_update integer NOT NULL,"
			+ "class_name string NOT NULL);";
	
	private static final String[] TABLES = {TAG_CLASS_TABLE, FACTS_TABLE, META_TABLE, DESCRIPTIONS_TABLE, 
		QCOND_TABLE, ACOND_TABLE, QAT_TABLE, TAGS_TABLE, SUBSCRIPTIONS_TABLE};
	private static final String[] TABLE_CREATION = {TAG_CLASS_CREATE, QAT_CREATE, META_CREATE, FACTS_CREATE,
		QCOND_CREATE, ACOND_CREATE, DESCRIPTIONS_CREATE, TAGS_CREATE, SUBSCRIPTIONS_CREATE};
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			// TODO Auto-generated constructor stub
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			for (String s : TABLE_CREATION) {
				db.execSQL(s);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			/*Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");*/
			for (String s : TABLES) {
				db.execSQL("DROP TABLE IF EXISTS " + s);
			}
			onCreate(db);
		}
	}
	
	/**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
	public KBDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}
	
	public void setDW(DataWrapper wrapper) {
		this.dw = wrapper;
	}
	/**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public KBDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
	
    public void close() {
    	mDbHelper.close();
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public synchronized long createTagClass(String name) {
    	ContentValues initialValues = new ContentValues();
    	initialValues.put("name", name); //put the name variable in the "name" column
    	return mDb.insert(TAG_CLASS_TABLE, null, initialValues);
    }

    /**
     * 
     * @param name
     * @return
     */
    public synchronized long findTagClassByName(String name) {
    	Cursor c = mDb.query(TAG_CLASS_TABLE, new String[] {KEY_ROWID, "name"}, "name='"+name+"'", null, null, null, null);
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		Long retVal = c.getLong(c.getColumnIndex(KEY_ROWID));
    		c.close();
    		return retVal;
    	}
    	c.close();
    	return -1;
    }
    
    public synchronized long returnTagNumbers(String name) {
    	Cursor c = mDb.query(TAG_CLASS_TABLE, new String[] {KEY_ROWID, "name"}, "name='"+name+"'", null, null, null, null);
    	int retVal = c.getCount();
    	c.close();
    	return retVal;
    }
    
    /**
     * 
     * @param rowId
     * @return
     */
    public synchronized boolean deleteTagClass(long rowId) {
    	return mDb.delete(TAG_CLASS_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * 
     * @param rowId
     * @param name
     * @return
     */
    public synchronized boolean updateTagClass(long rowId, String name) {
    	ContentValues args = new ContentValues();
    	args.put("name", name);
    	
    	return mDb.update(TAG_CLASS_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * 
     * @return
     */
    public synchronized Cursor fetchAllTagClasses() {
    	return mDb.query(TAG_CLASS_TABLE, new String[] {KEY_ROWID, "name"}, null, null, null, null, null);
    }
    
    public synchronized long createFact(String timestamp, String dayOfWeek) {
    	ContentValues fact_vals = new ContentValues();
    	fact_vals.put("timestamp", UtilityFuncs.join(timestamp.split("T"), " "));
    	fact_vals.put("dayOfWeek", dayOfWeek);
    	fact_vals.put("queried", "false");
    	return mDb.insert(FACTS_TABLE, null, fact_vals);
    }
    
    public synchronized long createFact(String timestamp, String dayOfWeek, String persistence) {
    	ContentValues fact_vals = new ContentValues();
    	fact_vals.put("timestamp", UtilityFuncs.join(timestamp.split("T"), " "));
    	fact_vals.put("dayOfWeek", dayOfWeek);
    	fact_vals.put("persistence", persistence);
    	fact_vals.put("queried", "false");
    	return mDb.insert(FACTS_TABLE, null, fact_vals);
    }
    
    public synchronized boolean registerFactAsQueried(long rowId) {
    	ContentValues args = new ContentValues();
    	args.put("queried", "true");
    	//Log.d("KBDbAdapter", "registering fact with id " + rowId + " as queried");
    	return mDb.update(FACTS_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public synchronized int registerFactsAsQueried(ArrayList<Long> rowIds) {
    	if (rowIds.size() <= 0) return 0;
    	ContentValues args = new ContentValues();
    	args.put("queried", "true");
    	return mDb.update(FACTS_TABLE, args, KEY_ROWID + " in " + UtilityFuncs.joinInCaluse(rowIds), null);
    }
    
    public synchronized boolean isFactQueried(long rowId) {
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID, "queried" }, 
    			KEY_ROWID + "=" + rowId, null, null, null, null);
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		String compVal = c.getString(c.getColumnIndex("queried"));
    		c.close();
    		return (compVal != null && compVal.equalsIgnoreCase("true"));
    	}
    	c.close();
    	return false;
    }
    
    public synchronized Long[] getFactsWithNullQueried() {
    	ArrayList<Long> retVal = new ArrayList<Long>();
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID }, 
    			"queried is null", null, null, null, null);
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		while (!c.isAfterLast()) {
    			retVal.add(c.getLong(c.getColumnIndex(KEY_ROWID)));
    			c.moveToNext();
    		}
    	}
    	c.close();
    	return retVal.toArray(new Long[retVal.size()]);
    }
    
    public synchronized void cullOldFacts(String timestamp, int thresholdDays) {
    	String pivotTime = (timestamp.equalsIgnoreCase("*") ? "'now'" : "'" + timestamp + "'");
    	String query = "timestamp < datetime(" + pivotTime + ",'-" + thresholdDays + " days') AND persistence = 'dynamic'";
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID }, query, null, null, null, null);
    	//Log.d("myAuth", "Culling " + c.getCount() + " fact(s)");
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		while (!c.isAfterLast()) {
    			this.deleteFact(c.getLong(0));
    			c.moveToNext();
    		}
    	}
    	c.close();
    	//Log.d("myAuth", "Done culling.");
    }
    
    public synchronized long createTag(String tag_class,String subclass,String subvalue,String idtype,long idval) {
    	ContentValues tag_vals = new ContentValues();
    	long tagclass_id = this.findTagClassByName(tag_class);
		if (tagclass_id >= 0) 
			tag_vals.put("tag_classid", tagclass_id);
		else return -1;
		
		if (idtype.equalsIgnoreCase("factsid") || idtype.equalsIgnoreCase("qcondsid") || idtype.equalsIgnoreCase("acondsid"))
			tag_vals.put(idtype, idval);
		else return -1;
		
		tag_vals.put("subclass", subclass);
		if (subvalue != null && !subvalue.equalsIgnoreCase("")) tag_vals.put("subvalue", subvalue);
    	return mDb.insert(TAGS_TABLE, null, tag_vals);
    }
    
    public synchronized long createMeta(String name, String value, String idtype, long idval) {
    	ContentValues meta_values = new ContentValues();
    	meta_values.put("name", name);
    	if (value != null) meta_values.put("value", value);
    	if (idtype.equalsIgnoreCase("factsid") || idtype.equalsIgnoreCase("qatid"))
    		meta_values.put(idtype, idval);
    	else return -1;
    	return mDb.insert(META_TABLE, null, meta_values);
    }
    
    public synchronized long createFact(String timestamp, String dayOfWeek, String persistence, ArrayList<HashMap<String,String>> tags, ArrayList<HashMap<String,String>> metas) {
    	/* Insert fact */
    	ContentValues fact_vals = new ContentValues();
    	fact_vals.put("timestamp", UtilityFuncs.join(timestamp.split("T"), " "));
    	fact_vals.put("dayOfWeek", dayOfWeek);
    	fact_vals.put("queried", "false");
    	fact_vals.put("persistence", persistence);
    	long fact_id = mDb.insert(FACTS_TABLE, null, fact_vals);
    	
    	if (fact_id < 0)
    		return fact_id;
    	
    	/*Insert tags */
    	for (HashMap<String,String> tag : tags) {
    		if (tag.keySet().contains("tag_class") && tag.keySet().contains("subclass")) {
    			ContentValues tag_vals = new ContentValues();
    			//find tag_class id to enter into db
    			long tagclass_id = this.findTagClassByName(tag.get("tag_class"));
    			if (tagclass_id >= 0) tag_vals.put("tag_classid", tagclass_id);
    			else continue;
    			tag_vals.put("factsid", fact_id);
    			tag_vals.put("subclass", tag.get("subclass"));
    			if (tag.keySet().contains("subvalue")) tag_vals.put("subvalue", tag.get("subvalue"));
    			mDb.insert(TAGS_TABLE, null, tag_vals);
    		}
    	}
    	
    	/* Insert metas*/
    	for (HashMap<String,String> meta : metas) {
    		if (meta.keySet().contains("name")) {
    			ContentValues meta_vals = new ContentValues();
    			meta_vals.put("name", meta.get("name"));
    			if (meta.keySet().contains("value")) meta_vals.put("value", meta.get("value"));
    			meta_vals.put("factsid", fact_id);
    			mDb.insert(META_TABLE,null,meta_vals);
    		}
    	}
    	
    	return fact_id;
    }
    
    /**
     * Finds all facts with the given <tag_class,subclass,subvalue> tuples.
     * subclass and subvalue values can be '*' which means that those values can be ignored
     * for multiple subclasses of the same tag class, it is permissible to pass
     * in an array of tags with duplicate entries.
     * 
     * e.g. to find all facts with Person:Contact=Jason and Person User,
     *      parameters would look like:
     *      	tags: ['Person','Person']
     *      	subclasses: ['Contact','User']
     *      	subvalues: ['Jason','*'].
     *      
     *      The return value will be a list of factids that have the given tags.
     * @param tags
     * @param subclasses
     * @param subvalues
     * @return
     */
    public synchronized Long[] findAllFactsWithTags(String[] tags, String[] subclasses, String[] subvalues) {
    	if (tags.length != subclasses.length && subvalues.length != tags.length)
    		return new Long[0];
    	long[] tag_class_ids = new long[tags.length];
    	int counter = 0;
    	ArrayList<Integer> valid = new ArrayList<Integer>();
    	for (int i = 0; i < tags.length; i++) {
    		String s = tags[i];
    		long val = this.findTagClassByName(s);
    		if (val >= 0) valid.add(i);
    		tag_class_ids[counter++] = val;
    	}
    	
    	ArrayList<HashSet<Long>> queryResults = new ArrayList<HashSet<Long>>();
    	// Get facts that match each qualifying valid tag,subclass,subvalue tuple
    	for (int index : valid) {
    		StringBuffer selectionQuery = new StringBuffer();
    		selectionQuery.append("tag_classid = " + tag_class_ids[index]);
    		if (!subclasses[index].equalsIgnoreCase("*")) {
    			selectionQuery.append(" AND subclass = '" + subclasses[index] + "'");
    			if (!subvalues[index].equalsIgnoreCase("*")) 
    				selectionQuery.append(" AND subvalue = '" + subvalues[index] + "'");
    		}
    		// selectionQuery.append(" AND persistence = '" + persistence + "'");
    		Cursor c = mDb.query(TAGS_TABLE, 
    				new String[] { KEY_ROWID, "tag_classid", "subclass", "subvalue", "factsid" }, 
        			selectionQuery.toString(), null, null, null, null);
    		
    		/*StringBuffer q = new StringBuffer();
    		q.append("SELECT a." + KEY_ROWID + ", a.tag_classid, a.subclass, a.subvalue, a.factsid ");
    		q.append("FROM " + TAGS_TABLE + " a JOIN " + FACTS_TABLE + " b ON ");
    		q.append("a.factsid = b." + KEY_ROWID + " AND b.persistence = '" + persistence + "' AND ");
    		q.append(selectionQuery.toString());*/
    		//System.out.println(q.toString());
    		
    		//Cursor c = mDb.rawQuery(q.toString(), null);
    		if (c != null && c.getCount() > 0) {
    			HashSet<Long> queryResult = new HashSet<Long>();
    			c.moveToFirst();
    			while (!c.isAfterLast()) {
    				queryResult.add(c.getLong(c.getColumnIndex("factsid")));
    				c.moveToNext();
    			}
    			queryResults.add(queryResult);
    		}
    		if (c != null) c.close();
    	}
    	
    	// If no valid results, then return nothing
    	if (queryResults.size() == 0)
    		return new Long[0];
    	// Intersect all ArrayLists in queryLists so that we can get only the facts that satisfy all constraints
    	HashSet<Long> cum = queryResults.get(0);
    	for (int i = 1; i < queryResults.size(); i++)
    		cum.retainAll(queryResults.get(i));
    	
    	return cum.toArray(new Long[cum.size()]);
    }
    
    public synchronized Long[] getAllQatsWithMeta(String meta_key, String meta_value) {
    	String selection = "name = '" + meta_key + "' AND value = '" + meta_value + "' AND qatid IS NOT NULL";
    	Cursor c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "name", "value", "qatid" }, selection, null, null, null, null);
    	
    	if (c != null && c.getCount() > 0) {
    		Long[] retVal = new Long[c.getCount()];
    		int counter = 0;
    		c.moveToFirst();
    		while (!c.isAfterLast()) {
    			retVal[counter++] = c.getLong(c.getColumnIndex("qatid"));
    			c.moveToNext();
    		}
    		return retVal;
    	}
    	if (c != null) c.close();
    	
    	return new Long[0];
    }
    
    public synchronized Long[] findAllFactsWithTagsWithinTime(String[] tags, String[] subclasses, 
    		String[] subvalues, long ellapsedTimeMillis, String persistence) {
    	Long[] tagFacts = findAllFactsWithTags(tags, subclasses, subvalues);
    	Long[] timeFacts = getFilteredFactsByTime(ellapsedTimeMillis, "*", persistence);
    	//Log.d("fafwt", "tag: "+ tagFacts.length);
    	//Log.d("fafwt", "time: "+ timeFacts.length);
    	HashSet<Long> tagFactsSet = new HashSet<Long>();
    	HashSet<Long> timeFactsSet = new HashSet<Long>();
    	int max = Math.max(tagFacts.length, timeFacts.length);
    	for (int i = 0; i < max; i++) {
    		if (i < tagFacts.length) tagFactsSet.add(tagFacts[i]);
    		if (i < timeFacts.length) timeFactsSet.add(timeFacts[i]);
    	}
    	tagFactsSet.retainAll(timeFactsSet);
    	//Log.d("fafwt", "intersect: " + tagFactsSet.size());
    	return tagFactsSet.toArray(new Long[tagFactsSet.size()]);
    }
    
    
    public synchronized Long[] intersectFactsWithPersistence(Long[] original, String persistence) {
    	Cursor c = mDb.query(FACTS_TABLE, 
    			new String[] { KEY_ROWID, "persistence" }, 
    			"persistence = '" + persistence +"'", null, null, null, null);
    	HashSet<Long> origSet = new HashSet<Long>();
    	for (Long l : original) origSet.add(l);
    	HashSet<Long> persistSet = new HashSet<Long>();
    	if (c != null && c.getCount() > 0) {
    		c.moveToFirst();
    		while (!c.isAfterLast()) {
    			persistSet.add(c.getLong(c.getColumnIndex(KEY_ROWID)));
    			c.moveToNext();
    		}
    	}
    	if (c != null) c.close();
    	origSet.retainAll(persistSet);
    	return origSet.toArray(new Long[origSet.size()]);
    }
    
    /**
     * 
     * @param rowId
     * @return
     */
    public synchronized boolean deleteFact(long rowId) {
    	//TODO: delete all tags/metas associated with this fact
    	int val = 0;
		if (mDb.isDbLockedByCurrentThread()) return false;
		mDb.delete(TAGS_TABLE, "factsid=" + rowId, null);
    	mDb.delete(META_TABLE, "factsid=" + rowId, null);
    		
    	val = mDb.delete(FACTS_TABLE, KEY_ROWID + "=" + rowId, null);
    	
    	return val > 0;
    }
    
    /**
     * 
     * @return
     */
    public synchronized Cursor fetchAllFacts(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    	return mDb.query(FACTS_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
    }
    
    public synchronized Cursor fetchFact(long row_id, String[] projection) {
    	return mDb.query(FACTS_TABLE, projection, KEY_ROWID + "=" + row_id, null, null, null, null);
    }
    
    /**
     * To create a full QAT, must write a wrapper function that first creates QAT and saves rowId
     * Then, create Aconds and Qconds and pass in the rowId of QAT.
     * @param qtext
     * @param metas
     * @return
     */
    public synchronized long createQAT(String qtext, ArrayList<HashMap<String,String>> metas) {
    	ContentValues qat_vals = new ContentValues();
    	qat_vals.put("qtext", qtext);
    	long qat_id = mDb.insert(QAT_TABLE, null, qat_vals);
    	
    	/* Insert metas*/
    	for (HashMap<String,String> meta : metas) {
    		if (meta.keySet().contains("name")) {
    			ContentValues meta_vals = new ContentValues();
    			meta_vals.put("name", meta.get("name"));
    			if (meta.keySet().contains("value")) meta_vals.put("value", meta.get("value"));
    			meta_vals.put("qatid", qat_id);
    			mDb.insert(META_TABLE,null,meta_vals);
    		}
    	}
    	
    	return qat_id;
    }
    
    /**
     * 
     * @param rowId
     * @return
     */
    public synchronized boolean deleteQAT(long rowId) {
    	//delete all Aconds, Qconds, Metas, and then QAT
    	//for Aconds/Qconds, need to do recursively because they are complex
    	
    	//recursive call to delete all Aconds
    	//recursive call to delete all Qconds
    	Cursor toDel = mDb.query(ACOND_TABLE, new String[] {KEY_ROWID,"qatid"}, "qatid=" + rowId, null, null, null, null);
    	toDel.moveToFirst();
    	while (!toDel.isAfterLast()) {
    		this.deleteAcond(toDel.getLong(toDel.getColumnIndex(KEY_ROWID)));
    		toDel.moveToNext();
    	}
    	toDel.close();
    	
    	toDel = mDb.query(QCOND_TABLE, new String[] {KEY_ROWID, "qatid"}, null, null, null, null, null);
    	toDel.moveToFirst();
    	while (!toDel.isAfterLast()) {
    		this.deleteAcond(toDel.getLong(toDel.getColumnIndex(KEY_ROWID)));
    		toDel.moveToNext();
    	}
    	toDel.close();
    	
    	mDb.delete(META_TABLE, "qatid=" + rowId, null);
    	return mDb.delete(QAT_TABLE, KEY_ROWID+"="+rowId, null) > 0;
    }
    
    /**
     * 
     * @return
     */
    public synchronized Cursor fetchAllQATs() {
    	return mDb.query(QAT_TABLE, new String[] {KEY_ROWID,"qtext"}, null, null, null, null, null);
    }
    
    /**
     * 
     * @return
     */
    public int getNumQATS() {
    	return fetchAllQATs().getCount();
    }
    
    /**
     * 
     * @param qatid
     * @param descs
     * @param tags
     * @return
     */
    public synchronized long createAcond(long qatid, ArrayList<String> descs, ArrayList<HashMap<String,String>> tags) {
    	ContentValues vals = new ContentValues();
    	vals.put("qatid", qatid);
    	long acond_id = mDb.insert(ACOND_TABLE,null,vals);
    	
    	/* insert descs */
    	for (String desc : descs) {
			ContentValues descVals = new ContentValues();
			descVals.put("description", desc);
			descVals.put("acondsid", acond_id);
			mDb.insert(DESCRIPTIONS_TABLE, null, descVals);
    	}
    	
    	/*Insert tags */
    	for (HashMap<String,String> tag : tags) {
    		if (tag.keySet().contains("tag_class") && tag.keySet().contains("subclass")) {
    			ContentValues tag_vals = new ContentValues();
    			//find tag_class id to enter into db
    			long tagclass_id = this.findTagClassByName(tag.get("tag_class"));
    			if (tagclass_id > 0) tag_vals.put("tag_classid", tagclass_id);
    			else continue;
    			tag_vals.put("acondsid", acond_id);
    			tag_vals.put("subclass", tag.get("subclass"));
    			if (tag.keySet().contains("subvale")) tag_vals.put("subvalue", tag.get("subval"));
    			mDb.insert(TAGS_TABLE, null, tag_vals);
    		}
    	}
    	
    	return acond_id;
    }
    
    /**
     * 
     * @param rowId
     * @return
     */
    public synchronized int deleteAcond(long rowId) {
    	//TODO: delete all descriptions referring to Acond, then Tags, then Acond
    	mDb.delete(DESCRIPTIONS_TABLE, "acondsid=" + rowId, null);
    	mDb.delete(TAGS_TABLE, "acondsid=" + rowId, null);
    	return mDb.delete(ACOND_TABLE, KEY_ROWID+"="+rowId, null);
    }
    
    /**
     * 
     * @param qatid
     * @param refnum
     * @param tags
     * @return
     */
    public synchronized long createQcond(long qatid, int refnum, ArrayList<HashMap<String,String>> tags) {
    	ContentValues vals = new ContentValues();
    	vals.put("qatid", qatid);
    	vals.put("refnum", refnum);
    	long qcond_id = mDb.insert(QCOND_TABLE, null, vals);
    	
    	/*Insert tags */
    	for (HashMap<String,String> tag : tags) {
    		if (tag.keySet().contains("tag_class") && tag.keySet().contains("subclass")) {
    			ContentValues tag_vals = new ContentValues();
    			//find tag_class id to enter into db
    			long tagclass_id = this.findTagClassByName(tag.get("tag_class"));
    			if (tagclass_id > 0) tag_vals.put("tag_classid", tagclass_id);
    			else continue;
    			tag_vals.put("qcondsid", qcond_id);
    			tag_vals.put("subclass", tag.get("subclass"));
    			if (tag.keySet().contains("subvale")) tag_vals.put("subvalue", tag.get("subval"));
    			mDb.insert(TAGS_TABLE, null, tag_vals);
    		}
    	}
    	
    	return qcond_id;
    }
    
    /**
     * 
     * @param rowId
     * @return
     */
    public synchronized long deleteQcond(long rowId) {
    	//TODO delete all tags referring to Qcond, then delete Qcond
    	mDb.delete(TAGS_TABLE, "qcondsid="+rowId, null);
    	return mDb.delete(QCOND_TABLE, KEY_ROWID+"="+rowId, null);
    }
    
    
    public synchronized long createSubscription(String subskey,long poll_interval,long last_update,String class_name) {
    	ContentValues vals = new ContentValues();
    	vals.put("subskey",subskey);
    	vals.put("poll_interval", poll_interval);
    	vals.put("last_update", last_update);
    	vals.put("class_name", class_name);
    	return mDb.insert(SUBSCRIPTIONS_TABLE,null,vals);
    }
    
    public synchronized int deleteSubscription(long rowId) {
    	return mDb.delete(SUBSCRIPTIONS_TABLE, KEY_ROWID + "=" + rowId, null);
    }
    
    public synchronized int deleteSubscription(String subskey) {
    	return mDb.delete(SUBSCRIPTIONS_TABLE, "subskey='" + subskey + "'", null);
    }
    
    public synchronized HashMap<String,String> getAllSubscriptions() {
    	HashMap<String,String> subs = new HashMap<String,String>();
    	Cursor c = mDb.query(SUBSCRIPTIONS_TABLE, new String[] { KEY_ROWID, "subskey", "class_name" }, null, null, null, null, null);
    	c.moveToFirst();
    	while (!c.isAfterLast()) {
    		subs.put(c.getString(c.getColumnIndex("subskey")),c.getString(c.getColumnIndex("class_name")));
    		c.moveToNext();
    	}
    	c.close();
    	return subs;
    }
    
    public synchronized int getNumSubscriptions() {
    	return mDb.query(SUBSCRIPTIONS_TABLE, new String[] { KEY_ROWID }, null, null, null, null, null).getCount();
    }
    
    public synchronized String getSubscriptionClassFor(String subskey) {
    	Cursor c = mDb.query(SUBSCRIPTIONS_TABLE, new String[] { "subskey", "class_name" }, "subskey='" + subskey + "'", null, null, null, null);
    	if (c.getCount() == 0) {
    		c.close();
    		return "";
    	}
    	String retVal = c.getString(c.getColumnIndex("class_name"));
    	c.close();
    	return retVal;
    }
    
    public synchronized int updateSubscriptionTime(String subskey,long last_update) {
		ContentValues vals = new ContentValues();
		vals.put("last_update", last_update);
		return mDb.update(SUBSCRIPTIONS_TABLE, vals, "subskey='"+subskey+"'", null);
    }
    
    public synchronized HashMap<String,String> getAllDueSubscriptions() {
    	HashMap<String,String> subs = new HashMap<String,String>();
    	Cursor c = mDb.query(SUBSCRIPTIONS_TABLE, new String[] { KEY_ROWID, "subskey", "class_name", "last_update", "poll_interval"}, "last_update + poll_interval <= " + System.currentTimeMillis(), null, null, null, null);
    	c.moveToFirst();
    	while (!c.isAfterLast()) {
    		subs.put(c.getString(c.getColumnIndex("subskey")),c.getString(c.getColumnIndex("class_name")));
    		c.moveToNext();
    	}
    	c.close();
    	return subs;
    }
    
    public synchronized long getSubscriptionDueTimeFor(String subskey) {
    	Cursor c = mDb.query(SUBSCRIPTIONS_TABLE, new String[] { "subskey", "last_update", "poll_interval" }, "subskey = '" + subskey + "'", null, null, null, null);
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		long retVal = c.getLong(c.getColumnIndex("last_update")) + c.getLong(c.getColumnIndex("poll_interval"));
    		c.close();
    		return retVal;
    	} else {
    		c.close();
    		return -1l;
    	}
    }
    
    public synchronized Cursor fetchDueSubscriptions() {
    	return mDb.rawQuery("select subskey,class_name,last_update,poll_interval from " + SUBSCRIPTIONS_TABLE + " where (last_update + poll_interval) < " + System.currentTimeMillis(), null);
    	//return mDb.query(SUBSCRIPTIONS_TABLE, new String[] { "subskey", "class_name", "last_update", "poll_interval"}, "(last_update + poll_interval) <= " + System.currentTimeMillis(), null, null, null, null);
    }
    
    public synchronized boolean subscriptionExists(String subskey) {
    	return mDb.query(SUBSCRIPTIONS_TABLE, new String[] { "subskey" }, "subskey='" + subskey + "'", null, null, null, null).getCount() > 0;
    }
    
    public synchronized Cursor fetchAllSubscriptions(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    	return mDb.query(SUBSCRIPTIONS_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
    }
    
    public synchronized Cursor fetchSubscription(long row_id,String[] projection) {
    	return mDb.query(SUBSCRIPTIONS_TABLE, projection, KEY_ROWID + "=" + row_id, null, null, null, null);
    }
    
    //Knowledge Base Functions
    
    public synchronized Tag getTag(long row_id) {
    	String tc, subclass, subvalue=null;
    	HashMap<String,String> retVal = new HashMap<String,String>();
    	Cursor c = mDb.query(TAGS_TABLE, new String[] { KEY_ROWID, "tag_classid", "subclass", "subvalue"}, KEY_ROWID+"="+row_id, null, null, null, null);
    	if (c.getCount() == 0) {
    		c.close();
    		return dw.new Tag(-1, "","","");
    	}
    	c.moveToFirst();
    	tc = this.getTagClass(c.getLong(c.getColumnIndex("tag_classid")));
    	subclass = c.getString(c.getColumnIndex("subclass"));
    	if (!c.isNull(c.getColumnIndex("subvalue"))) subvalue = c.getString(c.getColumnIndex("subvalue"));
    	c.close();
    	return dw.new Tag(row_id, tc, subclass, subvalue);
    }
    
    public synchronized String getDescription(long row_id) {
    	Cursor c = mDb.query(DESCRIPTIONS_TABLE, new String[] { KEY_ROWID, "description" }, KEY_ROWID+"="+row_id, null, null, null, null);
    	if (c.getCount() == 0) {
    		c.close();
    		return "";
    	}
    	c.moveToFirst();
    	String retVal = c.getString(c.getColumnIndex("description"));
    	c.close();
    	return retVal;
    }
    
    public synchronized String getTagClass(long row_id) {
    	Cursor c = mDb.query(TAG_CLASS_TABLE, new String[] { KEY_ROWID, "name"}, KEY_ROWID + "=" + row_id, null, null, null, null);
    	if (c.getCount() == 0) {
    		c.close();
    		return "";
    	}
    	c.moveToFirst();
    	String retVal = c.getString(c.getColumnIndex("name"));
    	c.close();
    	return retVal;
    }
    
    public synchronized Meta getMeta(long row_id) {
    	String name = "", value = null;
    	Cursor c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "name", "value"}, KEY_ROWID + "=" + row_id, null, null, null, null);
    	c.moveToFirst();
    	name = c.getString(c.getColumnIndex("name"));
    	if (!c.isNull(c.getColumnIndex("value"))) value = c.getString(c.getColumnIndex("value"));
    	c.close();
    	return dw.new Meta(row_id, name, value);
    }
    
    public synchronized Acond getAcond(long row_id) {
    	Long[] di = new Long[0], ti = new Long[0];
    	//get descriptions
    	Cursor c = mDb.query(DESCRIPTIONS_TABLE, new String[] { KEY_ROWID, "description", "acondsid" }, "acondsid=" + row_id, null, null, null, null);
    	if (c.getCount() > 0) {
	    	di = this.getIndicesFromCursor(c);
    	}
    	//get tags
    	c.close();
    	c = mDb.query(TAGS_TABLE, new String[] { KEY_ROWID, "acondsid"}, "acondsid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		ti = this.getIndicesFromCursor(c);
    	}
    	c.close();
    	return dw.new Acond(row_id, di, ti);
    }
    
    public synchronized Qcond getQcond(long row_id) {
    	int refNum = -1;
    	Long[] ti = new Long[0];
    	//get refnum
    	Cursor c = mDb.query(QCOND_TABLE, new String[] {KEY_ROWID,"refnum"}, KEY_ROWID + "=" + row_id, null, null, null, null);
    	if (c.getCount() > 0) {	
    		c.moveToFirst();
    		refNum = c.getInt(c.getColumnIndex("refnum"));
    	}
    	//get tags
    	c.close();
    	c = mDb.query(TAGS_TABLE, new String[] { KEY_ROWID, "qcondsid"}, "qcondsid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		ti = this.getIndicesFromCursor(c);
    	}
    	c.close();
    	return (refNum == -1 && ti.length == 0 ? dw.new Qcond(row_id) : dw.new Qcond(row_id, refNum, ti));
    }
    
    public synchronized Fact getFact(long row_id) {
    	String timestamp = "", dayOfWeek = "", queried = "", persistence = "";
    	Long[] mi = new Long[0], ti = new Long[0];
    	
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID, "timestamp", "dayOfWeek", "queried", "persistence"}, KEY_ROWID+"="+row_id, null, null, null, null);
    	if (c.getCount() <= 0) {
    		c.close();
    		return dw.new Fact();
    	}
    	//get timestamp
    	c.moveToFirst();
    	timestamp = c.getString(c.getColumnIndex("timestamp"));
    	dayOfWeek = c.getString(c.getColumnIndex("dayOfWeek"));
    	queried = c.getString(c.getColumnIndex("queried"));
    	persistence = c.getString(c.getColumnIndex("persistence"));
    	
    	//get tags
    	c.close();
    	c = mDb.query(TAGS_TABLE, new String[] { KEY_ROWID, "factsid"}, "factsid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		ti = this.getIndicesFromCursor(c);
    	}
    	//get metas
    	c.close();
    	c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "factsid"}, "factsid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		mi = this.getIndicesFromCursor(c);
    	}
    	c.close();
    	return dw.new Fact(row_id, timestamp, dayOfWeek, mi, ti, queried, persistence);
    }
    
    public synchronized QAT getQAT(long row_id) {
    	String qtext = "";
    	long acond_id = -1;
    	Long[] qi = new Long[0], mi = new Long[0];
    	
    	Cursor c = mDb.query(QAT_TABLE, new String[] { KEY_ROWID, "qtext"}, KEY_ROWID + "=" + row_id, null,null,null,null,null);
    	if (c.getCount() <= 0) {
    		c.close();
    		return dw.new QAT();
    	}
    	c.moveToFirst();
    	qtext = c.getString(c.getColumnIndex("qtext"));
    	//get acond
    	c.close();
    	c = mDb.query(ACOND_TABLE, new String[] {KEY_ROWID, "qatid"}, "qatid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		acond_id = c.getLong(c.getColumnIndex("qatid"));
    	}
    	//get qconds
    	c.close();
    	c = mDb.query(QCOND_TABLE, new String[] { KEY_ROWID, "qatid"}, "qatid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		qi = this.getIndicesFromCursor(c);
    	}
    	//get metas
    	c.close();
    	c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "qatid"}, "qatid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		mi = this.getIndicesFromCursor(c);
    	}
    	c.close();
    	return dw.new QAT(row_id, qtext, acond_id, qi, mi);
    }
    
    public synchronized Long[] getAllQATs() {
    	Cursor c = mDb.query(QAT_TABLE, new String[] { KEY_ROWID }, null, null, null, null, null);
    	if (c.getCount() <= 0) {
    		c.close();
    		return new Long[0];
    	}
    	Long[] retVal = this.getIndicesFromCursor(c);
    	c.close();
    	return retVal;
    }
    
    public synchronized Long[] getAllFacts() {
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID }, null, null, null, null, null);
    	if (c.getCount() <= 0) {
    		c.close();
    		return new Long[0];
    	}
    	Long[] retVal = this.getIndicesFromCursor(c);
    	c.close();
    	return retVal;
    }
    
    /**
     * Should also make memory efficient version that just has references to complex types (e.g. tags, etc)
     * @param fact_id
     * @return
     */
    public synchronized HashMap<String,Object> getExpandedFact(long fact_id) {
		HashMap<String,Object> retVal = new HashMap<String,Object>();
		//get timestamp
		Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID, "timestamp", "dayOfWeek" }, KEY_ROWID + "=" + fact_id, null, null, null, null);
		if (c.getCount() != 1) return null;
		
		c.moveToFirst();
		for (int i = 1; i < c.getColumnCount(); i++) {
			retVal.put(c.getColumnName(i), c.getString(i));
		}
		//get tags
		c = mDb.query(TAGS_TABLE, new String[] {KEY_ROWID, "tag_classid", "subclass", "subvalue", "factsid"}, "factsid"+"="+fact_id, null, null, null, null);
		ArrayList<HashMap<String,String>> tags = new ArrayList<HashMap<String,String>>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			HashMap<String,String> tag = new HashMap<String,String>();
			//first get tag_classes
			Cursor sub = mDb.query(TAG_CLASS_TABLE, new String[] {KEY_ROWID,"name"}, KEY_ROWID+"="+c.getLong(c.getColumnIndex("tag_classid")), null, null, null, null);
			if (sub.getCount() <= 0) continue;
			sub.moveToFirst();
			tag.put("class", sub.getString(sub.getColumnIndex("name")));
			tag.put("subclass", c.getString(c.getColumnIndex("subclass")));
			if (!c.isNull(c.getColumnIndex("subvalue"))) tag.put("subvalue", c.getString(c.getColumnIndex("subvalue")));
			tags.add(tag);
			c.moveToNext();
		}
		retVal.put("tags", tags);
		//get metas
		c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "name", "value", "factsid" }, "factsid"+"="+fact_id, null, null, null, null);
		/*
		 * "create table " + META_TABLE + " (_id integer primary key autoincrement, "
			+ "name string NOT NULL, value string,"
			+ "factsid integer references " + FACTS_TABLE + "(id)," 
			+ "qatid integer references " + QAT_TABLE + "(id));";
		 */
		ArrayList<HashMap<String,String>> metas = new ArrayList<HashMap<String,String>>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			HashMap<String,String> meta = new HashMap<String,String>();
			meta.put("name", c.getString(c.getColumnIndex("name")));
			if (!c.isNull(c.getColumnIndex("value"))) meta.put("value", c.getString(c.getColumnIndex("value")));
			metas.add(meta);
			c.moveToNext();
		}
		retVal.put("metas", metas);
		return retVal;
	}
    
    /**
     * Should also make memory efficient version that just has ids to complex types (e.g. acond, tags etc.)
     * @param qat_id
     * @return
     */
    public synchronized HashMap<String,Object> getExpandedQAT(long qat_id) {
    	//Log.d("KBDbAdapter", "Sup, getQAT");
    	HashMap<String,Object> retVal = new HashMap<String,Object>();
    	//get QText
    	/*"create table " + QAT_TABLE + " (_id integer primary key autoincrement, qtext string NOT NULL);";*/
    	Cursor c = mDb.query(QAT_TABLE, new String[] {KEY_ROWID,"qtext"}, KEY_ROWID+"="+qat_id, null, null, null, null);
    	if (c.getCount() != 1) {
    		//Log.d("KBDbAdapter", "Can't find QAT with id " + qat_id);
    		return null;
    	}
    	c.moveToFirst();
    	retVal.put("qtext", c.getString(c.getColumnIndex("qtext")));
    	//get Acond
    	c.close();
    	c = mDb.query(ACOND_TABLE, new String[] { KEY_ROWID, "qatid"}, "qatid="+qat_id, null, null, null, null);
    	if (c.getCount() != 1) {
    		System.out.print("QAT with id " + qat_id + " has " + c.getCount() + " Aconds associated with it, with ids: ");
    		c.moveToFirst();
    		while (!c.isAfterLast()) {
    			System.out.print(c.getLong(c.getColumnIndex(KEY_ROWID)) + ",");
    			c.moveToNext();
    		}
    		//Log.d("KBDbAdapter", "");
    		return null;
    	}
    	c.moveToFirst();
    	HashMap<String,Object> acond = new HashMap<String,Object>();
    	//get tags & descriptions
    	Cursor tcursor = mDb.query(TAGS_TABLE, new String[] {KEY_ROWID, "tag_classid", "subclass", "subvalue", "acondsid"}, "acondsid"+"="+c.getLong(c.getColumnIndex(KEY_ROWID)), null, null, null, null);
    	ArrayList<HashMap<String,String>> tags = new ArrayList<HashMap<String,String>>();
    	tcursor.moveToFirst();
    	while (!tcursor.isAfterLast()) {
    		HashMap<String,String> tag = new HashMap<String,String>();
			//first get tag_classes
			Cursor sub = mDb.query(TAG_CLASS_TABLE, new String[] {KEY_ROWID,"name"}, KEY_ROWID+"="+tcursor.getLong(tcursor.getColumnIndex("tag_classid")), null, null, null, null);
			if (sub.getCount() <= 0) continue;
			sub.moveToFirst();
			tag.put("class", sub.getString(sub.getColumnIndex("name")));
			tag.put("subclass", tcursor.getString(tcursor.getColumnIndex("subclass")));
			if (!tcursor.isNull(tcursor.getColumnIndex("subvalue"))) tag.put("subvalue", tcursor.getString(tcursor.getColumnIndex("subvalue")));
			tags.add(tag);
			tcursor.moveToNext();
			sub.close();
    	}
    	acond.put("tags", tags);
    	//get descriptions
    	tcursor.close();
    	tcursor = mDb.query(DESCRIPTIONS_TABLE, new String[] {KEY_ROWID,"description","acondsid"}, "acondsid="+c.getLong(c.getColumnIndex(KEY_ROWID)), null, null, null, null);
    	ArrayList<String> descs = new ArrayList<String>();
    	tcursor.moveToFirst();
    	while (!tcursor.isAfterLast()) {
    		descs.add(tcursor.getString(tcursor.getColumnIndex("description")));
    		tcursor.moveToNext();
    	}
    	tcursor.close();
    	acond.put("descriptions", descs);
    	retVal.put("acond", acond);
    	//get Qconds
    	c.close();
    	c = mDb.query(QCOND_TABLE, new String[] { KEY_ROWID, "refnum", "qatid"}, "qatid="+qat_id, null, null, null, null);
    	ArrayList<HashMap<String,Object>> qconds = new ArrayList<HashMap<String,Object>>();
    	c.moveToFirst();
    	while (!c.isAfterLast()) {
    		HashMap<String,Object> qcond = new HashMap<String,Object>();
    		qcond.put("refnum", c.getInt(c.getColumnIndex("refnum")));
    		//get tags
    		tcursor = mDb.query(TAGS_TABLE, new String[] {KEY_ROWID, "tag_classid", "subclass", "subvalue", "qcondsid"}, "qcondsid"+"="+c.getLong(c.getColumnIndex(KEY_ROWID)), null, null, null, null);
        	ArrayList<HashMap<String,String>> qtags = new ArrayList<HashMap<String,String>>();
        	tcursor.moveToFirst();
        	while (!tcursor.isAfterLast()) {
        		HashMap<String,String> tag = new HashMap<String,String>();
    			//first get tag_classes
    			Cursor sub = mDb.query(TAG_CLASS_TABLE, new String[] {KEY_ROWID,"name"}, KEY_ROWID+"="+tcursor.getLong(tcursor.getColumnIndex("tag_classid")), null, null, null, null);
    			if (sub.getCount() <= 0) continue;
    			sub.moveToFirst();
    			tag.put("class", sub.getString(sub.getColumnIndex("name")));
    			tag.put("subclass", tcursor.getString(tcursor.getColumnIndex("subclass")));
    			if (!tcursor.isNull(tcursor.getColumnIndex("subvalue"))) tag.put("subvalue", tcursor.getString(tcursor.getColumnIndex("subvalue")));
    			qtags.add(tag);
    			tcursor.moveToNext();
        	}
        	tcursor.close();
        	qcond.put("tags", qtags);
        	qconds.add(qcond);
        	c.moveToNext();
    	}
    	retVal.put("qconds", qconds);
    	//get MetaInfo
    	c.close();
    	c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "name", "value", "qatid" }, "qatid"+"="+qat_id, null, null, null, null);
    	ArrayList<HashMap<String,String>> metas = new ArrayList<HashMap<String,String>>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			HashMap<String,String> meta = new HashMap<String,String>();
			meta.put("name", c.getString(c.getColumnIndex("name")));
			if (!c.isNull(c.getColumnIndex("value"))) meta.put("value", c.getString(c.getColumnIndex("value")));
			metas.add(meta);
			c.moveToNext();
		}
		//Log.d("KBDbAdapter", "Exiting getQat");
		retVal.put("metas", metas);
		c.close();
    	return retVal;
    }
    
    /**
     * 
     * @param ellapsedTimeMillis
     * @param timeString
     * @param tags
     * @return
     */
    public Long[] getFilteredFacts(long ellapsedTimeMillis, String timeString, String[] tags, String persistence) {
    	Long[] time_filtered = null, tag_filtered = null;
    	if (ellapsedTimeMillis >= 0)
    		time_filtered = this.getFilteredFactsByTime(ellapsedTimeMillis, timeString, persistence);
    	if (tags != null)
    		tag_filtered = this.getFilteredFactsByTag(tags);
    	
    	if (time_filtered == null && tag_filtered == null) return new Long[0];
    	else if (time_filtered == null) return tag_filtered;
    	else if (tag_filtered == null) return time_filtered;
    	else return UtilityFuncs.getIntersection(time_filtered, tag_filtered);
    }
    
    /**
     * 
     * @param columns
     * @param selection
     * @param selectionArgs
     * @return
     */
    public synchronized Long[] getFilteredFacts(String[] columns, String selection, String[] selectionArgs) {
    	Cursor c = mDb.query(FACTS_TABLE, columns, selection, selectionArgs, null, null, null);
    	if (c.getCount() <= 0) {
    		c.close();
    		return new Long[0];
    	}
    	Long[] retVal = this.getIndicesFromCursor(c);
    	c.close();
    	return retVal;
    }
    
    /**
     * Gets all facts within ellapsedTimeMillis of the given timeString or the current time
     * Centers the pivotTime in the interval
     * @param ellapsedTimeMillis
     * @param timeString formatted as: YYYY-MM-DDHH:mm:ss or * to use current time
     * @return
     */
    public Long[] getFilteredFactsByTimeCentered(long ellapsedTimeMillis,String timeString) {
    	long seconds = ellapsedTimeMillis/1000;
    	String pivotTime = (timeString.equalsIgnoreCase("*") ? "'now'" : "'" + timeString + "'");
    	String query = "timestamp between datetime(" + pivotTime + ",'-" + seconds + " seconds') and datetime(" + pivotTime + ") or timestamp between datetime(" + pivotTime + ") and datetime(" + pivotTime + ",'+" + seconds + " seconds')";
    	//Log.d("KBDbAdapter", query);
    	return getFilteredFacts(new String[] { KEY_ROWID, "timestamp"}, query, null);
    }
    
    /**
     * Gets all facts before ellapsedTimeMillis of the given timeString or the current time
     * Centers the pivotTime in the interval
     * @param ellapsedTimeMillis
     * @param timeString formatted as: YYYY-MM-DDHH:mm:ss or * to use current time
     * @return
     */
    public Long[] getFilteredFactsByTime(long ellapsedTimeMillis,String timeString, String persistence) {
    	long seconds = ellapsedTimeMillis/1000;
    	String pivotTime = (timeString.equalsIgnoreCase("*") ? "'now'" : "'" + timeString + "'");
    	String query = "timestamp between datetime(" + pivotTime + ",'-" + seconds + " seconds') and datetime(" + pivotTime + ") AND " +
    			"persistence = '" + persistence + "'";
    	//Log.d("KBDbAdapter", query);
    	return getFilteredFacts(new String[] { KEY_ROWID, "timestamp"}, query, null);
    }
    
    public Long[] getUnqueriedFilteredFactsByTime(long ellapsedTimeMillis,String timeString, String persistence) {
    	long seconds = ellapsedTimeMillis/1000;
    	String pivotTime = (timeString.equalsIgnoreCase("*") ? "'now'" : "'" + timeString + "'");
    	String query = "timestamp between datetime(" + pivotTime + ",'-" + seconds 
    			+ " seconds') and datetime(" + pivotTime + ") and " 
    			+ " queried = 'false' and " 
    			+ "persistence = '" + persistence + "'";
    	//Log.d("KBDbAdapter", query);
    	return getFilteredFacts(new String[] { KEY_ROWID, "timestamp"}, query, null);
    }
    
    public synchronized Long[] getPersistentFacts() {
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID, "persistence" }, "persistence = 'persistent'", 
    			null, null, null, null);
    	ArrayList<Long> retVal = new ArrayList<Long>();
    	if (c != null) {
    		if (c.getCount() > 0) {
    			c.moveToFirst();
    			while (!c.isAfterLast()) {
    				retVal.add(c.getLong(c.getColumnIndex(KEY_ROWID)));
    				c.moveToNext();
    			}
    		}
    		c.close();
    	}
    	return retVal.toArray(new Long[retVal.size()]);
    }
    
    public synchronized Long[] getUnqueriedFacts() {
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID, "queried"}, 
    			"queried = 'false'", null, null, null, null);
    	ArrayList<Long> retVal = new ArrayList<Long>();
    	if (c != null) {
    		if (c.getCount() > 0) {
    			c.moveToFirst();
    			while (!c.isAfterLast()) {
    				retVal.add(c.getLong(c.getColumnIndex(KEY_ROWID)));
    				c.moveToNext();
    			}
    		}
    		c.close();
    	}
    	return retVal.toArray(new Long[retVal.size()]);
    }
    /**
     * Gets all facts based on presence of a tag
     * Tags should be formatted as class_name:subclass_name
     * If any subclass is permissible, just enter the class_name
     * @param tags
     * @return
     */
    public synchronized Long[] getFilteredFactsByTag(String[] tags) {
    	//Log.d("KBDbAdapter", "Entering get filtered facts by tag:");
    	String[] helper = tags[0].split(":");
    	StringBuffer query = new StringBuffer("select distinct facts._id, tag_class.name from facts,tags,tag_class where tags.tag_classid = tag_class._id and facts._id = tags.factsid and ((tag_class.name = '" + helper[0] + "'" + (helper.length > 1 ? " and tags.subclass = '" + helper[1] + "'": "") + ") ");   	
    	for (int i = 1; i < tags.length; i++) {
    		helper = tags[i].split(":");
    		query.append("or (tag_class.name = '" + helper[0] + "'" + (helper.length > 1 ? " and tags.subclass = '" + helper[1] + "'" : "") + ") ");
    	}
    	query.append(");");
    	//Log.d("KBDbAdapter", query.toString());
    	Cursor c = mDb.rawQuery(query.toString(), null);
    	Long[] retVal = this.getIndicesFromCursor(c);
    	c.close();
    	return retVal;
    }
    
    /**
     * Gets all facts on a certain weekday
     * @param weekday
     * @return
     */
    public Long[] getFilteredFactsByWeekday(String weekday) {
    	return getFilteredFacts(new String[] { KEY_ROWID, "dayOfWeek"}, "dayOfWeek="+weekday,null);
    }
    
    /**
     * 
     * @param metas
     * @param tags
     * @return
     */
    public Long[] getFilteredQats(String[] metas, String[] tags) {
    	Long[] meta_filtered = null, tag_filtered = null;
    	if (metas != null) meta_filtered = this.getFilteredQatsByMetas(metas);
    	if (tags != null) tag_filtered = this.getFilteredQatsByTag(tags);
    	
    	if (meta_filtered == null && tag_filtered == null) return new Long[0];
    	else if (meta_filtered == null) return tag_filtered;
    	else if (tag_filtered == null) return meta_filtered;
    	else return UtilityFuncs.getIntersection(meta_filtered, tag_filtered);
    }
    
    /**
     * Gets all facts based on presence of a tag
     * Tags should be formatted as class_name:subclass_name
     * If any subclass is permissible, just enter the class_name
     * @param tags
     * @return
     */
    public synchronized Long[] getFilteredQatsByTag(String[] tags) {
    	//Log.d("KBDbAdapter", "Entering get filtered qats by tag:");
    	String[] helper = tags[0].split(":");
    	StringBuffer query = new StringBuffer("select distinct qats._id from qats,qconds,tags,tag_class where qconds.qatid = qats._id and tags.qcondsid = qconds._id and tags.tag_classid = tag_class._id and ((tag_class.name = '" + helper[0] + "'" + (helper.length > 1 ? " and tags.subclass = '" + helper[1] + "'": "") + ") ");
    	for (int i = 1; i < tags.length; i++) {
    		helper = tags[i].split(":");
    		query.append("or (tag_class.name = '" + helper[0] + "'" + (helper.length > 1 ? " and tags.subclass = '" + helper[1] + "'" : "") + ") ");
    	}
    	query.append(");");
    	//Log.d("KBDbAdapter", query.toString());
    	Cursor c = mDb.rawQuery(query.toString(), null);
    	Long[] qconds_matches = this.getIndicesFromCursor(c);
    	
    	helper = tags[0].split(":");
    	query = new StringBuffer("select distinct qats._id from qats,aconds,tags,tag_class where aconds.qatid = qats._id and tags.acondsid = aconds._id and tags.tag_classid = tag_class._id and ((tag_class.name = '" + helper[0] + "'" + (helper.length > 1 ? " and tags.subclass = '" + helper[1] + "'": "") + ") ");
    	for (int i = 1; i < tags.length; i++) {
    		helper = tags[i].split(":");
    		query.append("or (tag_class.name = '" + helper[0] + "'" + (helper.length > 1 ? " and tags.subclass = '" + helper[1] + "'" : "") + ") ");
    	}
    	query.append(");");
    	//Log.d("KBDbAdapter", query.toString());
    	c.close();
    	c = mDb.rawQuery(query.toString(), null);
    	Long[] aconds_matches = this.getIndicesFromCursor(c);
    	c.close();
    	return UtilityFuncs.getUnion(qconds_matches, aconds_matches);
    }
    
    /**
     * 
     * @param metas
     * @return
     */
    public synchronized Long[] getFilteredQatsByMetas(String[] metas) {
    	String[] helper = metas[0].split(":");
    	StringBuffer query = new StringBuffer("select distinct qats._id from qats,metas where metas.qatid = qats._id and ((metas.name = '" + helper[0] + "'" + (helper.length > 1 ? " and metas.value = '" + helper[1] + "'": "") + ") ");   	
    	for (int i = 1; i < metas.length; i++) {
    		helper = metas[i].split(":");
    		query.append("or (metas.name = '" + helper[0] + "'" + (helper.length > 1 ? " and metas.value = '" + helper[1] + "'" : "") + ") ");
    	}
    	query.append(");");
    	//Log.d("KBDbAdapter", query.toString());
    	Cursor c = mDb.rawQuery(query.toString(), null);
    	Long[] retVal = this.getIndicesFromCursor(c);
    	c.close();
    	return retVal;
    }
    
    /**
     * Pre-requisite is that c contains column KEY_ROWID
     * @param c
     * @return
     */
    private Long[] getIndicesFromCursor(Cursor c) {
    	Long[] indices = new Long[c.getCount()];
    	int counter = 0;
    	c.moveToFirst();
    	while (!c.isAfterLast()) {
    		indices[counter++] = c.getLong(c.getColumnIndex(KEY_ROWID));
    		c.moveToNext();
    	}
    	return indices;
    }
}
