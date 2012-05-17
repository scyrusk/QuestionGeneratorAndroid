package com.cmuchimps.myauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cmuchimps.myauth.KnowledgeTranslatorWrapper.KnowledgeSubscription;
import com.cmuchimps.myauth.QuestionGenerator.QuestionAnswerPair;

public class MyAuthActivity extends Activity {
    /** Called when the activity is first created. */
	private KBXMLParser mParser;
	private KBDbAdapter mDbHelper;
	private DataWrapper dw;
	private QuestionGenerator qg;
	private KnowledgeTranslatorWrapper ktw;
	private int pollAllMenuId;
	
	public static final Random r = new Random();
	private QuestionAnswerPair currQ;
	
	private LocationManager lm;
	private LocationListener ll;
	private ConnectivityManager cm;
	
	private User mUser;
	private ServerCommunicator mCommunicator;
	
	//fields
	TextView output;
	Button submit;
	Button newq;
	Button pollAll;
	Button printFacts;
	
	//private final int seed;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        output = (TextView)this.findViewById(R.id.output);
        submit = (Button)this.findViewById(R.id.submit);
        newq = (Button)this.findViewById(R.id.newq);
        pollAll = (Button)this.findViewById(R.id.pollAll);
        printFacts = (Button)this.findViewById(R.id.printFacts);
        mCommunicator = new ServerCommunicator(this.getApplicationContext());
        
        final EditText input = (EditText)this.findViewById(R.id.input);

        submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currQ.matches(input.getText().toString())) {
					Toast.makeText(getApplicationContext(), "Correct Answer!", Toast.LENGTH_SHORT).show();
					input.setText("");
					askQuestion();
				} else {
					String[] answers = currQ.getAnswers().toArray(new String[currQ.getAnswers().size()]);
					Toast.makeText(getApplicationContext(), "Incorrect Answer! Acceptable answers were: " + UtilityFuncs.join(answers, ","), Toast.LENGTH_SHORT).show();
				}
			}
     
        });
        
        newq.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				input.setText("");
				askQuestion();
			}
        });
        
        pollAll.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View arg0) {
        		/*Cursor temp = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[] { Calls.DATE, Calls.NUMBER, Calls.DURATION, Calls.TYPE, Calls.CACHED_NAME}, null, null, null);
        		if (temp != null && temp.getCount() > 0) {
        			temp.moveToFirst();
        			while (!temp.isAfterLast()) {
        				System.out.println("Date in millis: " + temp.getString(temp.getColumnIndex(Calls.DATE)));
        				temp.moveToNext();
        			}
        		}*/
        		forcePollSubscriptions();
        	}
        });
        
        printFacts.setOnClickListener(new OnClickListener() {
        	public void onClick(View arg0) {
        		printFacts(mDbHelper.getAllFacts());
        	}
        });
        
        //this.deleteDB();
        this.mDbHelper = new KBDbAdapter(this);
        this.mDbHelper.open();
        //this.repopulateDB();
        this.dw = new DataWrapper(mDbHelper);
        this.qg = new QuestionGenerator(mDbHelper,dw);
        this.lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        this.ktw = new KnowledgeTranslatorWrapper();
        this.setUpdaterAlarm();
        this.initializeLocationListener();
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        //this.deleteUser();
        /*System.out.println("Testing QAT-Fact");
        ArrayList<Fact> testFacts = new ArrayList<Fact>();
        testFacts.add(mDbHelper.getFact(64l));
        System.out.println("QText := " + mDbHelper.getQAT(6l).getQText());
        this.qg.testQATagainstFacts(mDbHelper.getQAT(6l), testFacts);
        System.out.println("Ending QAT-Fact test");*/
        //this.forcePollSubscriptions();
        //UtilityFuncs.TestingFLEXJson(this.getFilesDir());
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (mUser == null && !User.exists(getFilesDir())) {
    		startActivity(new Intent(this, NewUserActivity.class));
    	} else {
    		try {
				mUser = User.load(getFilesDir());
				System.out.println("User:");
				System.out.println(mUser);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	this.setUpdaterAlarm();
    	if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
		} else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
		}
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	if (mUser == null && !User.exists(getFilesDir())) {
    		startActivity(new Intent(this, NewUserActivity.class));
    	} else {
    		try {
				mUser = User.load(getFilesDir());
				System.out.println("User:");
				System.out.println(mUser);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
		} else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
		}
    }
    
    @Override
    public void onPause() {
    	lm.removeUpdates(ll);
    	super.onPause();
    }
    
    @Override
    public void onStop() {
    	lm.removeUpdates(ll);
    	super.onStop();
    }
    
    private void deleteUser() {
    	File f = new File(getFilesDir(),User.USER_FILE);
    	f.delete();
    }
    
    private void initializeLocationListener() {
		ll = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				//create fact about location here
				//2 tags: person:User was at location:Towers=
				//also get Timestamp and day of week
				System.out.println("Location changed event has triggered for some reason.");
				if (System.currentTimeMillis() > mDbHelper.getSubscriptionDueTimeFor("Location")) {
					System.out.println("adding location fact from location listener");
					String timestamp, dayOfWeek;
					ArrayList<HashMap<String,String>> tags = new ArrayList<HashMap<String,String>>(),metas = new ArrayList<HashMap<String,String>>();
					Date date = new Date(location.getTime());
					timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
					dayOfWeek = (String) DateFormat.format("EEEE", date);
					//add person:User tag
					HashMap<String,String> curr = new HashMap<String,String>();
					curr.put("tag_class", "Person");
					curr.put("subclass", "User");
					tags.add(curr);
					//add location:GPS tag
					curr = new HashMap<String,String>();
					curr.put("tag_class", "Location");
					curr.put("subclass", "Provider");
					curr.put("subvalue", location.getProvider());
					tags.add(curr);
					//add location:Geopoint tag
					curr = new HashMap<String,String>();
					curr.put("tag_class", "Location");
					curr.put("subclass", "Geopoint");
					curr.put("subvalue", location.getLatitude() + "," + location.getLongitude());
					tags.add(curr);
					//add location:Accuracy tag
					curr = new HashMap<String,String>();
					curr.put("tag_class","Location");
					curr.put("subclass", "Geopoint");
					curr.put("subvalue", ""+location.getAccuracy());
					tags.add(curr);
					//TODO: Should we do reverse geocoding here? Who knows.
					//no metas for now
					mDbHelper.createFact(timestamp, dayOfWeek, tags, metas);
					mDbHelper.updateSubscriptionTime("Location", location.getTime());
				}
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				
			}
			
		};
    }
    
    @Override
    public void onDestroy() {
    	this.mDbHelper.close();
    	super.onDestroy();
    }
    
    private void askQuestion() {
    	currQ = qg.askQuestion();
        if (currQ != null) {
	        output.setText(currQ.getQuestion());
        } else {
        	Toast.makeText(getApplicationContext(), "Could not create a question...", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void printFacts(Long[] facts) {
    	System.out.println("ALL FACTS:\n");
        for (Long l : facts) {
        	System.out.println(mDbHelper.getFact(l));
        }
    }
    
    private void printQATS(Long[] qats) {
        System.out.println("ALL QATS:\n");
        for (Long l : qats) { 
        	System.out.println(mDbHelper.getQAT(l));
        }
    }
    
    private void repopulateDB() {
        try {
			this.populateTagClasses(this.getAssets().open("qg_other_files/tag_classes.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        this.mParser = new KBXMLParser(this,mDbHelper);
        mParser.parseFactBase("qg_xml/knowledgebase.xml");
        mParser.parseQATs("qg_xml/qats.xml");
        createSensorSubscriptions();
    }
    
    private void deleteDB() {
    	this.deleteDatabase(mDbHelper.DATABASE_NAME);
	}	
    /**
     * 
     * @param file
     */
    private void populateTagClasses(InputStream file) {
    	try {
			BufferedReader br = new BufferedReader(new InputStreamReader(file));
			String input;
			while ((input = br.readLine()) != null) {
				if (this.mDbHelper == null) System.out.println("mDbHelper is null?");
				this.mDbHelper.createTagClass(input.replace("\n", ""));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * Sets updater alarm so that subscriptions are polled periodically.
     */
    private void setUpdaterAlarm() {
    	Intent updater = new Intent(this, SubscriptionReceiver.class);
    	PendingIntent recurringUpdate = PendingIntent.getBroadcast(getApplicationContext(), 0, updater, PendingIntent.FLAG_CANCEL_CURRENT);
    	AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    	//alarms.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * UtilityFuncs.MIN_TO_MILLIS, 10l*UtilityFuncs.MIN_TO_MILLIS, recurringUpdate);
    	alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * UtilityFuncs.MIN_TO_MILLIS, 10l*UtilityFuncs.MIN_TO_MILLIS, recurringUpdate);
    }
    
    /**
     * Mainly for testing purposes. Manually starts intent to update all due subscriptions.
     */
    private void pollDueSubscriptions() {
    	Intent updater = new Intent(this, KnowledgeTranslatorWrapper.class);
    	String[] dueSubs = mDbHelper.getAllDueSubscriptions().keySet().toArray(new String[mDbHelper.getAllDueSubscriptions().size()]);
    	updater.putExtra("dueSubs", dueSubs);
    	this.startService(updater);
    }
    
    /**
     * Mainly for testing purposes. Manually starts intent to update all subscriptions, regardless of whether or not they are due.
     */
    private void forcePollSubscriptions() {
    	System.out.println("Entering force poll subscriptions...");
    	/*Cursor c = mDbHelper.fetchAllSubscriptions(new String[] { "subskey", "class_name", "last_update", "poll_interval"}, null, null, null);
    	//System.out.println("Current time millis: " + System.currentTimeMillis());
    	if (c.getCount() > 0) {
    		c.moveToFirst();
    		while (!c.isAfterLast()) {
    			for (int i = 0; i < c.getColumnCount(); i++) {
    				System.out.print(c.getString(i) + ",");
    			}
    			System.out.println();
    			c.moveToNext();
    		}
    	}*/
    	Intent updater = new Intent(this, KnowledgeTranslatorWrapper.class);
    	String[] dueSubs = mDbHelper.getAllSubscriptions().keySet().toArray(new String[mDbHelper.getAllSubscriptions().size()]);
    	updater.putExtra("dueSubs", dueSubs);
    	this.startService(updater);
    }
    
    /**
     * Initializes subscriptions table.
     */
    private void createSensorSubscriptions() {
    	String[] SensorSubscriptions = new String[] { "Communication", "InternetBrowsing", "Media", "UserDictionary", "Contact", "Calendar", "ApplicationUse", "Location"};
    	Long[] PollIntervals = new Long[] { 3l*UtilityFuncs.HOUR_TO_MILLIS, 3l*UtilityFuncs.DAY_TO_MILLIS, 1l*UtilityFuncs.DAY_TO_MILLIS, 7l*UtilityFuncs.DAY_TO_MILLIS, 3l*UtilityFuncs.DAY_TO_MILLIS, 7l*UtilityFuncs.DAY_TO_MILLIS, 30l*UtilityFuncs.MIN_TO_MILLIS, 30l*UtilityFuncs.MIN_TO_MILLIS};
    	
    	for (int i = 0; i < SensorSubscriptions.length; i++) {
			if (!mDbHelper.subscriptionExists(SensorSubscriptions[i])) {
				//System.out.println(System.currentTimeMillis());
				mDbHelper.createSubscription(SensorSubscriptions[i], PollIntervals[i], System.currentTimeMillis() - (1*UtilityFuncs.DAY_TO_MILLIS), "com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + SensorSubscriptions[i] + "KnowledgeSubscription");
				try {
					Class c = Class.forName("com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + SensorSubscriptions[i] + "KnowledgeSubscription");
					if (!(Modifier.isStatic(c.getModifiers()) || Modifier.isAbstract(c.getModifiers()))) {
						KnowledgeSubscription ks = (KnowledgeSubscription) c.getDeclaredConstructor(new Class[] { KnowledgeTranslatorWrapper.class }).newInstance(new Object[] { this.ktw });
						c.getMethod("poll", null).invoke(ks, null);
						System.out.println("Successfully polled " + SensorSubscriptions[i] + "KnowledgeSubscription!");
					}
				} catch (Throwable e) {
					System.out.println("Failed to update class " + SensorSubscriptions[i]);
					e.printStackTrace();
				}
			}
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        	case (R.id.sendPackets):
        		Toast.makeText(getApplicationContext(), "Send Packets Selected", Toast.LENGTH_SHORT).show();
        		return true;
        	case (R.id.contact):
        		Toast.makeText(getApplicationContext(), "Contact Info Selected", Toast.LENGTH_SHORT).show();
        		return true;
        	case (R.id.instructions):
        		Toast.makeText(getApplicationContext(), "Instructions Selected", Toast.LENGTH_SHORT).show();
        		return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == Activity.RESULT_OK) {
            if (resultCode == RESULT_OK) {
            	//Send user packet to server if we have connectivity
            	try {
	            	if (User.exists(getFilesDir())) {
	            		mCommunicator.queuePacket((mUser == null ? User.load(getFilesDir()) : mUser));
	            	}
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            }
        }
    }
}