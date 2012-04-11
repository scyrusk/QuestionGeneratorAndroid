package com.cmuchimps.myauth;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class KBDbAdapter {
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
			+ "dayOfWeek string);";
	
	
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
	
	private static final String[] TABLES = {TAG_CLASS_TABLE, FACTS_TABLE, META_TABLE, DESCRIPTIONS_TABLE, 
		QCOND_TABLE, ACOND_TABLE, QAT_TABLE, TAGS_TABLE};
	private static final String[] TABLE_CREATION = {TAG_CLASS_CREATE, QAT_CREATE, META_CREATE, FACTS_CREATE,
		QCOND_CREATE, ACOND_CREATE, DESCRIPTIONS_CREATE, TAGS_CREATE};
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			// TODO Auto-generated constructor stub
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			for (String s : TABLE_CREATION) {
				System.out.println(s);
				db.execSQL(s);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
			for (String s : TABLES) {
				db.execSQL("DROP TABLE IF EXISTS " + TABLES);
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
    public long createTagClass(String name) {
    	ContentValues initialValues = new ContentValues();
    	initialValues.put("name", name); //put the name variable in the "name" column
    	return mDb.insert(TAG_CLASS_TABLE, null, initialValues);
    }

    /**
     * 
     * @param name
     * @return
     */
    public long findTagClassByName(String name) {
    	Cursor c = mDb.query(TAG_CLASS_TABLE, new String[] {KEY_ROWID, "name"}, "name='"+name+"'", null, null, null, null);
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		return c.getLong(c.getColumnIndex(KEY_ROWID));
    	}
    	return -1;
    }
    
    /**
     * 
     * @param rowId
     * @return
     */
    public boolean deleteTagClass(long rowId) {
    	return mDb.delete(TAG_CLASS_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * 
     * @param rowId
     * @param name
     * @return
     */
    public boolean updateTagClass(long rowId, String name) {
    	ContentValues args = new ContentValues();
    	args.put("name", name);
    	
    	return mDb.update(TAG_CLASS_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * 
     * @return
     */
    public Cursor fetchAllTagClasses() {
    	return mDb.query(TAG_CLASS_TABLE, new String[] {KEY_ROWID, "name"}, null, null, null, null, null);
    }
    
    public long createFact(String timestamp, String dayOfWeek, ArrayList<HashMap<String,String>> tags, ArrayList<HashMap<String,String>> metas) {
    	/* Insert fact */
    	ContentValues fact_vals = new ContentValues();
    	fact_vals.put("timestamp", UtilityFuncs.join(timestamp.split("T"), ""));
    	fact_vals.put("dayOfWeek", dayOfWeek);
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
     * 
     * @param rowId
     * @return
     */
    public boolean deleteFact(long rowId) {
    	//TODO: delete all tags/metas associated with this fact
    	mDb.delete(TAGS_TABLE, "factsid=" + rowId, null);
    	mDb.delete(META_TABLE, "factsid=" + rowId, null);
    	
    	return mDb.delete(FACTS_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * 
     * @return
     */
    public Cursor fetchAllFacts() {
    	return mDb.query(FACTS_TABLE, new String[] {}, null, null, null, null, null);
    }
    
    /**
     * To create a full QAT, must write a wrapper function that first creates QAT and saves rowId
     * Then, create Aconds and Qconds and pass in the rowId of QAT.
     * @param qtext
     * @param metas
     * @return
     */
    public long createQAT(String qtext, ArrayList<HashMap<String,String>> metas) {
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
    public boolean deleteQAT(long rowId) {
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
    	
    	mDb.delete(META_TABLE, "factsid=" + rowId, null);
    	return mDb.delete(QAT_TABLE, KEY_ROWID+"="+rowId, null) > 0;
    }
    
    /**
     * 
     * @return
     */
    public Cursor fetchAllQATS() {
    	return null;
    }
    
    /**
     * 
     * @param qatid
     * @param descs
     * @param tags
     * @return
     */
    public long createAcond(long qatid, ArrayList<String> descs, ArrayList<HashMap<String,String>> tags) {
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
    public int deleteAcond(long rowId) {
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
    public long createQcond(long qatid, int refnum, ArrayList<HashMap<String,String>> tags) {
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
    public long deleteQcond(long rowId) {
    	//TODO delete all tags referring to Qcond, then delete Qcond
    	mDb.delete(TAGS_TABLE, "qcondsid="+rowId, null);
    	return mDb.delete(QCOND_TABLE, KEY_ROWID+"="+rowId, null);
    }
    
    
    //Knowledge Base Functions
    
    /**
     * Should also make memory efficient version that just has references to complex types (e.g. tags, etc)
     * @param fact_id
     * @return
     */
    public HashMap<String,Object> getFact(long fact_id) {
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
    public HashMap<String,Object> getQAT(long qat_id) {
    	System.out.println("Sup, getQAT");
    	HashMap<String,Object> retVal = new HashMap<String,Object>();
    	//get QText
    	/*"create table " + QAT_TABLE + " (_id integer primary key autoincrement, qtext string NOT NULL);";*/
    	Cursor c = mDb.query(QAT_TABLE, new String[] {KEY_ROWID,"qtext"}, KEY_ROWID+"="+qat_id, null, null, null, null);
    	if (c.getCount() != 1) {
    		System.out.println("Can't find QAT with id " + qat_id);
    		return null;
    	}
    	c.moveToFirst();
    	retVal.put("qtext", c.getString(c.getColumnIndex("qtext")));
    	//get Acond
    	c = mDb.query(ACOND_TABLE, new String[] { KEY_ROWID, "qatid"}, "qatid="+qat_id, null, null, null, null);
    	if (c.getCount() != 1) {
    		System.out.print("QAT with id " + qat_id + " has " + c.getCount() + " Aconds associated with it, with ids: ");
    		c.moveToFirst();
    		while (!c.isAfterLast()) {
    			System.out.print(c.getLong(c.getColumnIndex(KEY_ROWID)) + ",");
    			c.moveToNext();
    		}
    		System.out.println();
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
    	}
    	acond.put("tags", tags);
    	//get descriptions
    	tcursor = mDb.query(DESCRIPTIONS_TABLE, new String[] {KEY_ROWID,"description","acondsid"}, "acondsid="+c.getLong(c.getColumnIndex(KEY_ROWID)), null, null, null, null);
    	ArrayList<String> descs = new ArrayList<String>();
    	tcursor.moveToFirst();
    	while (!tcursor.isAfterLast()) {
    		descs.add(tcursor.getString(tcursor.getColumnIndex("description")));
    		tcursor.moveToNext();
    	}
    	acond.put("descriptions", descs);
    	retVal.put("acond", acond);
    	//get Qconds
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
        	qcond.put("tags", qtags);
        	qconds.add(qcond);
        	c.moveToNext();
    	}
    	retVal.put("qconds", qconds);
    	//get MetaInfo
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
		System.out.println("Exiting getQat");
		retVal.put("metas", metas);
    	return retVal;
    }
    
    public HashMap<String,String> getTag(long row_id) {
    	HashMap<String,String> retVal = new HashMap<String,String>();
    	Cursor c = mDb.query(TAGS_TABLE, new String[] { KEY_ROWID, "tag_classid", "subclass", "subvalue"}, KEY_ROWID+"="+row_id, null, null, null, null);
    	if (c.getCount() == 0) return retVal;
    	c.moveToFirst();
    	retVal.put("class", this.getTagClass(c.getLong(c.getColumnIndex("tag_classid"))));
    	retVal.put("subclass", c.getString(c.getColumnIndex("subclass")));
    	if (!c.isNull(c.getColumnIndex("subvalue"))) retVal.put("subvalue", c.getString(c.getColumnIndex("subvalue")));
    	return retVal;
    }
    
    public String getDescription(long row_id) {
    	Cursor c = mDb.query(DESCRIPTIONS_TABLE, new String[] { KEY_ROWID, "description" }, KEY_ROWID+"="+row_id, null, null, null, null);
    	if (c.getCount() == 0) return "";
    	c.moveToFirst();
    	return c.getString(c.getColumnIndex("descriptions"));
    }
    
    public String getTagClass(long row_id) {
    	Cursor c = mDb.query(TAG_CLASS_TABLE, new String[] { KEY_ROWID, "name"}, KEY_ROWID + "=" + row_id, null, null, null, null);
    	if (c.getCount() == 0) return "";
    	c.moveToFirst();
    	return c.getString(c.getColumnIndex("name"));
    }
    
    public HashMap<String,String> getMeta(long row_id) {
    	HashMap<String,String> retVal = new HashMap<String,String>();
    	Cursor c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "name", "value"}, KEY_ROWID + "=" + row_id, null, null, null, null);
    	c.moveToFirst();
    	retVal.put("name", c.getString(c.getColumnIndex("name")));
    	if (!c.isNull(c.getColumnIndex("value"))) retVal.put("value", c.getString(c.getColumnIndex("value")));
    	return retVal;
    }
    
    public HashMap<String,Object> getAcond(long row_id) {
    	HashMap<String,Object> retVal = new HashMap<String,Object>();
    	//get descriptions
    	Cursor c = mDb.query(DESCRIPTIONS_TABLE, new String[] { KEY_ROWID, "description", "acondsid" }, "acondsid=" + row_id, null, null, null, null);
    	if (c.getCount() > 0) {
	    	retVal.put("descriptions", this.getIndicesFromCursor(c));
    	}
    	//get tags
    	c = mDb.query(TAGS_TABLE, new String[] { KEY_ROWID, "acondsid"}, "acondsid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		retVal.put("tags", this.getIndicesFromCursor(c));
    	}
    	return retVal;
    }
    
    public HashMap<String,Object> getQcond(long row_id) {
    	HashMap<String,Object> retVal = new HashMap<String,Object>();
    	//get refnum
    	Cursor c = mDb.query(QCOND_TABLE, new String[] {KEY_ROWID,"refnum"}, KEY_ROWID + "=" + row_id, null, null, null, null);
    	retVal.put("refnum", c.getInt(c.getColumnIndex("refnum")));
    	//get tags
    	c = mDb.query(TAGS_TABLE, new String[] { KEY_ROWID, "acondsid"}, "acondsid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		retVal.put("tags", this.getIndicesFromCursor(c));
    	}
    	return retVal;
    }
    
    public HashMap<String,Object> getFactMemEff(long row_id) {
    	HashMap<String,Object> retVal = new HashMap<String,Object>();
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID, "timestamp", "dayOfWeek"}, KEY_ROWID+"="+row_id, null, null, null, null);
    	if (c.getCount() <= 0) return retVal;
    	//get timestamp
    	retVal.put("timestamp", c.getString(c.getColumnIndex("timestamp")));
    	retVal.put("dayOfWeek", c.getString(c.getColumnIndex("dayOfWeek")));
    	//get tags
    	c = mDb.query(TAGS_TABLE, new String[] { KEY_ROWID, "factsid"}, "factsid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		retVal.put("tags", this.getIndicesFromCursor(c));
    	}
    	//get metas
    	c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "factsid"}, "factsid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		retVal.put("metas", this.getIndicesFromCursor(c));
    	}
    	return retVal;
    }
    
    public HashMap<String,Object> getQATMemEff(long row_id) {
    	HashMap<String,Object> retVal = new HashMap<String,Object>();
    	Cursor c = mDb.query(QAT_TABLE, new String[] { KEY_ROWID, "qtext"}, KEY_ROWID + "=" + row_id, null,null,null,null,null);
    	if (c.getCount() <= 0) return retVal;
    	retVal.put("qtext", c.getString(c.getColumnIndex("qtext")));
    	//get acond
    	c = mDb.query(ACOND_TABLE, new String[] {KEY_ROWID, "qatid"}, "qatid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		retVal.put("acond", c.getLong(c.getColumnIndex("qatid")));
    	}
    	//get qconds
    	c = mDb.query(QCOND_TABLE, new String[] { KEY_ROWID, "qatid"}, "qatid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		retVal.put("qconds", this.getIndicesFromCursor(c));
    	}
    	//get metas
    	c = mDb.query(META_TABLE, new String[] { KEY_ROWID, "qatid"}, "qatid="+row_id, null, null, null, null);
    	if (c.getCount() > 0) {
    		retVal.put("metas", this.getIndicesFromCursor(c));
    	}
    	return retVal;
    }
    
    public Long[] getAllQATs() {
    	Cursor c = mDb.query(QAT_TABLE, new String[] { KEY_ROWID }, null, null, null, null, null);
    	if (c.getCount() <= 0) return new Long[0];
    	return this.getIndicesFromCursor(c);
    }
    
    public Long[] getAllFacts() {
    	Cursor c = mDb.query(FACTS_TABLE, new String[] { KEY_ROWID }, null, null, null, null, null);
    	if (c.getCount() <= 0) return new Long[0];
    	return this.getIndicesFromCursor(c);
    }
    
    /**
     * 
     * @param columns
     * @param selection
     * @param selectionArgs
     * @return
     */
    public Long[] getFilteredFacts(String[] columns, String selection, String[] selectionArgs) {
    	Cursor c = mDb.query(FACTS_TABLE, columns, selection, selectionArgs, null, null, null);
    	if (c.getCount() <= 0) return new Long[0];
    	return this.getIndicesFromCursor(c);
    }
    
    /**
     * Gets all facts within ellapsedTimeMillis of the given timeString or the current time
     * @param ellapsedTimeMillis
     * @param timeString formatted as: YYYY-MM-DDHH:mm:ss or * to use current time
     * @return
     */
    public Long[] getFilteredFactsByTime(long ellapsedTimeMillis,String timeString) {
    	long seconds = ellapsedTimeMillis/1000;
    	String pivotTime = (timeString.equalsIgnoreCase("*") ? "datetime('now')" : "datetime('" + timeString + "')");
    	return getFilteredFacts(new String[] { KEY_ROWID, "timestamp"}, "timestamp between ? and ? or timestamp between ? and ?", new String[] {"datetime('now','-" + seconds + " seconds'",pivotTime,pivotTime,"datetime('now','+" + seconds + " seconds'"});
    }
    
    public Long[] getFilteredFactsByWeekday(String weekday) {
    	return getFilteredFacts(new String[] { KEY_ROWID, "dayOfWeek"}, "dayOfWeek="+weekday,null);
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
    	}
    	return indices;
    }
    
}
