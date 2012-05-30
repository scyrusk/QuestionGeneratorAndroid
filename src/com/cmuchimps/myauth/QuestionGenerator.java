package com.cmuchimps.myauth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.cmuchimps.myauth.DataWrapper.Fact;
import com.cmuchimps.myauth.DataWrapper.Meta;
import com.cmuchimps.myauth.DataWrapper.QAT;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class QuestionGenerator {
	private final String NEXT_ID_FILE = "nextid.json";
	private KBDbAdapter mDbHelper;
	private DataWrapper dw;
	private File mFilesDir;
	
	private IDTracker mIDTrack;
	private boolean mLoadID = true;
	
	//lazy loaded
	private int getNextID(boolean qid) throws IOException {
		if (mLoadID) {
			if (new File(mFilesDir,NEXT_ID_FILE).exists()) mIDTrack = new JSONDeserializer<IDTracker>().deserialize(new BufferedReader(new FileReader(new File(mFilesDir, NEXT_ID_FILE))), IDTracker.class );
			else {
				mIDTrack = new IDTracker();
			}
			mLoadID = false;
		}
		int retVal = (qid ? mIDTrack.nextQID++ : mIDTrack.nextAID++);
		return retVal;
	}
	
	public void serializeIDTrack() throws IOException {
		Writer writer = new BufferedWriter(new FileWriter(new File(mFilesDir, NEXT_ID_FILE)));
		try {
			new JSONSerializer().deepSerialize(mIDTrack,writer);
			writer.flush();
		} finally {
			writer.close();
		}
	}
	
	public class QuestionAnswerPair {
		private String question;
		private HashMap<String,Float> answers;
		private HashMap<String,String> question_metas; //should contain at least timestamp
		private ArrayList<HashMap<String,String>> answer_metas;
		
		public QuestionAnswerPair() {
			initialize("", new HashMap<String,Float>(), new HashMap<String,String>(), new ArrayList<HashMap<String,String>>());
		}
		
		public QuestionAnswerPair(String q, HashMap<String,Float> as, HashMap<String,String> qms, ArrayList<HashMap<String,String>> ams) {
			initialize(q, as, qms, ams);
		}
		
		public QuestionAnswerPair(String q, ArrayList<String> as, HashMap<String,String> qms, ArrayList<HashMap<String,String>> ams) {
			HashMap<String,Float> acouples = new HashMap<String,Float>();
			for (String a : as)
				acouples.put(a, 0.5f);
			initialize(q,acouples,qms,ams);
		}
		
		private void initialize(String q, HashMap<String,Float> as,HashMap<String,String> qms,ArrayList<HashMap<String,String>> ams) {
			question = q;
			answers = as;
			question_metas = qms;
			answer_metas = ams;
		}
		
		public Float get(String a) {
			return (answers.get(a) == null ? 0.0f : answers.get(a));
		}
		
		public String getQuestion() { return question; }
		public HashMap<String,Float> getAllAnswers() { return answers; }
		public HashMap<String,String> getQuestionMetas() {
			return UtilityFuncs.duplicateMap(question_metas);
		}
		public ArrayList<HashMap<String,String>> getAnswerMetas() {
			ArrayList<HashMap<String,String>> retVal = new ArrayList<HashMap<String,String>>();
			for (HashMap<String,String> hm : answer_metas) {
				retVal.add(UtilityFuncs.duplicateMap(hm));
			}
			return retVal;
		}
		public ArrayList<String> getAnswers() { return new ArrayList<String>(answers.keySet()); }
		public boolean matches(String answer) {
			for (String potential : answers.keySet()) {
				if (potential.equalsIgnoreCase(answer)) return true;
			}
			return false;
		}
	}
	
	public QuestionGenerator(KBDbAdapter kbdb, DataWrapper ddub, File filesDir) {
		mDbHelper = kbdb;
		dw = ddub;
		mFilesDir = filesDir;
	}
	
	public void testQATagainstFacts(QAT theQat,List<Fact> facts) {
		for (Fact fact : facts) {
			Integer[] result = theQat.matches(fact);
			if (result != null) {
				System.out.println("matches!");
			} else {
				System.out.println("no match!");
			}
		}
	}
	
	public QuestionAnswerPair askQuestion() {
		/*
		 * Filter knowledge base facts by the past 24 hours
		 * Compress all filtered facts into distinct type
		 * Randomly select type of fact to ask about
		 * Randomly select specific fact out of the randomly selected type
		 * Find matching QATs
		 * Randomly select QAT out of those that match
		 * Fill in the blanks
		 */
		Long[] qats = mDbHelper.getAllQATs();
		Long[] afacts = mDbHelper.getAllFacts();
		ArrayList<Long> facts = new ArrayList<Long>();
		//System.out.println("length of afacts:" + afacts.length);
		//Long[] temp = mDbHelper.getFilteredFactsByTime(24*60*60*1000, mDbHelper.getFact(afacts[MyAuthActivity.r.nextInt(afacts.length)]).getTimestamp());
		Long[] temp = mDbHelper.getFilteredFactsByTime(24*60*60*1000, "*");
		//Long[] temp = afacts;
		HashMap<Fact,ArrayList<Long>> factCompression = new HashMap<Fact,ArrayList<Long>>();
		for (Long l : temp) {
			Fact curr = mDbHelper.getFact(l);
			Fact matched = null;
			for (Fact existing : factCompression.keySet()) {
				if (existing.sameStructure(curr, true)) {
					matched = existing;
					break;
				}
			}
			ArrayList<Long> toAdd = (matched == null? new ArrayList<Long>() : factCompression.get(matched));
			Fact key = (matched == null ? curr : matched);
			toAdd.add(l);
			factCompression.put(key, toAdd);	
		}
		//Long[] temp = afacts;
		for (Long l : temp) { facts.add(l); }
		System.out.println("Sup, here are the different types of facts (" + factCompression.size() + "):");
		for (Fact f : factCompression.keySet()) {
			System.out.println(f);
			System.out.println("****" + factCompression.get(f).size() + "****");
		}
		//System.out.println("Number of qualifying facts: " + facts.size());
		while (facts.size() > 0) {
			int index = MyAuthActivity.r.nextInt(factCompression.size());
			ArrayList<Long> simFactStructures = factCompression.get(factCompression.keySet().toArray(new Fact[factCompression.size()])[index]);
			index = MyAuthActivity.r.nextInt(simFactStructures.size());
			Fact fact = mDbHelper.getFact(simFactStructures.get(index));
			HashMap<Long,Integer[]> fqats = new HashMap<Long,Integer[]>(); //qat index => matching matrix
			System.out.println(fact.toString(0));
	        for (Long l : qats) {
	        	Integer[] result = mDbHelper.getQAT(l).matches(fact);
	        	if (result != null) {
	        		fqats.put(l, result);
	        	}
	        }
			if (fqats.size() == 0) { //no matches
				facts.remove(index);
				continue;
			} else {
				//generate question answer pair
				//first pick qat at random
				HashSet<Fact> simFacts = new HashSet<Fact>();
				ArrayList<Long> possQats = new ArrayList<Long>(fqats.keySet());
				int qindex = MyAuthActivity.r.nextInt(fqats.size());
				QAT theQAT = mDbHelper.getQAT(possQats.get(qindex));
				Integer[] matches = fqats.get(possQats.get(qindex));
				//now that we have the QAT and fact tag matches, start filling in the blanks
				String question = theQAT.getQText();
				HashMap<String,Float> answers = new HashMap<String,Float>();
				answers.put(fact.getTagAt(matches[0]).getSV(), 0.5f);
				//first check to see if the answer has the "list" or "anysubclass" meta
				ArrayList<String> acond_descs = new ArrayList<String>();
				for (String s : theQAT.getAcond().getAllDescriptions()) { acond_descs.add(s); };
				
				//strictly speaking, can do this only if acond_descs.contains("list") in real system
				//doing it here without conditional for analysis purposes (will send all possible answers in the past day)
				//get similar facts
				for (Long l : facts) {
					Fact comp = mDbHelper.getFact(l);
					if (fact.sameStructure(comp, !acond_descs.contains("anysubclass"))) {
						simFacts.add(comp);
					}
				}
				
				//Currently context unaware, so assign constant float value to answers
				if (acond_descs.contains("list")) {
					for (Fact f : simFacts) {
						answers.put(f.getTagAt(matches[0]).getSV(),0.5f);
					}
				}
				//also mark which Acond tag condition has been matched with the answer
				/*int qat_acond_chosen = -1;
				for (int i = 0; i < theQAT.getAcond().getAllTags().length; i++) {
					Tag s = theQAT.getAcond().getAllTags()[i];
					if (s.getTC().equalsIgnoreCase(fact.getTagAt(matches[0]).getTC())) {
						if (s.getSC().equalsIgnoreCase("*") || s.getSC().equalsIgnoreCase(fact.getTagAt(matches[0]).getSC())) {
							qat_acond_chosen = i;
							break;
						}
					}
				}*/
				//Substitute qconds appropriately
				for (int i = 1; i < matches.length; i++) {
					int refVal = i - 1;
					//replace all instances of {refVal} in qtext with fact.getTagsAt(i).getSV();
					question.replaceAll("\\{" + refVal + "\\}", fact.getTagAt(i).getSV());
				}
				//Substitute substructs
				question = question.replaceAll("\\(acond-subclass\\)",fact.getTagAt(matches[0]).getSC());
				//substruct time
				int idxOfTime = question.indexOf("(time");
				int endIndex = question.indexOf(")",idxOfTime);
				String tsubs = question.substring(idxOfTime+1,endIndex);
				String[] timeformat = tsubs.split(":");
				if (timeformat.length == 1 || timeformat[1].equalsIgnoreCase("date")) {
					question = question.replaceAll("\\(time:?.*\\)", fact.getTimestamp().split("[T ]")[0]);
				} else {
					question = question.replaceAll("\\(time:?.*\\)", this.formatTimeString(fact, timeformat[1]));
				}
				
				HashMap<String,String> question_metas = new HashMap<String,String>();
				int nextQID;
				try {
					nextQID = getNextID(true);
				} catch (IOException e) {
					nextQID = MyAuthActivity.r.nextInt(Integer.MAX_VALUE);
				}
				question_metas.put("qid", ""+ nextQID);
				question_metas.put("timestamp", fact.getTimestamp());
				question_metas.put("qatid", "" + theQAT.getID());
				for (Meta m : theQAT.getAllMetas()) {
					question_metas.put(m.getName(), m.getValue());
				}
				
				ArrayList<HashMap<String,String>> answer_metas = new ArrayList<HashMap<String,String>>();
				HashMap<String,String> answer_meta = new HashMap<String,String>();
				int nextAID;
				try {
					nextAID = getNextID(true);
				} catch (IOException e) {
					nextAID = MyAuthActivity.r.nextInt(Integer.MAX_VALUE);
				}
				answer_meta.put("aid", ""+nextAID);
				answer_meta.put("timestamp",fact.getTimestamp());
				answer_meta.put("value", fact.getTagAt(matches[0]).getSV());
				answer_meta.put("correct", "yes");
				answer_metas.add(answer_meta);
				for (Fact f : simFacts) {
					try {
						nextAID = getNextID(true);
					} catch (IOException e) {
						nextAID = MyAuthActivity.r.nextInt(Integer.MAX_VALUE);
					}
					answer_meta = new HashMap<String,String>();
					answer_meta.put("aid", ""+nextAID);
					answer_meta.put("timestamp", f.getTimestamp());
					answer_meta.put("value", f.getTagAt(matches[0]).getSV());
					answer_meta.put("correct", "no");
				}
				return new QuestionAnswerPair(question,answers,question_metas,answer_metas);
			}
		}
		
		System.out.println("Returning null, no facts found");
		return null;
	}
	
	public String formatTimeString(Fact fact,String timeformat) {
		StringBuffer retVal = new StringBuffer();
		String[] components = timeformat.split(" ");
		boolean first = false;
		for (String s : components) {
			if (first) first = false;
			else retVal.append(" ");
			
			if (s.equalsIgnoreCase("date")) {
				retVal.append(fact.getTimestamp().split("[T ]")[0]);
			} else if (s.equalsIgnoreCase("dow")) {
				//day of week
				retVal.append(fact.getDayOfWeek());
			} else if (s.equalsIgnoreCase("hod")) {
				//hour of day
				String hod = fact.getTimestamp().split("[T ]")[1].split(":")[0];
				int i = Integer.parseInt(hod);
				System.out.println("Hour of day val:" + i);
				String ampm = "am";
				if (i >= 12) {
					ampm = "pm";
					i -= 12;
				}
				retVal.append((i == 0 ? "midnight" : (i == 12 ? "noon" : i + ampm)));
			} else {
				retVal.append(s);
			}
		}
		return retVal.toString();
	}
}
