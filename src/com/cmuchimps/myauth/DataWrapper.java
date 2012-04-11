package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.HashMap;

public class DataWrapper {
	public HashMap<String,String> atomicElements; //key => single value
	public HashMap<String,ArrayList<String>> descriptors; //key => list of single values
	public HashMap<String,HashMap<String,String>> complexTypes; //key => hashmap of complex types
	
}
