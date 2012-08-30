package com.cmuchimps.myauth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.util.Log;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class ServerCommunicator {
	private final static String NEXT_RESPONSE_ID_FILE = "nextrid.json";
	private final static String QUEUE_FILE = "packetqueue.json";
	private final static String URL = "http://casa.cmuchimps.org/handler/index";
	private final static String OK_RESP = "OK";
	private final static String NOT_OK_RESP = "NOT OK";
	private final static String SEND_USER_RESP = "SEND_USER";
	
	private ArrayList<TransmittablePacket> mQueue;
	private Context mContext;
	private boolean mPopQueue;
	private static int nextRespID = 0;
	private static boolean loadNext = true;
	
	public ServerCommunicator(Context c) {
		mContext = c;
		mPopQueue = true;
		try {
			initialize();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d("ServerCommunicator","Cannot write new queue file");
		}
	}
	
	public void clearQueue() throws IOException {
		mQueue.clear();
		serializeQueue();
	}
	
	public static int getNextPacketID(File filesDir) {
		if (loadNext) {
			File temp = new File(filesDir,NEXT_RESPONSE_ID_FILE);
			if (temp.exists()) {
				try {
					nextRespID = new JSONDeserializer<Integer>().deserialize(new BufferedReader(new FileReader(temp)));
				} catch (FileNotFoundException e) {
					return MyAuthActivity.r.nextInt(Integer.MAX_VALUE-10000) + 10000;
				}
			} else {
				nextRespID = 0;
			}
			loadNext = false;
		}
		return nextRespID++;
	}
	
	public static void serializePacketID(File filesDir) throws IOException {
		Writer writer = new BufferedWriter(new FileWriter(new File(filesDir,NEXT_RESPONSE_ID_FILE)));
		try {
			new JSONSerializer().deepSerialize(nextRespID,writer);
			writer.flush();
		} finally {
			writer.close();
		}
	}
	
	private void initialize() throws IOException {
		File f = new File(mContext.getFilesDir(),QUEUE_FILE);
		if (!f.exists()) {
			mQueue = new ArrayList<TransmittablePacket>();
			serializeQueue();
		}
	}
	
	private void populateQueue() throws IOException {
		mQueue = new JSONDeserializer<ArrayList<TransmittablePacket>>().deserialize(new BufferedReader(new FileReader(new File(mContext.getFilesDir(), QUEUE_FILE))), ArrayList.class );
	}
	
	private void serializeQueue() throws IOException {
		//Update mQueue file
		File f = mContext.getFilesDir();
		Writer writer = new BufferedWriter(new FileWriter(new File(f, QUEUE_FILE)));
		try {
			new JSONSerializer().deepSerialize(mQueue,writer);
			writer.flush();
		} finally {
			writer.close();
		}
	}
	
	public void updateQueue() {
		try {
			populateQueue();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return;
		}
	}
	
	public boolean hasQueuedPackets() {
		if (mPopQueue) {
			try {
				populateQueue();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return false;
			}
			mPopQueue = false;
		}
		return mQueue.size() > 0;
	}
	
	public int numQueuedPackets() {
		if (mPopQueue) {
			try {
				populateQueue();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return -1;
			}
			mPopQueue = false;
		}
		return mQueue.size();
	}
	
	public void printQueue() {
		if (mPopQueue) {
			try {
				populateQueue();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return;
			}
			mPopQueue = false;
		}
		
		for (TransmittablePacket tp : mQueue) {
			Log.d("ServerCommunicator",tp.toString());
		}
	}
	
	public void queuePacket(TransmittablePacket toQueue) throws IOException {
		if (mPopQueue) {
			populateQueue();
			mPopQueue = false;
		}
		mQueue.add(toQueue);
		serializeQueue();
	}
	
	public String sendUserPacket() throws IOException {
		User user = User.load(mContext.getFilesDir());
		if (user != null) {
			return sendPacket(user);
		}
		return "COULD NOT SEND USER";
	}
	
	public boolean sendQueuedPackets() throws IOException {
		boolean retVal = true;
		if (mPopQueue) {
			populateQueue();
			mPopQueue = false;
		}
		ArrayList<TransmittablePacket> toDestroy = new ArrayList<TransmittablePacket>();
		//try and submit all packets queued up
		for (TransmittablePacket toSend : mQueue) {
			String resp = sendPacket(toSend);
			if (resp.equalsIgnoreCase("OK")) {
				toDestroy.add(toSend);
			} else {
				retVal = false;
			}
		}
		
		Log.d("ServerCommunicator","Num packets accepted by server:" + toDestroy.size());
		//destroy successfully transmitted packets
		for (TransmittablePacket dest : toDestroy) {
			mQueue.remove(dest);
		}
		
		serializeQueue();
		
		return retVal; //false means not all were successfully transmitted
	}
	
	private String sendPacket(TransmittablePacket toSend) {
		Log.d("ServerCommunicator","Attempting to send packet...");
		HttpClient client = new DefaultHttpClient();
		//Instantiate a POST HTTP method
		try {
			HttpPost postReq = new HttpPost(URL);
			postReq.setEntity(new UrlEncodedFormEntity(toSend.convertToNVP()));
			HttpResponse response=client.execute(postReq);
		    String resp = UtilityFuncs.convertStreamToString(new BufferedReader(new InputStreamReader(response.getEntity().getContent())));
		    Log.d("ServerCommunicator","Response received is: ");
		    Log.d("ServerCommunicator",resp);
		    if (resp.equalsIgnoreCase(OK_RESP)) { //data secured on server
		    	return "OK";
		    } else if (resp.equalsIgnoreCase(SEND_USER_RESP)) {
		    	return sendUserPacket();
			}
		    else { //data not secured on server
		    	if (!mQueue.contains(toSend)) mQueue.add(toSend);
		    	return "NOT OK";
		    }
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
	        e.printStackTrace();
		}
		
		return "NOT OK";
	}
}