package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Wrapper class to easily filter facts according to various criteria.
 * @author sauvikd
 *
 */
public class KnowledgeBase {
	private KBDbAdapter mDbHelper;
	
	public KnowledgeBase(KBDbAdapter db) {
		this.mDbHelper = db;
	}
	
	public static String printFact(HashMap<String,Object> fact) {
		StringBuffer retVal = new StringBuffer();
        for (String s : fact.keySet()) {
        	retVal.append(s + " => ");
        	Object val = fact.get(s);
        	if (val instanceof ArrayList) {
        		ArrayList<HashMap<String,String>> maps = (ArrayList<HashMap<String,String>>)val;
        		retVal.append("{");
        		for (HashMap<String,String> map : maps) {
        			retVal.append("[ ");
        			for (String key : map.keySet()) {
        				retVal.append(key + "=" + map.get(key) + ",");
        			}
        			retVal.append("] ");
        		}
        		retVal.append("}\n");
        	} else {
        		retVal.append(val+"\n");
        	}
        }
        return retVal.toString();
	}
	
	public static String printQAT(HashMap<String,Object> qat) {
		if (qat == null) return "";
		StringBuffer retVal = new StringBuffer();
		for (String s : qat.keySet()) {
			retVal.append(s + " => ");
			Object val = qat.get(s);
        	if (val instanceof ArrayList && ((ArrayList)val).size() > 0) {
        		HashMap workingVal = (HashMap)(((ArrayList)val).get(0));
        		if (workingVal.values().iterator().next() instanceof String) {
        			//metas, ArrayList<HashMap<String,String>>
	        		ArrayList<HashMap<String,String>> maps = (ArrayList<HashMap<String,String>>)val;
	        		retVal.append("{");
	        		for (HashMap<String,String> map : maps) {
	        			retVal.append("[ ");
	        			for (String key : map.keySet()) {
	        				retVal.append(key + "=" + map.get(key) + ",");
	        			}
	        			retVal.append("] ");
	        		}
	        		retVal.append("}\n");
        		} else {
        			//qconds, ArrayList<HashMap<String,Object>>
        			ArrayList<HashMap<String,Object>> qconds = (ArrayList<HashMap<String,Object>>)val;
        			retVal.append("\n{\n");
        			for (HashMap<String,Object> qcond : qconds) {
        				for (String key : qcond.keySet()) {
        					if (qcond.get(key) instanceof ArrayList<?>) {
        						//tags
        						ArrayList<HashMap<String,String>> maps = (ArrayList<HashMap<String,String>>)qcond.get(key);
        						retVal.append("{");
        		        		for (HashMap<String,String> map : maps) {
        		        			retVal.append("[ ");
        		        			for (String skey : map.keySet()) {
        		        				retVal.append(skey + "=" + map.get(skey) + ",");
        		        			}
        		        			retVal.append("] ");
        		        		}
        		        		retVal.append("}\n");
        					} else {
        						retVal.append(key+"="+qcond.get(key));
        					}
        				}
        			}
        			retVal.append("}\n");
        		}
        	} else if (val instanceof HashMap) {
        		//aconds, HashMap<String,Object>>
        		HashMap<String,Object> acond = (HashMap<String,Object>)val;
        		for (String key : acond.keySet()) {
					if (acond.get(key) instanceof ArrayList<?> && ((ArrayList)acond.get(key)).size() > 0) {
						if (((ArrayList)acond.get(key)).get(0) instanceof HashMap) {
							ArrayList<HashMap<String,String>> maps = (ArrayList<HashMap<String,String>>)acond.get(key);
							retVal.append("{");
			        		for (HashMap<String,String> map : maps) {
			        			retVal.append("[ ");
			        			for (String skey : map.keySet()) {
			        				retVal.append(skey + "=" + map.get(skey) + ",");
			        			}
			        			retVal.append("] ");
			        		}
			        		retVal.append("}\n");
						} else {
							//descriptions
							ArrayList<String> descriptions = (ArrayList<String>)acond.get(key);
							retVal.append("{");
							for (String d : descriptions) {
								retVal.append(d+",");
							}
							retVal.append("}\n");
						}
					}
				}
        	} else {
        		retVal.append(val+"\n");
        	}
		}
		return retVal.toString();
	}
	
	public HashMap<String,Object> reconstructQAT(long qat_id) {
		HashMap<String,Object> retVal = new HashMap<String,Object>();
		
		return retVal;
	}
}
