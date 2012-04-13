package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class UtilityFuncs {
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
}
