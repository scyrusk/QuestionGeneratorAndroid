package com.cmuchimps.myauth;

import java.io.BufferedReader;
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
import android.os.Bundle;
import android.text.format.DateFormat;
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
	public static KBDbAdapter mDbHelper;
	private DataWrapper dw;
	private QuestionGenerator qg;
	private KnowledgeTranslatorWrapper ktw;
	
	public static final Random r = new Random();
	private QuestionAnswerPair currQ;
	
	private LocationManager lm;
	
	//fields
	TextView output;
	Button submit;
	Button newq;
	
	//private final int seed;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        output = (TextView)this.findViewById(R.id.output);
        submit = (Button)this.findViewById(R.id.submit);
        newq = (Button)this.findViewById(R.id.newq);
        final EditText input = (EditText)this.findViewById(R.id.input);

        submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currQ.getAnswers().contains(input.getText().toString())) {
					Toast.makeText(getApplicationContext(), "Correct Answer!", Toast.LENGTH_SHORT).show();
					input.setText("");
					askQuestion();
				} else {
					Toast.makeText(getApplicationContext(), "Incorrect Answer!", Toast.LENGTH_SHORT).show();
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
        
        
        this.mDbHelper = new KBDbAdapter(this);
        this.mDbHelper.open();
        //this.repopulateDB();
        this.dw = new DataWrapper(mDbHelper);
        this.qg = new QuestionGenerator(mDbHelper,dw);
        this.lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        this.ktw = new KnowledgeTranslatorWrapper();
        this.setUpdaterAlarm();
        //need to figure out how to call update on a separate thread
        
        //initializeLocationListener();
        //this.getSystemService(Context.)
        
        //Long[] ff = mDbHelper.getFilteredFactsByTime(24*60*60*1000,mDbHelper.getFact(facts[1]).getTimestamp());
        //Long[] ff = mDbHelper.getFilteredFactsByTag(new String[] {"Application:Game", "Application:Communication"});
        /*Long[] fq = mDbHelper.getFilteredQats(null, new String[] { "Application", "Location" });
        System.out.println("Count: " + fq.length);
        for (Long l : fq) {
            System.out.println(mDbHelper.getQAT(l));
        }
        System.out.println("");*/
        //System.out.println(mDbHelper.getQAT(2));
        /*Fact fact = mDbHelper.getFact(facts[r.nextInt(facts.length)]);
        System.out.println("Chosen Fact::");
        System.out.println(fact);
        HashMap<Long,Integer[]> fqats = new HashMap<Long,Integer[]>();
        for (Long l : qats) {
        	Integer[] result = mDbHelper.getQAT(l).matches(fact);
        	if (result != null) {
        		fqats.put(l, result);
        	}
        }
        System.out.println("Macthing QATS:");
        for (Long l : fqats.keySet()) {
        	System.out.println(mDbHelper.getQAT(l));
        }*/
    }
    
    private void initializeLocationListener() {
		LocationListener ll = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				//create fact about location here
				//2 tags: person:User was at location:Towers=
				//also get Timestamp and day of week
				String timestamp, dayOfWeek;
				ArrayList<HashMap<String,String>> tags = new ArrayList<HashMap<String,String>>(),metas = new ArrayList<HashMap<String,String>>();
				Date date = new Date(location.getTime());
				timestamp = (String) DateFormat.format("yyyy-MM-dd hh:mm:ss", date);
				dayOfWeek = (String) DateFormat.format("EEEE", date);
				//add person:User tag
				HashMap<String,String> curr = new HashMap<String,String>();
				curr.put("tag_class", "person");
				curr.put("subclass", "User");
				tags.add(curr);
				//add location:GPS tag
				curr = new HashMap<String,String>();
				curr.put("tag_class", "location");
				curr.put("subclass", "Network");
				//lat,long,accuracy
				curr.put("subvalue", location.getLatitude() + "," + location.getLongitude() + "," + location.getAccuracy());
				tags.add(curr);
				//TODO: Should we do reverse geocoding here? Who knows.
				//no metas for now
				mDbHelper.createFact(timestamp, dayOfWeek, tags, metas);
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
		
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
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
    	String[] dueSubs = mDbHelper.getAllDueSubscriptions().keySet().toArray(new String[mDbHelper.getAllDueSubscriptions().size()]);
    	updater.putExtra("dueSubs", dueSubs);
    	PendingIntent recurringUpdate = PendingIntent.getBroadcast(getApplicationContext(), 0, updater, PendingIntent.FLAG_UPDATE_CURRENT);
    	AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    	alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * UtilityFuncs.MIN_TO_MILLIS, AlarmManager.INTERVAL_HOUR, recurringUpdate);
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
    	Intent updater = new Intent(this, KnowledgeTranslatorWrapper.class);
    	String[] dueSubs = mDbHelper.getAllSubscriptions().keySet().toArray(new String[mDbHelper.getAllSubscriptions().size()]);
    	updater.putExtra("dueSubs", dueSubs);
    	this.startService(updater);
    }
    
    /**
     * Initializes subscriptions table.
     */
    private void createSensorSubscriptions() {
    	String[] SensorSubscriptions = new String[] { "Communication", "InternetBrowsing", "Media", "UserDictionary", "Contact", "Calendar"};
    	Long[] PollIntervals = new Long[] { 3l*UtilityFuncs.HOUR_TO_MILLIS, 3l*UtilityFuncs.DAY_TO_MILLIS, 1l*UtilityFuncs.DAY_TO_MILLIS, 7l*UtilityFuncs.DAY_TO_MILLIS, 3l*UtilityFuncs.DAY_TO_MILLIS, 7l*UtilityFuncs.DAY_TO_MILLIS};
    	
    	for (int i = 0; i < SensorSubscriptions.length; i++) {
			if (!mDbHelper.subscriptionExists(SensorSubscriptions[i])) {
				System.out.println(System.currentTimeMillis());
				mDbHelper.createSubscription(SensorSubscriptions[i], PollIntervals[i], System.currentTimeMillis(), "com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + SensorSubscriptions[i] + "KnowledgeSubscription");
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
}