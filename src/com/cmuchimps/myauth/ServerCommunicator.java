package com.cmuchimps.myauth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class ServerCommunicator {
	private final String QUEUE_FILE = "packetqueue.json";
	private final String URL = "http://www.casa.cmuchimps.org/appserver";
	private final String OK_RESP = "";
	private final String NOT_OK_RESP = "";
	private final String SEND_USER_RESP = "";
	
	private ArrayList<TransmittablePacket> mQueue;
	private Context mContext;
	private boolean mPopQueue;
	
	public ServerCommunicator(Context c) {
		mContext = c;
		mPopQueue = true;
	}
	
	private void populateQueue() throws IOException {
		mQueue = new JSONDeserializer<ArrayList<TransmittablePacket>>().deserialize(new FileReader(new File(mContext.getFilesDir(), QUEUE_FILE)), ArrayList.class );
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
		
		//destroy successfully transmitted packets
		for (TransmittablePacket dest : toDestroy) {
			mQueue.remove(dest);
		}
		
		serializeQueue();
		
		return retVal; //false means not all were successfully transmitted
	}
	
	private String sendPacket(TransmittablePacket toSend) {
		HttpClient client = new DefaultHttpClient(toSend.convertToParams());
		//Instantiate a POST HTTP method
		try {
			HttpResponse response=client.execute(new HttpPost(URL));
		    String resp = UtilityFuncs.convertStreamToString(new BufferedReader(new InputStreamReader(response.getEntity().getContent())));
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
	
	private String sendPacket(String qt, HashMap<String,String> qs, ArrayList<HashMap<String,String>> as, String ua, HashMap<String,String> supp, String ts) {
		return sendPacket(new TransmissionPacket(qt,qs,as,ua,supp,ts));
	}
	
}