package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;

public class SkipQuestionPacket extends TransmittablePacket {
	public static final int typeid = 2;
	public int response_id;
	public boolean choice_uncomfortable;
	public boolean choice_cant_remember;
	public boolean choice_dont_understand;
	public boolean choice_other;
	public String qtext; //the verbatim question text
	public HashMap<String,String> question; //meta-values about the question such as the time and qatid
	public String explanation;
	public String timestamp;
	public String user_id;
	public long amountTime;
	public boolean isRecog;
	
	public SkipQuestionPacket() {
		initialize(-1,"", new HashMap<String,String>(), false, false, false, false, "", "", "", false, 0l);
	}
	
	public SkipQuestionPacket(int rid,String qt, HashMap<String,String> qms, boolean c1, 
			boolean c2, boolean c3, boolean c4, String e, String ts, String uid, boolean ir,
			long at) {
		initialize(rid, qt, qms, c1, c2, c3, c4, e, ts, uid, ir, at);
	}
	
	private void initialize(int rid, String qt, HashMap<String,String> qms, boolean c1, 
			boolean c2, boolean c3, boolean c4, String e, String ts, String uid, boolean ir,
			long at) {
		response_id = rid;
		qtext = qt;
		question = UtilityFuncs.duplicateMap(qms);
		choice_uncomfortable = c1;
		choice_cant_remember = c2;
		choice_dont_understand = c3;
		choice_other = c4;
		explanation = e;
		timestamp = ts;
		user_id = uid;
		isRecog = ir;
		amountTime = at;
	}
	
	@Override
	public HttpParams convertToParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NameValuePair> convertToNVP() {
		List<NameValuePair> retVal = new ArrayList<NameValuePair>();
		retVal.add(new BasicNameValuePair("rid", "" + response_id));
		retVal.add(new BasicNameValuePair("user_id", user_id));
		retVal.add(new BasicNameValuePair("type", "" + typeid));
		retVal.add(new BasicNameValuePair("choice_uncomfortable", "" + this.choice_uncomfortable));
		retVal.add(new BasicNameValuePair("choice_cant_remember", "" + this.choice_cant_remember));
		retVal.add(new BasicNameValuePair("choice_dont_understand", "" + this.choice_dont_understand));
		retVal.add(new BasicNameValuePair("choice_other", "" + this.choice_other));
		retVal.add(new BasicNameValuePair("explanation", this.explanation));
		retVal.add(new BasicNameValuePair("timestamp", this.timestamp));
		retVal.add(new BasicNameValuePair("qtext", qtext));
		retVal.add(new BasicNameValuePair("isRecog",""+isRecog));
		retVal.add(new BasicNameValuePair("amountTime", ""+amountTime));
		
		for (String qkey : question.keySet()) {
			retVal.add(new BasicNameValuePair("question_" + qkey, question.get(qkey)));
		}
		return retVal;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\n");
		sb.append("\tResponseID: " + response_id + "\n");
		sb.append("\tType: " + typeid + "\n");
		sb.append("\tUserID: " + user_id + "\n");
		sb.append("\tQtext: " + qtext + "\n");
		sb.append("\tQuestion_Meta:\n\t{\n");
		for (String qkey : question.keySet()) {
			sb.append("\t\t" + qkey + ":" + question.get(qkey) + "\n");
		}
		sb.append("\t}\n");
		sb.append("\tChoice Uncomfortable: " + choice_uncomfortable + "\n");
		sb.append("\tChoice Can't Remember: " + choice_cant_remember + "\n");
		sb.append("\tChoice Don't Understand: " + choice_dont_understand + "\n");
		sb.append("\tChoice Other: " + choice_other + "\n");
		sb.append("\tExplanation: " + explanation + "\n");
		sb.append("\tTimestamp: " + timestamp + "\n");
		sb.append("}\n");
		return sb.toString();
	}

}
