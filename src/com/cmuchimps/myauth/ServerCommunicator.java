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
	
	private ArrayList<TransmissionPacket> mQueue;
	private Context mContext;
	
	public ServerCommunicator(Context c) {
		mContext = c;
	}
	
	private void populateQueue() throws IOException {
		mQueue = new JSONDeserializer<ArrayList<TransmissionPacket>>().deserialize(new FileReader(new File(mContext.getFilesDir(), QUEUE_FILE)), ArrayList.class );
	}
	
	public boolean sendQueuedPackets() throws IOException {
		boolean retVal = true;
		populateQueue();
		ArrayList<TransmissionPacket> toDestroy = new ArrayList<TransmissionPacket>();
		//try and submit all packets queued up
		for (TransmissionPacket toSend : mQueue) {
			String resp = sendPacket(toSend);
			if (resp.equalsIgnoreCase("OK")) {
				toDestroy.add(toSend);
			} else {
				retVal = false;
			}
		}
		
		//destroy successfully transmitted packets
		for (TransmissionPacket dest : toDestroy) {
			mQueue.remove(dest);
		}
		
		//Update mQueue file
		File f = mContext.getFilesDir();
		Writer writer = new BufferedWriter(new FileWriter(new File(f, QUEUE_FILE)));
		try {
			new JSONSerializer().deepSerialize(mQueue,writer);
			writer.flush();
		} finally {
			writer.close();
		}
		
		return retVal; //false means not all were successfully transmitted
	}
	
	public String sendPacket(TransmissionPacket toSend) {
		HttpClient client = new DefaultHttpClient(toSend.convertToParams());
		//Instantiate a POST HTTP method
		try {
			HttpResponse response=client.execute(new HttpPost(URL));
		    String resp = UtilityFuncs.convertStreamToString(new BufferedReader(new InputStreamReader(response.getEntity().getContent())));
		    if (resp.equalsIgnoreCase(OK_RESP)) { //data secured on server
		    	return "OK";
		    } else { //data not secured on server
		    	//two options: store on SD card, or store in database and retry sending later
		    	return "NOT OK";
		    }
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
	        e.printStackTrace();
		}
		
		return "NOT OK";
	}
	
	public String sendPacket(String qt, HashMap<String,String> qs, ArrayList<HashMap<String,String>> as, String ua, HashMap<String,String> supp, String ts) {
		return sendPacket(new TransmissionPacket(qt,qs,as,ua,supp,ts));
	}
	
}