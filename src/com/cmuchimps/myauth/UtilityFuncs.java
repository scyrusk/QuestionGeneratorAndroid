package com.cmuchimps.myauth;

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
}
