package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

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
		System.out.println(sb.toString());
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
}
