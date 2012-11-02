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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.util.Log;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class UtilityFuncs {
	public static final String DEBUG_TAG = "UtilityFuncs";
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
		//Log.d("UtilityFuncs", sb.toString());
		return sb.toString();
	}
	
	public static String joinInCaluse(ArrayList<Long> composite) {
		if (composite.size() <= 0) return "(-1)";
		StringBuffer sb = new StringBuffer("(" + composite.get(0));
		for (int i = 1; i < composite.size(); i++)
			sb.append("," + composite.get(i));
		sb.append(")");
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
	
	public static TransmissionPacket makeTestPacket(DataWrapper.Fact f) {
		HashMap<String,String> qs = new HashMap<String,String>();
		qs.put("qid", "2");
		qs.put("timestamp", f.getTimestamp());
		qs.put("qatid","6");
		ArrayList<HashMap<String,String>> answers = new ArrayList<HashMap<String,String>>();
		HashMap<String,String> answer = new HashMap<String,String>();
		answer.put("timestamp", f.getTimestamp());
		answer.put("value", f.getTagAt(1).getSV());
		answer.put("correct", "yes");
		answer.put("tag_metas", UtilityFuncs.JSONify(f.getAllExcept(0)));
		answers.add(answer);
		HashMap<String,String> supp = new HashMap<String,String>();
		supp.put("How easy was it for you to recall the answer to this question?", "5");
		supp.put("How confident are you in your answer?", "5");
		TransmissionPacket temp = new TransmissionPacket(0,"kash","Who what when where how at 11am?",qs,answers,"abc",supp,"now", 1000l, false);
		temp.setTypeToDebug();
		return temp;
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
			TransmissionPacket temp = new TransmissionPacket(0,"kash","Who what when where how at 11am?",qs,answers,"abc",supp,"now", 1000l, false);
			User us = new User(filesDir,"Ced","ced@ceds.com",23,"Black","Male");
			ArrayList<TransmittablePacket> hi = new ArrayList<TransmittablePacket>();
			hi.add(temp);
			hi.add(us);
			//Log.d("UtilityFuncs", "Before:");
			//Log.d("UtilityFuncs", temp.toString());
			Writer writer = new BufferedWriter(new FileWriter(new File(filesDir,"temp.json")));
		    new JSONSerializer().deepSerialize(hi,writer);
		    writer.flush();
		    writer.close();
		    Reader reader = new BufferedReader(new FileReader(new File(filesDir,"temp.json")));
		    //Log.d("UtilityFuncs", "Serialized = " + s);
		    ArrayList<TransmittablePacket> response = new JSONDeserializer<ArrayList<TransmittablePacket>>().deserialize(reader, ArrayList.class );
		    //Log.d("UtilityFuncs", response.size() + " transmission packets!");
		    /*for (TransmittablePacket tp : response)
		    	Log.d("UtilityFuncs", tp.toString());*/
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
	
	public static double shannonEntropy(List<Long> values) {
		HashMap<Long, Integer> ft = new HashMap<Long, Integer>();
		for (Long l : values) {
			int counter = (ft.containsKey(l) ? ft.get(l) : 0);
			ft.put(l, counter + 1);
		}
		
		double result = 0.0;
		for (Long l : ft.keySet()) {
			double PrX = ((double)ft.get(l))/values.size();
			result += PrX*(Math.log10(PrX) / Math.log10(2));
		}
		
		return -result;
	}
	
	public static String bin2hex(byte[] data) {
		return String.format("%0" + (data.length * 2) + 'x', new BigInteger(1, data));
	}
	
	public static void dumpFileInfo(File f) {
		//Log.d("UtilityFuncs", "dumping file info");
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String input = null;
			/*while ((input = br.readLine()) != null) {
				//Log.d("UtilityFuncs", input);
			}*/
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String getURLHost(String url) {
		String[] prefixes = {
				"www.", 
				"m."
		};
		if (!url.startsWith("http://") && !url.startsWith("https://"))
			url = "http://" + url;
		try {
			url = new URL(url).getHost();
		} catch (MalformedURLException e) {
			//Log.d("UtilityFuncs-getURLHost", "Unable to parse URL");
		}
		for (String pre : prefixes) {
			if (url.startsWith(pre))
				url = url.substring(pre.length());
		}
		return url;
	}
	
	public static void quicksort(double[] main, int[] index) {
	    quicksort(main, index, 0, index.length - 1);
	}

	// quicksort a[left] to a[right]
	public static void quicksort(double[] a, int[] index, int left, int right) {
	    if (right <= left) return;
	    int i = partition(a, index, left, right);
	    quicksort(a, index, left, i-1);
	    quicksort(a, index, i+1, right);
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition(double[] a, int[] index, 
	int left, int right) {
	    int i = left - 1;
	    int j = right;
	    while (true) {
	        while (less(a[++i], a[right]))      // find item on left to swap
	            ;                               // a[right] acts as sentinel
	        while (less(a[right], a[--j]))      // find item on right to swap
	            if (j == left) break;           // don't go out-of-bounds
	        if (i >= j) break;                  // check if pointers cross
	        exch(a, index, i, j);               // swap two elements into place
	    }
	    exch(a, index, i, right);               // swap with partition element
	    return i;
	}

	// is x < y ?
	private static boolean less(double x, double y) {
	    return (x < y);
	}

	// exchange a[i] and a[j]
	private static void exch(double[] a, int[] index, int i, int j) {
	    double swap = a[i];
	    a[i] = a[j];
	    a[j] = swap;
	    int b = index[i];
	    index[i] = index[j];
	    index[j] = b;
	}
	
	public static ArrayList<Integer> randomizeWithinEquivalence(ArrayList<Double> val) {
		HashMap<Double, ArrayList<Integer>> equivalenceClasses = new HashMap<Double,ArrayList<Integer>>();
		for (int i = 0; i < val.size(); i++) {
			Double d = val.get(i);
			ArrayList<Integer> allIndices = (equivalenceClasses.containsKey(d) ? 
				equivalenceClasses.get(d) : 
				new ArrayList<Integer>());
			allIndices.add(i);
			equivalenceClasses.put(d, allIndices);
		}
		ArrayList<Integer> retVal = new ArrayList<Integer>();
		
		Double[] uniqueVals = equivalenceClasses.keySet().toArray(new Double[equivalenceClasses.size()]);
		Arrays.sort(uniqueVals);
		for (Double d : uniqueVals) {
			ArrayList<Integer> dIndices = equivalenceClasses.get(d);
			Collections.shuffle(dIndices);
			retVal.addAll(dIndices);
		}
		return retVal;
	}
	
	public static ArrayList<Integer> randomizeWithinEquivalence(double[] val) {
		HashMap<Double, ArrayList<Integer>> equivalenceClasses = new HashMap<Double,ArrayList<Integer>>();
		for (int i = 0; i < val.length; i++) {
			Double d = val[i];
			ArrayList<Integer> allIndices = (equivalenceClasses.containsKey(d) ? 
				equivalenceClasses.get(d) : 
				new ArrayList<Integer>());
			allIndices.add(i);
			equivalenceClasses.put(d, allIndices);
		}
		ArrayList<Integer> retVal = new ArrayList<Integer>();
		
		Double[] uniqueVals = equivalenceClasses.keySet().toArray(new Double[equivalenceClasses.size()]);
		Arrays.sort(uniqueVals);
		for (Double d : uniqueVals) {
			ArrayList<Integer> dIndices = equivalenceClasses.get(d);
			Collections.shuffle(dIndices);
			retVal.addAll(dIndices);
		}
		return retVal;
	}
	
	public static Long[] sample(Long[] orig, int n) {
		Collections.shuffle(Arrays.asList(orig));
		Long[] retVal = new Long[(orig.length > n ? n : orig.length)];
		for (int i = 0; i < retVal.length; i++) retVal[i] = orig[i];
		return retVal;
	}
	
	public static String JSONify(DataWrapper.Tag[] tags) {
		StringBuilder sb = new StringBuilder("{");
		if (tags.length > 0) {
			sb.append("\"" + tags[0].getTC() + ":" + tags[0].getSC() + "\":\"" + tags[0].getSV() + "\"");
			for (int i = 1; i < tags.length; i++) {
				sb.append(",\"" + tags[i].getTC() + ":" + tags[i].getSC() + "\":\"" + tags[i].getSV() + "\"");
			}
		}
		sb.append("}");
		return sb.toString();
	}
	
	public static boolean invalidTime(DataWrapper.Fact fact) {
		//Log.d(DEBUG_TAG, fact.getTimestamp());
		try {
			int ts = Integer.parseInt(fact.getTimestamp().split(" ")[1].split(":")[0]);
			//Log.d(DEBUG_TAG, ""+ts);
			return ts >= 2 && ts <= 7;
		} catch (Exception e) {
			return true;
		}
	}
	//yyyy-MM-dd kk:mm:ss
	public static long getLongTimeForFact(DataWrapper.Fact fact) {
		String ts = fact.getTimestamp();
		String[] top_level = ts.split(" ");
		try {
			String[] ymd = top_level[0].split("-");
			String[] hms = top_level[1].split(":");
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, Integer.parseInt(ymd[0]));
			cal.set(Calendar.MONTH, Integer.parseInt(ymd[1]));
			cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(ymd[2]));
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hms[0]));
			cal.set(Calendar.MINUTE, Integer.parseInt(hms[1]));
			cal.set(Calendar.SECOND, Integer.parseInt(hms[2]));
			return cal.getTimeInMillis();
		} catch (Exception e) {
			return -1l;
		}
	}
	
	public static boolean lastNSame(ArrayList<Long> arr, int n) {
		if (arr.size() < n) return false;
		for (int i = arr.size() - n + 1; i < arr.size(); i++) {
			if (arr.get(i-1) != arr.get(i)) return false;
		}
		return true;
	}
}