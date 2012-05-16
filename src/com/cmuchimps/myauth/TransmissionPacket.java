package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class TransmissionPacket {
	public String qtext;
	public HashMap<String,String> question;
	public ArrayList<HashMap<String,String>> answers;
	public String user_answer;
	public HashMap<String,String> supplementary_responses;
	public String timestamp;
	
	public TransmissionPacket() {
		initialize("", new HashMap<String,String>(), new ArrayList<HashMap<String,String>>(), "", new HashMap<String,String>(), "");
	}
	
	public TransmissionPacket(String qt, HashMap<String,String> qs, ArrayList<HashMap<String,String>> as, String ua, HashMap<String,String> supp, String ts) {
		initialize(qt,qs,as,ua,supp,ts);
	}
	
	private void initialize(String qt, HashMap<String,String> qs, ArrayList<HashMap<String,String>> as, String ua, HashMap<String,String> supp, String ts) {
		qtext = new String(qt);
		question = UtilityFuncs.duplicateMap(qs);
		answers = new ArrayList<HashMap<String,String>>();
		for (HashMap<String,String> answer : as)
			answers.add(UtilityFuncs.duplicateMap(answer));
		user_answer = new String(ua);
		supplementary_responses = UtilityFuncs.duplicateMap(supp);
		timestamp = new String(ts);
	}
	
	public void addQKey(String key, String value) {
		question.put(key, value);
	}
	
	public void addAnswer(HashMap<String,String> a) {
		answers.add(UtilityFuncs.duplicateMap(a));
	}
	
	public void addSupp(String key, String value) {
		supplementary_responses.put(key, value);
	}
	
	public HttpParams convertToParams() {
		HttpParams retVal = new BasicHttpParams();
		retVal.setParameter("qtext", qtext);
		retVal.setParameter("user_answer", user_answer);
		retVal.setParameter("timestamp", timestamp);
		for (String qkey : question.keySet()) {
			retVal.setParameter("question." + qkey, question.get(qkey));
		}
		int i = 0;
		for (HashMap<String,String> answer : answers) {
			for (String akey : answer.keySet()) {
				retVal.setParameter("answer." + i + "." + akey, answer.get(akey));
			}
			i++;
		}
		String[] supp_keys = supplementary_responses.keySet().toArray(new String[supplementary_responses.size()]);
		Arrays.sort(supp_keys);
		for (i = 0; i < supp_keys.length; i++) {
			retVal.setParameter("supp."+i, supplementary_responses.get(supp_keys[i]));
		}
		return retVal;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\n");
		sb.append("\tQtext: " + qtext + "\n");
		sb.append("\tQuestion_Meta:\n\t{\n");
		for (String qkey : question.keySet()) {
			sb.append("\t\t" + qkey + ":" + question.get(qkey) + "\n");
		}
		sb.append("\t}\n");
		sb.append("\tAnswers:\n\t{\n");
		for (HashMap<String,String> answer : answers) {
			sb.append("\t\tAnswer:\n\t\t{\n");
			for (String akey : answer.keySet()) {
				sb.append("\t\t\t" + akey + ":" + answer.get(akey) + "\n");
			}
			sb.append("\t\t}\n");
		}
		sb.append("\t}\n");
		sb.append("\tUserAnswer: " + user_answer + "\n");
		sb.append("\tSupplementary_Responses:\n\t{\n");
		for (String skey : supplementary_responses.keySet()) {
			sb.append("\t\t" + skey + ":" + supplementary_responses.get(skey) + "\n");
		}
		sb.append("\t}\n");
		sb.append("\tTimestamp: " + timestamp + "\n");
		sb.append("}\n");
		return sb.toString();
	}
}
