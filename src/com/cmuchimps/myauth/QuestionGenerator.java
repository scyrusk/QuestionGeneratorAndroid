package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.cmuchimps.myauth.DataWrapper.Fact;
import com.cmuchimps.myauth.DataWrapper.QAT;
import com.cmuchimps.myauth.DataWrapper.Tag;

public class QuestionGenerator {
	private KBDbAdapter mDbHelper;
	private DataWrapper dw;
	
	public class QuestionAnswerPair {
		private String question;
		private HashMap<String,Float> answers;
		
		public QuestionAnswerPair() {
			initialize("", new HashMap<String,Float>());
		}
		
		public QuestionAnswerPair(String q, HashMap<String,Float> as) {
			initialize(q, as);
		}
		
		public QuestionAnswerPair(String q, ArrayList<String> as) {
			HashMap<String,Float> acouples = new HashMap<String,Float>();
			for (String a : as)
				acouples.put(a, 0.5f);
			initialize(q,acouples);
		}
		
		private void initialize(String q, HashMap<String,Float> as) {
			question = q;
			answers = as;
		}
		
		public Float get(String a) {
			return (answers.get(a) == null ? 0.0f : answers.get(a));
		}
		
		public String getQuestion() { return question; }
		public HashMap<String,Float> getAllAnswers() { return answers; }
		public ArrayList<String> getAnswers() { return new ArrayList<String>(answers.keySet()); }
	}
	
	public QuestionGenerator(KBDbAdapter kbdb, DataWrapper ddub) {
		mDbHelper = kbdb;
		dw = ddub;
	}
	
	public QuestionAnswerPair askQuestion() {
		Long[] qats = mDbHelper.getAllQATs();
		Long[] afacts = mDbHelper.getAllFacts();
		ArrayList<Long> facts = new ArrayList<Long>();
		System.out.println("length of afacts:" + afacts.length);
		Long[] temp = mDbHelper.getFilteredFactsByTime(24*60*60*1000, mDbHelper.getFact(afacts[MyAuthActivity.r.nextInt(afacts.length)]).getTimestamp());
		for (Long l : temp) { facts.add(l); }
		System.out.println("Number of qualifying facts: " + facts.size());
		while (facts.size() > 0) {
			int index = MyAuthActivity.r.nextInt(facts.size());
			Fact fact = mDbHelper.getFact(facts.get(index));
			HashMap<Long,Integer[]> fqats = new HashMap<Long,Integer[]>();
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
				simFacts.add(fact);
				ArrayList<Long> possQats = new ArrayList<Long>(fqats.keySet());
				int qindex = MyAuthActivity.r.nextInt(fqats.size());
				QAT theQAT = mDbHelper.getQAT(possQats.get(qindex));
				Integer[] matches = fqats.get(possQats.get(qindex));
				//now that we have the QAT and fact tag matches, start filling in the blanks
				String question = theQAT.getQText();
				HashMap<String,Float> answers = new HashMap<String,Float>();
				//first check to see if the answer has the "list" or "anysubclass" meta
				ArrayList<String> acond_descs = new ArrayList<String>();
				for (String s : theQAT.getAcond().getAllDescriptions()) { acond_descs.add(s); };
				if (acond_descs.contains("list")) {
					//get similar facts
					for (Long l : facts) {
						Fact comp = mDbHelper.getFact(l);
						if (fact.sameStructure(comp, !acond_descs.contains("anysubclass"))) {
							simFacts.add(comp);
						}
					}
				}
				//Currently context unaware, so assign constant float value to answers
				for (Fact f : simFacts) {
					answers.put(f.getTagAt(matches[0]).getSV(),0.5f);
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
					question.replaceAll("{" + refVal + "}", fact.getTagAt(i).getSV());
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
				return new QuestionAnswerPair(question,answers);
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
				String ampm = "am";
				if (i > 12) {
					ampm = "pm";
					i -= 12;
				}
				retVal.append(i + ampm);
			} else {
				retVal.append(s);
			}
		}
		return retVal.toString();
	}
}
