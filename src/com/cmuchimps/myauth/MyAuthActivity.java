package com.cmuchimps.myauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
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
	
	public static final Random r = new Random();
	private QuestionAnswerPair prevQ;
	private QuestionAnswerPair currQ;
	
	private LocationManager lm;
	private LocationListener ll;
	private ConnectivityManager cm;
	private NotificationManager nm;
	private LayoutInflater li;
	private ProgressDialog mPD;
	
	private User mUser;
	private ServerCommunicator mCommunicator;
	private long questionStartTime;
	private long locAnswerTime=0l;
	
	//form fields
	TextView question_prompt;
	TextView send_to_server_message;
	AutoCompleteTextView input;
	Spinner inputDD;
	Button submit;
	Button skip;
	Button mapsButton;
	RadioGroup radioSupp1;
	RadioGroup radioSupp2;
	RadioGroup radioSupp3;
	
	
	//state fields
	private boolean askOnStart = true;
	private int mSuppResponse1;
	private int mSuppResponse2;
	private int mSuppResponse3;
	private boolean isDD = false;
	
	//state fields for skipping
	private boolean[] mChoice;
	private boolean exitOnDialogClose = false;
	
	private final int NUM_SKIP_CHOICES = 4;
	
	private final int DIALOG_CONTACT = 0;
	private final int DIALOG_INST = 1;
	private final int DIALOG_SUB_ERROR = 2;
	private final int DIALOG_SKIP_PICKER = 3;
	private final int DIALOG_EXPLANATION_GIVER = 4;
	private final int DIALOG_SEND_IN_BG_ON_EXIT = 5;
	private final int DIALOG_DELETE_DB = 6;
	
	private final int CONSENT_RESULT = 0;
	private final int NEW_USER_RESULT = 1;
	private final int LOCATION_SELECTOR_RESULT = 2;
	
	private boolean mConsentRequested;
	private final String INSTRUCTIONS = 
			"Thank you for participating in this study!\n\n" +
			"Detailed instructions can be found at http://casa.cmuchimps.org/instructions\n\n" +
			"In short, please answer as many questions as you can everyday.\n\n" +
			"Don't forget to hit Send Data to Server (in the menu options) when you are finished!";
	
	 private BroadcastReceiver packetUpdateReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	mCommunicator.updateQueue();
	        	handleQueuedPacketUpdate();       
	        }
	    };
	
	private OnClickListener mapsButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
				startActivityForResult(new Intent(getApplicationContext(), LocationSelectorActivity.class),
					LOCATION_SELECTOR_RESULT);
			} else {
				Toast.makeText(getApplicationContext(), 
						"Sorry, it looks you don't have internet access right now. Enter the name of the location into the text field instead", 
						Toast.LENGTH_SHORT).show();
			}
		}
    };
    
	//private final int seed;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.question);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        /* Initialize UI elements */
        question_prompt = (TextView)this.findViewById(R.id.question_prompt);
        submit = (Button)this.findViewById(R.id.question_submit);
        skip = (Button)this.findViewById(R.id.question_skip);
        mapsButton = (Button)this.findViewById(R.id.mapsButton);
        radioSupp1 = (RadioGroup)this.findViewById(R.id.radioSupp1);
        radioSupp2 = (RadioGroup)this.findViewById(R.id.radioSupp2);
        radioSupp3 = (RadioGroup)this.findViewById(R.id.radioSupp3);
        input = (AutoCompleteTextView)this.findViewById(R.id.user_answer);
        send_to_server_message = (TextView)this.findViewById(R.id.send_to_server_message);
        
        /* Make UI elements pretty */
        submit.getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
        skip.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
        
        /* Initialize UI element event listeners */
        skip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currQ != null) {
					prevQ = currQ;
					resetFields();
					showDialog(DIALOG_SKIP_PICKER);
				} else { //because ordering matters above
					resetFields();
					//askQuestion();
					(new AskQuestionTask()).execute();
				}
			}
        });
        
        submit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((isDD && inputDD.getSelectedItem() != null) || 
						(!isDD && input.getText().toString().length() > 0)) && 
					radioSupp1.getCheckedRadioButtonId() >= 0 && 
					radioSupp2.getCheckedRadioButtonId() >= 0 && 
					radioSupp3.getCheckedRadioButtonId() >= 0) {
					/**
					 * Validate that all appropriate parts of the form are filled in.
					 * Create TransmissionPacket with the answer;
					 * Ask new question and reset fields
					 */
					if (currQ != null) {
						long amountTime = (currQ.getMapView() ?
								locAnswerTime :
								System.currentTimeMillis() - questionStartTime);
						String user_id = getUser().unique_id;
						String qtext = currQ.getQuestion();
						HashMap<String,String> question = currQ.getQuestionMetas();
						ArrayList<HashMap<String,String>> answer_metas = currQ.getAnswerMetas();
						String user_answer = (isDD ? "" + inputDD.getSelectedItem() : input.getText().toString());
						HashMap<String,String> supplementary_responses = new HashMap<String,String>();
						supplementary_responses.put("supp1",""+mSuppResponse1);
						supplementary_responses.put("supp2",""+mSuppResponse2);
						supplementary_responses.put("supp3",""+mSuppResponse3);
						String timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", System.currentTimeMillis());
						//Toast.makeText(getApplicationContext(), user_answer,Toast.LENGTH_SHORT).show();
						ArrayList<Long> queriedFacts = currQ.getFactsToMarkAsQueried();
						//Log.d("Question", "Registering " + queriedFacts.size() + " facts as queried");
						int val = mDbHelper.registerFactsAsQueried(queriedFacts);
						//Log.d("Question", "Registered " + val + " facts as queried");
						try {
							mCommunicator.queuePacket(new TransmissionPacket(ServerCommunicator.getNextPacketID(getFilesDir()),
									user_id,qtext,question,answer_metas,user_answer,supplementary_responses,
									timestamp, amountTime, !currQ.isRecallQ()));
						} catch (IOException e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(), "An error occured. The packet could not be saved...", Toast.LENGTH_SHORT).show();
						}	
					}
					resetFields();
					//askQuestion();
					(new AskQuestionTask()).execute();
				} else {
					showDialog(DIALOG_SUB_ERROR);
				}
				handleQueuedPacketUpdate();
			}
        });
        
        radioSupp1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId >= 0) {
					mSuppResponse1 = Integer.parseInt(((RadioButton)findViewById(checkedId)).getText().toString());
				}
			}
        });

        radioSupp2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId >= 0) {
					mSuppResponse2 = Integer.parseInt(((RadioButton)findViewById(checkedId)).getText().toString());
				}
			}
        });
        
        radioSupp3.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId >= 0) {
					mSuppResponse3 = Integer.parseInt(((RadioButton)findViewById(checkedId)).getText().toString());
				}
			}
        });
        
        send_to_server_message.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mCommunicator.hasQueuedPackets()) {
		    		exitOnDialogClose = false;
					showDialog(DIALOG_SEND_IN_BG_ON_EXIT);
		    	}
			}
        });
        
        mapsButton.setOnClickListener(mapsButtonListener);
        
        /* Initialize members */
        this.mCommunicator = new ServerCommunicator(this.getApplicationContext());
        this.mConsentRequested = false;
        this.mDbHelper = new KBDbAdapter(this);
        this.mDbHelper.open();
        this.dw = new DataWrapper(mDbHelper);
        this.qg = new QuestionGenerator(mDbHelper,dw,getFilesDir());
        this.lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        this.nm = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        this.li = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        
        this.ktw = new KnowledgeTranslatorWrapper();
        this.mChoice = new boolean[NUM_SKIP_CHOICES];
    	for (int i = 0; i < mChoice.length; i++) {
    		this.mChoice[i] = false;
    	}
    	
        /* Initialize managers and sensor listeners */
        this.initializeLocationListener();
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        //testing cp
        /* for (PackageInfo pack : getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
            ProviderInfo[] providers = pack.providers;
            System.out.println(pack.packageName);
            if (providers != null) {
            	//System.out.println("==> nothing");
                for (ProviderInfo provider : providers) {
                    Log.d("Example", "cp provider: " + provider.authority);
                    
                }
            }
        }*/
    }
    
    private void testFactFinder() {
    	String[] tags = { "Audio" };
    	String[] subclasses = { "Song-Title" };
    	String[] subvalues = { "*" };
    	
    	Long[] facts = mDbHelper.findAllFactsWithTagsWithinTime(tags, subclasses, subvalues,24*UtilityFuncs.HOUR_TO_MILLIS, "dynamic");
    	//this.printFacts(facts);
    }
    
    private void replaceView(boolean dropdown) {
    	ViewGroup parent = (ViewGroup)this.findViewById(R.id.input_layout);
    	parent.removeAllViews();
    	if (dropdown) {
    		View v = li.inflate(R.layout.dropdown_input, parent);
    		inputDD = (Spinner)this.findViewById(R.id.user_answer);
    		isDD = true;
    	} else {
    		View v = li.inflate(R.layout.autocomplete_input, parent);
    		input = (AutoCompleteTextView)this.findViewById(R.id.user_answer);
    		input.requestFocus();
    		InputMethodManager imm = (InputMethodManager)
    			    this.getSystemService(Context.INPUT_METHOD_SERVICE);
    		if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    		isDD = false;
    	}
    	mapsButton = (Button)this.findViewById(R.id.mapsButton);
    	mapsButton.setOnClickListener(mapsButtonListener);
    }
    
    protected void handleQueuedPacketUpdate() {
    	send_to_server_message.setText((mCommunicator.hasQueuedPackets()) ? "You have responses to send to the server (?)." : "");
	}

	private User getUser() {
    	if (mUser != null) return mUser;
    	else {
    		try {
    			mUser = User.load(getFilesDir());
    		} catch (Exception e) {
    			e.printStackTrace();
    			//Log.d("MyAuthActivity","Could not load user");
    		}
    		return mUser;
    	}
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	//mCommunicator.sendPacket(UtilityFuncs.makeTestPacket(mDbHelper.getFact(mDbHelper.getUnqueriedFacts()[0])));
		questionStartTime = System.currentTimeMillis();
		registerReceiver(packetUpdateReceiver, new IntentFilter(UploaderService.BROADCAST_ACTION));
    }
    
    /*public void printFacts(Long[] ids) {
    	for (Long l : ids)
    		Log.d("Fact", mDbHelper.getFact(l).toString());
    }
    
    public void printQATs(Long[] ids) {
    	for (Long l : ids)
    		Log.d("QAT", mDbHelper.getQAT(l).toString());
    }*/
    
    @Override
    public void onStart() {
    	super.onStart();
        /* Control flow: consent form if not accepted => New User Activity if no user => Main Q/A activity */
    	if (!(new File(getFilesDir(),ConsentFormActivity.CONSENT_FILE)).exists()) {
    		this.forcePollSubscriptions();
    		if (!mConsentRequested) {
    			startActivityForResult(new Intent(this, ConsentFormActivity.class),CONSENT_RESULT);
    			mConsentRequested = true;
    		}
    	} else if (mUser == null && !User.exists(getFilesDir())) {
    		startActivityForResult(new Intent(this, NewUserActivity.class),NEW_USER_RESULT);
    	} else {
    		if (mDbHelper.getNumSubscriptions() == 0) {
	    		mPD = ProgressDialog.show(this, "Initializing...", "Please wait a moment while the knowledge base initializes.");
	    		(new DBInitiaterTask()).execute();
	    		try {
					mUser = User.load(getFilesDir());
				} catch (IOException e) {
					e.printStackTrace();
				}
	    		/* Set alarms for updating knowledge subscriptions and uploading packets to server */
	        	setUpdaterAlarm();
	        	setUploaderAlarm();
	        	setNotificationAlarm();
	        	
	    		send_to_server_message.setText((mCommunicator.hasQueuedPackets()) ? "You have responses to send to the server (?)." : "");
	    		
	    		Notification note = new Notification(R.drawable.ic_launcher, "A new notification", System.currentTimeMillis());
	    		note.flags |= Notification.FLAG_AUTO_CANCEL;
	    		note.number += 1;
    		} else {
    			try {
    				mUser = User.load(getFilesDir());
    				//Log.d("MyAuthActivity","User:");
    				//Log.d("MyAuthActivity",mUser.toString());
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
        		// Set alarms for updating knowledge subscriptions and uploading packets to server
            	this.setUpdaterAlarm();
            	this.setUploaderAlarm();
            	this.setNotificationAlarm();
            	
            	// Initialize location manager
        		if (lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
        		} else if (lm != null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
        		}
        		
        		setPassiveLocationUpdateAlarm();
        		
        		send_to_server_message.setText((mCommunicator.hasQueuedPackets()) ? "You have responses to send to the server (?)." : "");
        		//Log.d("Database lock problem", "asking new question");
        		if (askOnStart && mDbHelper.getAllFacts().length > 0) {
        			askOnStart = false;
        			//askQuestion();
        			(new AskQuestionTask()).execute();
        		}
        		Notification note = new Notification(R.drawable.ic_launcher, "A new notification", System.currentTimeMillis());
        		note.flags |= Notification.FLAG_AUTO_CANCEL;
        		note.number += 1;
    		}
        	/* Populate database with scaffolds (qats and subscriptions) if needed */
            /*if (mDbHelper.getNumSubscriptions() == 0) { //if we have no knowledge subscriptions
        		ProgressDialog pd = ProgressDialog.show(this, "Initializing...", "Please wait a moment while the knowledge base initializes.");
            	this.repopulateDB();
            	pd.dismiss();
        		this.forcePollSubscriptions();
        		//Log.d("Database lock problem", "repolling");
        	}
    		try {
				mUser = User.load(getFilesDir());
				//Log.d("MyAuthActivity","User:");
				//Log.d("MyAuthActivity",mUser.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
    		// Set alarms for updating knowledge subscriptions and uploading packets to server
        	this.setUpdaterAlarm();
        	this.setUploaderAlarm();
        	this.setNotificationAlarm();
        	
        	// Initialize location manager
    		if (lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
    			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
    		} else if (lm != null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
    			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
    		}
    		
    		setPassiveLocationUpdateAlarm();
    		
    		send_to_server_message.setText((mCommunicator.hasQueuedPackets()) ? "You have responses to send to the server (?)." : "");
    		//Log.d("Database lock problem", "asking new question");
    		if (askOnStart && mDbHelper.getAllFacts().length > 0) {
    			askOnStart = false;
    			//askQuestion();
    			(new AskQuestionTask()).execute();
    		}
    		Notification note = new Notification(R.drawable.ic_launcher, "A new notification", System.currentTimeMillis());
    		note.flags |= Notification.FLAG_AUTO_CANCEL;
    		note.number += 1;
    		this.cullOldDatabaseEntries();*/
    	}
    }
    
    private void setPassiveLocationUpdateAlarm() {
    	Intent locIntent = new Intent(this, LocationChangedReceiver.class);
		PendingIntent locLisPenInt = PendingIntent.getBroadcast(getApplicationContext(), 
				0, locIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 3*1000, 0, locLisPenInt);
	}

	@Override
    public void onPause() {
    	if (lm != null) lm.removeUpdates(ll);
    	if ((cm != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()) && mCommunicator.hasQueuedPackets()) { 
    		Intent uploader = new Intent(getApplicationContext(),UploaderService.class);
    		this.startService(uploader);
    	}
    	
    	try {
	    	ServerCommunicator.serializePacketID(getFilesDir());
	    	qg.serializeIDTrack();
    	} catch (IOException e) {
    		//Log.d("MyAuthActivity","Could not serialize...");
    		e.printStackTrace();
    	}
    	
    	try {
    		unregisterReceiver(packetUpdateReceiver);
    	} catch (IllegalArgumentException e) {
    		//Log.d("MyAuthActivity", "Cannot unregister packetUpdateReceiver because not registered.");
    	}
    	super.onPause();
    }
    
    @Override
    public void onBackPressed() {
    	if (mCommunicator.hasQueuedPackets()) {
    		showDialog(DIALOG_SEND_IN_BG_ON_EXIT);
    		exitOnDialogClose = true;
    	} else {
    		super.onBackPressed();
    	}
    }
    
    @Override
    public void onStop() {
    	if (lm != null) lm.removeUpdates(ll);
    	if (cm != null && cm.getActiveNetworkInfo().isConnectedOrConnecting() && mCommunicator.hasQueuedPackets()) { 
    		Intent uploader = new Intent(getApplicationContext(),UploaderService.class);
    		this.startService(uploader);
    	}
    	
    	try {
	    	ServerCommunicator.serializePacketID(getFilesDir());
	    	qg.serializeIDTrack();
    	} catch (IOException e) {
    		//Log.d("MyAuthActivity","Could not serialize packet id...");
    		e.printStackTrace();
    	}
    	
    	try {
    		unregisterReceiver(packetUpdateReceiver);
    	} catch (IllegalArgumentException e) {
    		//Log.d("MyAuthActivity", "Cannot unregister packetUpdateReceiver because not registered.");
    	}

		this.cullOldDatabaseEntries();
    	super.onStop();
    }
    
    private void resetFields() {
    	input.setText("");
    	radioSupp1.clearCheck();
    	radioSupp2.clearCheck();
    	radioSupp3.clearCheck();
    	mSuppResponse1 = -1;
    	mSuppResponse2 = -2;
    	mSuppResponse3 = -3;
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
				//2 tags: person:User was at location
				//also get Timestamp and day of week
				//Log.d("MyAuthActivity","Location changed event has triggered for some reason.");
				if (System.currentTimeMillis() > mDbHelper.getSubscriptionDueTimeFor("Location")) {
					//Log.d("MyAuthActivity","adding location fact from location listener");
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
					curr.put("subclass", "Accuracy");
					curr.put("subvalue", ""+location.getAccuracy());
					tags.add(curr);
					//TODO: Should we do reverse geocoding here? Who knows.
					//no metas for now
					mDbHelper.createFact(timestamp, dayOfWeek, "dynamic", tags, metas);
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
    	currQ = qg.askQuestion(getApplicationContext());
        if (currQ != null) {
	        question_prompt.setText(currQ.getQuestion());
	        if (!isDD) input.setAdapter(null);
	        else inputDD.setAdapter(null);
        	if (!currQ.isRecallQ()) {
        		//Log.d("QuestionType","Recog question");
        		replaceView(true);
        		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        				android.R.layout.simple_spinner_item, currQ.getAnswerListAsArr());
        		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        		inputDD.setAdapter(adapter);
        	} else if (currQ.isAutoCompl()) {
        		//Log.d("QuestionType","Recall autocomplete question");
        		replaceView(false);
	        	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
	            		android.R.layout.simple_dropdown_item_1line, currQ.getAutoComplListAsArr());
	        	input.setAdapter(adapter);
        	} else {
        		//Log.d("QuestionType","Neither type");
        		replaceView(false);
        	}
        	
	        if (currQ.getMapView()) {
	        	mapsButton.setEnabled(true);
	        	if (cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
	        		input.setEnabled(false);
	        		input.setHint("Click the map button to answer...");
	        		mapsButton.requestFocus();
	        	} else 
	        		input.setHint("Enter Answer...");
	        	input.setText("");
	        } else {
	        	mapsButton.setEnabled(false);
	        	if (!isDD) {
	        		input.setEnabled(true);
	        		input.setHint("Enter Answer...");
	        		input.setText("");
	        		input.requestFocus();
	        	}
	        }
	        questionStartTime = System.currentTimeMillis();
        } else {
        	question_prompt.setText(R.string.placeholder);
        	Toast.makeText(getApplicationContext(), "We do not have enough data to create a question yet, come back later!", Toast.LENGTH_SHORT).show();
        }
    }
    
    /*private void printFacts(Long[] facts) {
    	Log.d("MyAuthActivity-FACT","ALL FACTS:\n");
        for (Long l : facts) {
        	Log.d("MyAuthActivity-FACT",mDbHelper.getFact(l).toString());
        }
    }
    
    private void printQATS(Long[] qats) {
        Log.d("MyAuthActivity","ALL QATS:\n");
        for (Long l : qats) { 
        	Log.d("MyAuthActivity",mDbHelper.getQAT(l).toString());
        }
    }*/
    
    private void repopulateDB() {
        try {
			this.populateTagClasses(this.getAssets().open("qg_other_files/tag_classes.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        this.mParser = new KBXMLParser(this,mDbHelper);
        mParser.parseQATs("qg_xml/qats.xml");
        createSensorSubscriptions();
    }
    
    private void deleteDB() {
    	this.deleteDatabase(KBDbAdapter.DATABASE_NAME);
    	this.mDbHelper.close();
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
    
    private void setNotificationAlarm() {
    	Intent notifier = new Intent(this, NotificationReceiver.class);
    	Calendar cal = Calendar.getInstance();
    	cal.set(Calendar.HOUR_OF_DAY,20);
    	cal.set(Calendar.MINUTE,0);
    	cal.set(Calendar.SECOND, 0);
    	cal.set(Calendar.MILLISECOND, 0);
    	if (System.currentTimeMillis() > cal.getTimeInMillis()) 
    		cal.add(Calendar.DAY_OF_MONTH, 1);
    	//Log.d("MyAuthActivity","Setting alarm for " + cal.getTimeInMillis());
    	PendingIntent recurringNotification = PendingIntent.getBroadcast(getApplicationContext(), 0, 
    			notifier, PendingIntent.FLAG_UPDATE_CURRENT);
    	AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    	alarms.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 
    			AlarmManager.INTERVAL_HALF_DAY, recurringNotification);
    }
    
    /**
     * Sets updater alarm so that subscriptions are polled periodically.
     */
    private void setUpdaterAlarm() {
    	Intent updater = new Intent(this, SubscriptionReceiver.class);
    	PendingIntent recurringUpdate = PendingIntent.getBroadcast(getApplicationContext(), 0, updater, PendingIntent.FLAG_CANCEL_CURRENT);
    	AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    	alarms.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, 
    			10l * UtilityFuncs.MIN_TO_MILLIS, recurringUpdate);
    	//alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * UtilityFuncs.MIN_TO_MILLIS, 10l*UtilityFuncs.MIN_TO_MILLIS, recurringUpdate);
    }
    
    private void setUploaderAlarm() {
    	Intent uploader = new Intent(this, UploaderReceiver.class);
    	PendingIntent recurringUpload = PendingIntent.getBroadcast(getApplicationContext(), 0, uploader, PendingIntent.FLAG_UPDATE_CURRENT);
    	AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    	alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, 
    			System.currentTimeMillis() + 1 * UtilityFuncs.MIN_TO_MILLIS, 
    			AlarmManager.INTERVAL_HALF_DAY, recurringUpload);
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
    	// System.out.println("Entering force poll subscriptions...");
    	Intent updater = new Intent(this, KnowledgeTranslatorWrapper.class);
    	String[] dueSubs = mDbHelper.getAllSubscriptions().keySet().toArray(new String[mDbHelper.getAllSubscriptions().size()]);
    	updater.putExtra("dueSubs", dueSubs);
    	this.startService(updater);
    }
    
    /**
     * Initializes subscriptions table.
     */
    private void createSensorSubscriptions() {
    	String[] SensorSubscriptions = new String[] { 
    		"Communication", "InternetBrowsing", "Media", 
    		"UserDictionary", "Contact", "Calendar", 
    		"ApplicationUse", "Location"
    	};
    	Long[] PollIntervals = new Long[] { 
    		3l*UtilityFuncs.HOUR_TO_MILLIS, //Comm
    		3l*UtilityFuncs.DAY_TO_MILLIS, //Int Brows
    		1l*UtilityFuncs.DAY_TO_MILLIS, //Media
    		7l*UtilityFuncs.DAY_TO_MILLIS, //User Dict 
    		3l*UtilityFuncs.DAY_TO_MILLIS, //Contact
    		7l*UtilityFuncs.DAY_TO_MILLIS, //Calendar
    		30l*UtilityFuncs.MIN_TO_MILLIS, //AppUse
    		30l*UtilityFuncs.MIN_TO_MILLIS //Location
    	};
    	Long[] InitialUpdateTimes = new Long[] {
    		System.currentTimeMillis() - (1*UtilityFuncs.DAY_TO_MILLIS),
    		System.currentTimeMillis() - (1*UtilityFuncs.DAY_TO_MILLIS),
    		0l,
    		0l,
    		0l,
    		0l,
    		System.currentTimeMillis() - (1*UtilityFuncs.DAY_TO_MILLIS),
    		System.currentTimeMillis() - (1*UtilityFuncs.DAY_TO_MILLIS)
    	};
    	
    	for (int i = 0; i < SensorSubscriptions.length; i++) {
			if (!mDbHelper.subscriptionExists(SensorSubscriptions[i])) {
				//System.out.println(System.currentTimeMillis());
				mDbHelper.createSubscription(SensorSubscriptions[i], 
					PollIntervals[i], InitialUpdateTimes[i], 
					"com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + 
							SensorSubscriptions[i] + "KnowledgeSubscription");
				try {
					Class c = Class.forName("com.cmuchimps.myauth.KnowledgeTranslatorWrapper$" + SensorSubscriptions[i] + "KnowledgeSubscription");
					if (!(Modifier.isStatic(c.getModifiers()) || Modifier.isAbstract(c.getModifiers()))) {
						KnowledgeSubscription ks = (KnowledgeSubscription) c.getDeclaredConstructor(new Class[] { KnowledgeTranslatorWrapper.class }).newInstance(new Object[] { this.ktw });
						c.getMethod("poll", null).invoke(ks, null);
						//Log.d("MyAuthActivity","Successfully polled " + SensorSubscriptions[i] + "KnowledgeSubscription!");
					}
				} catch (Throwable e) {
					//Log.d("MyAuthActivity","Failed to update class " + SensorSubscriptions[i]);
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
        		sendPacketsInBackground();
        		return true;
        	case (R.id.contact):
        		this.showDialog(DIALOG_CONTACT);
        		return true;
        	case (R.id.instructions):
        		this.showDialog(DIALOG_INST);
        		return true;
        	case (R.id.refresh):
        		this.forcePollSubscriptions();
        		return true;
        	case (R.id.delete):
        		this.showDialog(DIALOG_DELETE_DB);
        		//startActivityForResult(new Intent(this, LocationSelectorActivity.class), LOCATION_SELECTOR_RESULT);
        		//this.replaceView(isDD);
        		return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == NEW_USER_RESULT) {
            if (resultCode == RESULT_OK) {
            	//Send user packet to server if we have connectivity
            	try {
            		//System.out.println("Getting to user created!");
	            	if (User.exists(getFilesDir())) {
	            		mCommunicator.queuePacket((mUser == null ? User.load(getFilesDir()) : mUser));
	            	}
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            }
        } else if (requestCode == CONSENT_RESULT) {
        	if (resultCode == RESULT_CANCELED) {
        		finish();
        	} else {
        		//queue up consent for transmission
        	}
        } else if (requestCode == LOCATION_SELECTOR_RESULT) {
        	double lat = (Double) data.getExtras().get("latitude");
        	double lng = (Double) data.getExtras().get("longitude");
        	locAnswerTime = (Long) data.getExtras().get("total_time");
        	String toPrint = lat + "," + lng;
        	input.setText(toPrint);
        	//System.out.println(toPrint);
        	//Toast.makeText(getApplicationContext(), toPrint, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int dialogId) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	switch (dialogId) {
    	case DIALOG_CONTACT:
    		builder.setMessage("You may reach me through phone or email\nPhone:678-978-1547\nEmail:sauvik@cmu.edu")
    			   .setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).setCancelable(true);
    		return builder.create();
    	case DIALOG_INST:
    		builder.setMessage(INSTRUCTIONS)
    			   .setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).setCancelable(true);
    		return builder.create();
    	case DIALOG_SUB_ERROR:
    		builder.setMessage("Please answer all fields before hitting submit.")
			    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).setCancelable(true);
    		return builder.create();
    	case DIALOG_SKIP_PICKER:
    		CharSequence[] items = { "Not comfortable answering", "Totally can't remember", "Don't understand", "Other" };
    		builder.setTitle("Pick a reason for skipping:")
    			.setMultiChoiceItems(items, new boolean[] { false, false, false, false }, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						mChoice[which] = isChecked;
					}
				})
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mChoice[3]) {
							dialog.dismiss();
							showDialog(DIALOG_EXPLANATION_GIVER);
						} else {
							queueSkipQuestionPacket("");
							dialog.dismiss();
							//askQuestion();
							(new AskQuestionTask()).execute();
						}
					}
				})
				.setCancelable(false);
    		return builder.create();
    	case DIALOG_EXPLANATION_GIVER:
    		LayoutInflater factory = LayoutInflater.from(this);
            final View explanationDialogView = factory.inflate(R.layout.explanation_dialog, null);
            builder.setTitle("Explanation for Other")
                .setView(explanationDialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	final EditText explanationEditText = (EditText)explanationDialogView.findViewById(R.id.explanationEntry);
                    	queueSkipQuestionPacket(explanationEditText.getText().toString());
    					//askQuestion();
                    	(new AskQuestionTask()).execute();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	queueSkipQuestionPacket("");
    					//askQuestion();
                    	(new AskQuestionTask()).execute();
                        dialog.dismiss();
                    }
                });
    		return builder.create();
    	case DIALOG_SEND_IN_BG_ON_EXIT:
    		builder.setMessage("It looks like you have " + mCommunicator.numQueuedPackets() + " responses to send to the server.\n\n" +
    				"Would you like to send them in the background now?")
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendPacketsInBackground();
					dialog.dismiss();
					if (exitOnDialogClose) finish();
				}
			})
			.setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					if (exitOnDialogClose) finish();
				}
			})
			.setCancelable(true);
    		return builder.create();
    	case DIALOG_DELETE_DB:
    		builder.setMessage("This action will delete the database, along with all facts encoded about your activities used by this app.\n\n" +
    				"Note, however, that this action will not affect the data of other applications on your phone.\n\n" +
    				"Please do not undergo this action until after the study or explicitly told to so.")
    		.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteDB();
					//repopulateDB();
					dialog.dismiss();
					finish();
				}
			})
			.setCancelable(true);
    		return builder.create();
    	default:
    		return null;
    	}
    }
    
    private void queueSkipQuestionPacket(String explanation) {
    	long amountTime = System.currentTimeMillis() - questionStartTime;
    	String qtext = currQ.getQuestion();
		HashMap<String,String> question = currQ.getQuestionMetas();
		String user_id = getUser().unique_id;
		String timestamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", System.currentTimeMillis());
		try {
			TransmittablePacket tp = new SkipQuestionPacket(ServerCommunicator.getNextPacketID(getFilesDir()),
					qtext, question, mChoice[0],mChoice[1],mChoice[2],mChoice[3],
					explanation,timestamp,user_id,!currQ.isRecallQ(), amountTime);
			mCommunicator.queuePacket(tp);
			handleQueuedPacketUpdate();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "An error occured. The packet could not be saved...", Toast.LENGTH_SHORT).show();
		}
    }
    
    private void sendPacketsInBackground() {
    	if (cm.getActiveNetworkInfo().isConnected() && mCommunicator.hasQueuedPackets()) { 
    		//Log.d("MyAuth","Starting service to upload packets...");
			Intent uploader = new Intent(getApplicationContext(),UploaderService.class);
    		this.startService(uploader);
    		Toast.makeText(getApplicationContext(), "Sending in background...", Toast.LENGTH_SHORT).show();
    	} else {
    		Toast.makeText(getApplicationContext(), "Sorry, it appears you do not have internet connectivity...", Toast.LENGTH_SHORT).show();
    	}
    }
    
    private void cullOldDatabaseEntries() {
    	this.startService(new Intent(getApplicationContext(), CullOldDBEntriesService.class));
    }
    
    private class DBInitiaterTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			repopulateDB();
			return null;
		}
    	
		@Override
		protected void onPostExecute(Void result) {	
        	forcePollSubscriptions();
        	if (mPD != null) mPD.dismiss();
        	
        	/* Initialize location manager */
    		if (lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
    			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
    		} else if (lm != null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
    			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10*UtilityFuncs.MIN_TO_MILLIS, 0, ll);
    		}
    		
    		setPassiveLocationUpdateAlarm();
    		
    		//Log.d("Database lock problem", "asking new question");
    		if (askOnStart && mDbHelper.getAllFacts().length > 0) {
    			askOnStart = false;
    			//askQuestion();
    			(new AskQuestionTask()).execute();
    		}
		}
    }
    
    private class AskQuestionTask extends AsyncTask<Void, Void, QuestionAnswerPair> {
    	@Override
    	protected void onPreExecute() {
    		question_prompt.setText("Please wait while I generate a question...");
    	}
    	
    	@Override
    	protected QuestionAnswerPair doInBackground(Void... params) {
    		return qg.askQuestion(getApplicationContext());
    	}
    	
    	@Override
    	protected void onPostExecute(QuestionAnswerPair result) {
    		currQ = result;
    		if (currQ != null) {
		        question_prompt.setText(currQ.getQuestion());
		        if (!isDD) input.setAdapter(null);
		        else inputDD.setAdapter(null);
	        	if (!currQ.isRecallQ()) {
	        		//Log.d("QuestionType","Recog question");
	        		replaceView(true);
	        		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
	        				android.R.layout.simple_spinner_item, currQ.getAnswerListAsArr());
	        		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        		inputDD.setAdapter(adapter);
	        	} else if (currQ.isAutoCompl()) {
	        		//Log.d("QuestionType","Recall autocomplete question");
	        		replaceView(false);
		        	ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
		            		android.R.layout.simple_dropdown_item_1line, currQ.getAutoComplListAsArr());
		        	input.setAdapter(adapter);
	        	} else {
	        		//Log.d("QuestionType","Neither type");
	        		replaceView(false);
	        	}
	        	
		        if (currQ.getMapView()) {
		        	mapsButton.setEnabled(true);
		        	if (cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
		        		input.setEnabled(false);
		        		input.setHint("Click the map button to answer...");
		        		mapsButton.requestFocus();
		        	} else 
		        		input.setHint("Enter Answer...");
		        	input.setText("");
		        } else {
		        	mapsButton.setEnabled(false);
		        	if (!isDD) {
		        		input.setEnabled(true);
		        		input.setHint("Enter Answer...");
		        		input.setText("");
		        		input.requestFocus();
		        	}
		        }
		        questionStartTime = System.currentTimeMillis();
	        } else {
	        	question_prompt.setText(R.string.placeholder);
	        	Toast.makeText(getApplicationContext(), "I don't have any questions to ask you right now. Please try again later!", Toast.LENGTH_SHORT).show();
	        }
    	}
    }
}