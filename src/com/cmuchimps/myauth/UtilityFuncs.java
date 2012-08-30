package com.cmuchimps.myauth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import android.util.Log;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class UtilityFuncs {
	public static final int MS_PER_SEC = 1000;
	public static final int SECS_PER_MIN = 60;
	public static final int MINS_PER_HOUR = 60;
	public static final int HOURS_PER_DAY = 24;
	public static final int DAYS_PER_WEEK = 7;
	
	public static final int SEC_TO_MILLIS = 1000;
	public static final int MIN_TO_MILLIS = SECS_PER_MIN * SEC_TO_MILLIS;
	public static final int HOUR_TO_MILLIS = MINS_PER_HOUR * MIN_TO_MILLIS;
	public static final int DAY_TO_MILLIS = HOURS_PER_DAY * HOUR_TO_MILLIS;
	public static final int WEEK_TO_MILLIS = DAYS_PER_WEEK * DAY_TO_MILLIS;
	
	public UtilityFuncs() {
		
	}
	
	public String hi() {
		return "hi";
	}
	
	public static String underscoreCamel(String camelCase) {
		StringBuffer retVal = new StringBuffer();
		for (int i = 0; i < camelCase.length(); i++)
			retVal.append( (Character.isUpperCase(camelCase.charAt(i)) && i > 0  ? "_" : "") + Character.toLowerCase(camelCase.charAt(i)));
		return retVal.toString();
	}
		
	public static String join(String[] composite, String glue) {
		if (composite.length <= 0) return "";
		StringBuffer sb = new StringBuffer(composite[0]);
		for (int i = 1; i < composite.length; i++) {
			sb.append(glue + composite[i]);
		}
		Log.d("UtilityFuncs", sb.toString());
		return sb.toString();
	}
	
	public static int search(Long[] haystack, Long needle) {
		for (int i = 0; i < haystack.length; i++) {
			if (needle == haystack[i])
				return i;
		}
		return -1;
	}
	
	public static boolean isMeaningfulString(String s) {
		return !(s == null || s.equalsIgnoreCase("")); 
	}
	
	public static Long[] getUnion(Long[] first, Long[] second) {
		HashSet<Long> temp = new HashSet<Long>();
		for (Long l : first) temp.add(l);
		for (Long l : second) temp.add(l);
		Long[] retVal = temp.toArray(new Long[temp.size()]);
		Arrays.sort(retVal);
		return retVal;
	}
	
	public static Long[] getIntersection(Long[] first, Long[] second) {
		HashSet<Long> temp = new HashSet<Long>();
		Long[] smaller = (first.length > second.length ? second : first);
		Long[] bigger = (smaller == first ? second : first);
		for (Long l : smaller) {
			if (UtilityFuncs.search(bigger, l) >= 0)
				temp.add(l);
		}
		Long[] retVal = temp.toArray(new Long[temp.size()]);
		Arrays.sort(retVal);
		return retVal;
	}
	
	public static int rsum(int[] arr) {
		int cum = 0;
		for (int i = 0; i < arr.length; i++) cum += arr[i];
		return cum;
	}
	
	public static int getMatchIdx(int[] arr) {
		for (int i = 0; i < arr.length; i++)
			if (arr[i] == 1) return i;
		return -1;
	}
	
	public static Integer[] getMatches(int[] arr) {
		ArrayList<Integer> retVal = new ArrayList<Integer>(arr.length);
		for (int i = 0; i < arr.length; i++) 
			if (arr[i] == 1) retVal.add(i);
		return retVal.toArray(new Integer[retVal.size()]);
	}
	
	public static HashMap<String,String> duplicateMap(HashMap<String,String> initial) {
		HashMap<String,String> retVal = new HashMap<String,String>();
		for (String k : initial.keySet()) 
			retVal.put(k, initial.get(k));
		return retVal;
	}
	
	public static String convertStreamToString(BufferedReader is) {
		StringBuffer sb = new StringBuffer();
		String input;
		try {
			while ((input = is.readLine()) != null) 
				sb.append(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	public static void TestingFLEXJson(File filesDir) {
		try {
			HashMap<String,String> qs = new HashMap<String,String>();
			qs.put("timeInQ", "11am");
			qs.put("qatid","6");
			ArrayList<HashMap<String,String>> answers = new ArrayList<HashMap<String,String>>();
			HashMap<String,String> answer = new HashMap<String,String>();
			answer.put("exact", "true");
			answer.put("timestamp", "12am");
			answer.put("value", "abc");
			answers.add(answer);
			answer = new HashMap<String,String>();
			answer.put("exact", "false");
			answer.put("timestamp", "1am");
			answer.put("value", "def");
			answers.add(answer);
			HashMap<String,String> supp = new HashMap<String,String>();
			supp.put("How easy was it for you to recall the answer to this question?", "5");
			supp.put("How confident are you in your answer?", "5");
			TransmissionPacket temp = new TransmissionPacket(0,"kash","Who what when where how at 11am?",qs,answers,"abc",supp,"now");
			User us = new User(filesDir,"Ced","ced@ceds.com",23,"Black","Male");
			ArrayList<TransmittablePacket> hi = new ArrayList<TransmittablePacket>();
			hi.add(temp);
			hi.add(us);
			Log.d("UtilityFuncs", "Before:");
			Log.d("UtilityFuncs", temp.toString());
			Writer writer = new BufferedWriter(new FileWriter(new File(filesDir,"temp.json")));
		    new JSONSerializer().deepSerialize(hi,writer);
		    writer.flush();
		    writer.close();
		    Reader reader = new BufferedReader(new FileReader(new File(filesDir,"temp.json")));
		    //Log.d("UtilityFuncs", "Serialized = " + s);
		    ArrayList<TransmittablePacket> response = new JSONDeserializer<ArrayList<TransmittablePacket>>().deserialize(reader, ArrayList.class );
		    Log.d("UtilityFuncs", response.size() + " transmission packets!");
		    for (TransmittablePacket tp : response)
		    	Log.d("UtilityFuncs", tp.toString());
		 } catch (Exception e) {
			 e.printStackTrace();
		 }
	}
	
	public static String getHash(String initial) {
		MessageDigest digest=null;
		try {
		    digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		}
		digest.reset();
		return bin2hex(digest.digest(initial.getBytes()));
	}
	
	public static String bin2hex(byte[] data) {
		return String.format("%0" + (data.length * 2) + 'x', new BigInteger(1, data));
	}
	
	public static void dumpFileInfo(File f) {
		Log.d("UtilityFuncs", "dumping file info");
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String input = null;
			while ((input = br.readLine()) != null) {
				Log.d("UtilityFuncs", input);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
