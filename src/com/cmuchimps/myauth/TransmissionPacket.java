package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class TransmissionPacket extends TransmittablePacket {
	public long response_id;
	public String user_id; //the unique_id of the user
	public String qtext; //the verbatim question text
	public HashMap<String,String> question; //meta-values about the question such as the time and qatid
	public ArrayList<HashMap<String,String>> answers; //answer values, with timestamp. additionally, the correct answer will have the exactly correct field
	public String user_answer; //the user's answer
	public HashMap<String,String> supplementary_responses; //the answers to the supplementary questions
	public String timestamp; //the time the user answered the question
	public long amountTime; //TODO: the amount of time it took the user to answer the question
	public boolean isRecog;
	
	public TransmissionPacket() {
		initialize(-1,"","", new HashMap<String,String>(), new ArrayList<HashMap<String,String>>(), "", 
				new HashMap<String,String>(), "", 0l, false);
	}
	
	public TransmissionPacket(long rid,String uid,String qt, HashMap<String,String> qs, 
			ArrayList<HashMap<String,String>> as, String ua, HashMap<String,String> supp, 
			String ts, long at, boolean ir) {
		initialize(rid,uid,qt,qs,as,ua,supp,ts, at, ir);
	}
	
	private void initialize(long rid,String uid,String qt, HashMap<String,String> qs, 
			ArrayList<HashMap<String,String>> as, String ua, HashMap<String,String> supp, 
			String ts, long at, boolean ir) {
		response_id = rid;
		typeid = 1;
		user_id = uid;
		qtext = new String(qt);
		question = UtilityFuncs.duplicateMap(qs);
		answers = new ArrayList<HashMap<String,String>>();
		for (HashMap<String,String> answer : as)
			answers.add(UtilityFuncs.duplicateMap(answer));
		user_answer = new String(ua);
		supplementary_responses = UtilityFuncs.duplicateMap(supp);
		timestamp = new String(ts);
		amountTime = at;
		isRecog = true;
	}
	
	public void setTypeToDebug() {
		typeid = -1;
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
		retVal.setParameter("rid", response_id);
		retVal.setParameter("type", typeid);
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
	
	public List<NameValuePair> convertToNVP() {
		List<NameValuePair> retVal = new ArrayList<NameValuePair>();
		retVal.add(new BasicNameValuePair("rid",""+response_id));
		retVal.add(new BasicNameValuePair("user_id", user_id));
        retVal.add(new BasicNameValuePair("type", ""+typeid));
        retVal.add(new BasicNameValuePair("qtext", qtext));
        retVal.add(new BasicNameValuePair("user_answer", user_answer));
        retVal.add(new BasicNameValuePair("timestamp", timestamp));
        retVal.add(new BasicNameValuePair("amountTime",""+amountTime));
		retVal.add(new BasicNameValuePair("isRecog",""+isRecog));
		
        for (String qkey : question.keySet()) {
			retVal.add(new BasicNameValuePair("question_" + qkey, question.get(qkey)));
		}
		int i = 0;
		retVal.add(new BasicNameValuePair("num_answers",""+answers.size()));
		for (HashMap<String,String> answer : answers) {
			for (String akey : answer.keySet()) {
				retVal.add(new BasicNameValuePair("answer" + i + "_" + akey, answer.get(akey)));
			}
			i++;
		}
		String[] supp_keys = supplementary_responses.keySet().toArray(new String[supplementary_responses.size()]);
		Arrays.sort(supp_keys);
		for (i = 0; i < supp_keys.length; i++) {
			retVal.add(new BasicNameValuePair("supp"+i, supplementary_responses.get(supp_keys[i])));
		}
        //httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        return retVal;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\n");
		sb.append("\tResponseID: " + response_id + "\n");
		sb.append("\tUserID: " + user_id + "\n");
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
