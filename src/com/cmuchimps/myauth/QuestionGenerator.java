package com.cmuchimps.myauth;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.format.DateFormat;
import android.util.Log;

import com.cmuchimps.myauth.DataWrapper.Fact;
import com.cmuchimps.myauth.DataWrapper.Meta;
import com.cmuchimps.myauth.DataWrapper.QAT;

import flexjson.JSONSerializer;

public class QuestionGenerator {
	private final String NEXT_ID_FILE = "nextid.json";
	private KBDbAdapter mDbHelper;
	private DataWrapper dw;
	private File mFilesDir;

	private IDTracker mIDTrack;
	
	//Constants
	public static final int QUESTION_FB_TYPE = 0;
	public static final int QUESTION_NAME_A_TYPE = 1;
	public static final int QUESTION_NAME_A_PERSISTENT_TYPE = 2;
	
	public static final long MARK_AS_QUERIED_THRESHOLD = 20l * UtilityFuncs.MIN_TO_MILLIS;
	
	private ArrayList<Long> mQuestionsAsked;
	
	// lazy loaded
	private long getNextID(boolean qid) {
		if (mIDTrack == null) {
			mIDTrack = new IDTracker();
		}
		long retVal = (qid ? mIDTrack.getNextQID() : mIDTrack.getNextAID());
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
		private ArrayList<String> allAnswers;
		private ArrayList<String> autoComplList;
		private boolean mapview;
		private boolean auto;
		private boolean recall;
		private ArrayList<Long> factsQueriedWhenAnswered;

		public QuestionAnswerPair() {
			initialize("", new HashMap<String,Float>(), new HashMap<String,String>(), 
					new ArrayList<HashMap<String,String>>(), false, false, true,
					new ArrayList<String>(), new ArrayList<String>(), new ArrayList<Long>());
		}

		public QuestionAnswerPair(String q, HashMap<String,Float> as, HashMap<String,String> qms, 
				ArrayList<HashMap<String,String>> ams, boolean mv, boolean au, boolean rc, 
				ArrayList<String> allAns, ArrayList<String> acl, ArrayList<Long> qf) {
			initialize(q, as, qms, ams, mv, au, rc, allAns, acl, qf);
		}

		public QuestionAnswerPair(String q, ArrayList<String> as, HashMap<String,String> qms, 
				ArrayList<HashMap<String,String>> ams, boolean mv, boolean au, boolean rc, 
				ArrayList<String> allAns, ArrayList<String> acl, ArrayList<Long> qf) {
			HashMap<String,Float> acouples = new HashMap<String,Float>();
			for (String a : as)
				acouples.put(a, 0.5f);
			initialize(q,acouples,qms,ams, mv, au, rc, allAns, acl, qf);
		}

		private void initialize(String q, HashMap<String,Float> as,HashMap<String,String> qms,
				ArrayList<HashMap<String,String>> ams, boolean mv, boolean au, boolean rc, 
				ArrayList<String> allAns, ArrayList<String> acl, ArrayList<Long> qf) {
			question = q;
			answers = as;
			question_metas = qms;
			answer_metas = ams;
			mapview = mv;
			auto = au;
			recall = rc;
			allAnswers = new ArrayList<String>(allAns);
			autoComplList = new ArrayList<String>(acl);
			Collections.shuffle(allAnswers);
			factsQueriedWhenAnswered = new ArrayList<Long>(qf);
		}

		public Float get(String a) {
			return (answers.get(a) == null ? 0.0f : answers.get(a));
		}
		
		public ArrayList<Long> getFactsToMarkAsQueried() {
			return new ArrayList<Long>(factsQueriedWhenAnswered);
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

		public boolean isAutoCompl() { return auto; }
		public boolean getMapView() { return mapview; }
		public boolean isRecallQ() { return recall; }
		public ArrayList<String> getAnswerList() { 
			return new ArrayList<String>(allAnswers); 
		}

		public String[] getAnswerListAsArr() {
			return allAnswers.toArray(new String[allAnswers.size()]);
		}

		public ArrayList<String> getAutoComplList() {
			return new ArrayList<String>(autoComplList);
		}

		public String[] getAutoComplListAsArr() {
			return autoComplList.toArray(new String[autoComplList.size()]);
		}

		public String getBestAnswer() {
			String best = answers.keySet().iterator().next();
			for (String key : answers.keySet())
				if (answers.get(key) > answers.get(best))
					best = key;
			return best;
		}

		public List<String> getNDistractionAnswers(int n) {
			List<String> distractions = new ArrayList<String>();
			String bestAnswer = getBestAnswer();
			int counter = 0;
			for (String key : answers.keySet()) {
				if (!key.equalsIgnoreCase(bestAnswer) && counter++ < n)
					distractions.add(key);
			}
			return distractions;
		}
	}

	public QuestionGenerator(KBDbAdapter kbdb, DataWrapper ddub, File filesDir) {
		mDbHelper = kbdb;
		dw = ddub;
		mFilesDir = filesDir;
		mQuestionsAsked = new ArrayList<Long>();
	}

	public void testQATagainstFacts(QAT theQat,List<Fact> facts) {
		for (Fact fact : facts) {
			Integer[] result = theQat.matches(fact);
		}
	}
	
	public QuestionAnswerPair askQuestion(Context ctx) {
		List<Integer> order = Arrays.asList(new Integer[] { 
				QUESTION_FB_TYPE, 
				QUESTION_NAME_A_TYPE/*, 
				QUESTION_NAME_A_PERSISTENT_TYPE*/ });
		Collections.shuffle(order);
		QuestionAnswerPair retVal;
		for (Integer type : order) {
			switch (type) {
			case QUESTION_FB_TYPE:
				//Log.d("QuestionType", "Fact Based Question");
				retVal = askQuestionFB(ctx);
				if (retVal == null) continue;
				return retVal;
			case QUESTION_NAME_A_TYPE:
				//Log.d("QuestionType", "Name A Type Question");
				retVal = askQuestionNA(ctx);
				if (retVal == null) continue;
				return retVal;
			case QUESTION_NAME_A_PERSISTENT_TYPE:
				//Log.d("QuestionType", "Name A Persistent Type Question");
				retVal = askQuestionNAB(ctx);
				if (retVal == null) continue;
				return retVal;
			default:
				retVal = askQuestionFB(ctx);
				if (retVal == null) continue;
				return retVal;
			}
		}
		return null;
	}
	
	private HashMap<Long, Double> possibleQAEntropiesFor(Long[] vals) {
		HashMap<Long, Double> entropies = new HashMap<Long, Double>();
		for (Long l : vals) {
			ArrayList<Long> dupQAs = new ArrayList<Long>(mQuestionsAsked);
			dupQAs.add(l);
			entropies.put(l, UtilityFuncs.shannonEntropy(dupQAs));
		}
		return entropies;
	}
	
	private ArrayList<Long> maximizeEntropySort(Long[] vals) {
		HashMap<Long, Double> entropyMap = possibleQAEntropiesFor(vals);
		long[] mapKeys = new long[entropyMap.size()];
		double[] entropies = new double[entropyMap.size()];
		//int[] indices = new int[entropyMap.size()];
		int counter = 0;
		for (Entry<Long, Double> entry : entropyMap.entrySet()) {
			mapKeys[counter] = entry.getKey();
			entropies[counter++] = entry.getValue();
			//indices[counter] = counter++;
		}
		//UtilityFuncs.quicksort(entropies, indices);
		
		ArrayList<Integer> indices = UtilityFuncs.randomizeWithinEquivalence(entropies);
		ArrayList<Long> retVal = new ArrayList<Long>(mapKeys.length);
		for (int i : indices) retVal.add(mapKeys[i]);
		return retVal;
	}
	
	public QuestionAnswerPair askQuestionNAB(Context ctx) {
		ArrayList<Long> qats = new ArrayList<Long>();
		for (Long l : mDbHelper.getAllQueryableQatsWithMeta("questionType","name_list_persistent"))
			qats.add(l);
		qats = maximizeEntropySort(qats.toArray(new Long[qats.size()]));
		for (int k = qats.size() - 1; k >= 0; k--) {
			Long qat_id = qats.get(k);
			QAT qat = mDbHelper.getQAT(qat_id);
			Long[] tagFacts = mDbHelper.findAllFactsWithTags(
					qat.getAllTCs(), qat.getAllSCs(), 
					qat.getAllSVs());
			Long[] qatFacts = mDbHelper.intersectFactsWithPersistence(tagFacts, "persistent");
			if (qatFacts.length == 0) {
				qats.remove(qat_id);
				continue;
			}
			String question = qat.getQText();
			HashMap<String,Float> answers = new HashMap<String,Float>();
			
			HashMap<Fact, Integer[]> factMatchesMap = new HashMap<Fact,Integer[]>();
			//add question meta
			String currTimeStamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", 
					System.currentTimeMillis());
			
			HashMap<String,String> question_metas = new HashMap<String,String>();
			long nextQID = getNextID(true);
			question_metas.put("qid", ""+ nextQID);
			question_metas.put("timestamp", currTimeStamp); // not asking ba
			question_metas.put("qatid", "" + qat.getID());
			for (Meta m : qat.getAllMetas()) 
				question_metas.put(m.getName(), m.getValue());
			
			ArrayList<String> acond_descs = new ArrayList<String>();
			for (String s : qat.getAcond().getAllDescriptions()) 
				acond_descs.add(s);
			
			boolean autocompl = acond_descs.contains("autocompl");
			
			HashSet<String> autoents = new HashSet<String>();
			String answerType = qat.getAnswerType();
			
			ArrayList<HashMap<String,String>> answer_metas = new ArrayList<HashMap<String,String>>();
			// For each qatFact, add answer meta
			for (Long l : qatFacts) {
				Fact fact = mDbHelper.getFact(l);
				Integer[] matches = null;
				for (Fact comp : factMatchesMap.keySet()) {
					if (fact.sameStructure(comp, true))
						matches = factMatchesMap.get(comp);
				}
				if (matches == null) {
					matches = qat.matches(fact);
					factMatchesMap.put(fact, matches);
				}
				
				String theAnswer = (answerType.equalsIgnoreCase("Internet:Visited-Site-Url") ?
						UtilityFuncs.getURLHost(fact.getTagAt(matches[0]).getSV()) :
							fact.getTagAt(matches[0]).getSV());
				
				if (answers.containsKey(theAnswer)) continue;
				
				answers.put(theAnswer, 1.0f);
				autoents.add(theAnswer);
				
				long nextAID = getNextID(false);
				HashMap<String,String> answer_meta = new HashMap<String,String>();
				answer_meta.put("aid", ""+nextAID);
				answer_meta.put("timestamp", fact.getTimestamp());
				answer_meta.put("value", theAnswer);
				answer_meta.put("correct", "yes");
				answer_metas.add(answer_meta);
			}
			
			//Log.d("NameAQuestionType", "Question: " + question);
			//Log.d("NameAQuestionType", "All answers:");
			/*for (String answer : answers.keySet()) {
				//Log.d("NameAQuestionType", answer);
			}*/
			
			if (answerType.equalsIgnoreCase("Audio:SongTitle")) {
				String[] topSongs = ctx.getResources().getStringArray(R.array.top_music);
				for (String s : topSongs) autoents.add(s);
			} else if (answerType.equalsIgnoreCase("Audio:Artist")) {
				String[] topArtists = ctx.getResources().getStringArray(R.array.top_artists);
				for (String s : topArtists) autoents.add(s);
			}
			
			//return new QuestionAnswerPair(question,answers,question_metas,answer_metas, 
			//		mapViewAnswer, autocompl, !recog, allAnswers, autoents);
			mQuestionsAsked.add(qat.getID());
			if (UtilityFuncs.lastNSame(mQuestionsAsked, 3))
				mDbHelper.setResetTimeOfQAT(qat.getID(), 
						System.currentTimeMillis() + 15l*UtilityFuncs.MIN_TO_MILLIS);
			return new QuestionAnswerPair(question, answers, question_metas,
					answer_metas, false, autocompl, true, 
					new ArrayList<String>(), new ArrayList<String>(autoents),
					new ArrayList<Long>());
		}
		
		return null;
	}
	
	public QuestionAnswerPair askQuestionNA(Context ctx) {
		/*
		 * Randomly choose from qats with questionType name_list
		 * Find all facts in the past 24 hours with tags in qconds and aconds.
		 * Ask question
		 */
		ArrayList<Long> qats = new ArrayList<Long>();
		int hoursInPast = 24;
		for (Long l : mDbHelper.getAllQueryableQatsWithMeta("questionType","name_list"))
			qats.add(l);
		qats.removeAll(mQuestionsAsked);
		qats = maximizeEntropySort(qats.toArray(new Long[qats.size()]));
		for (int k = qats.size() - 1; k >= 0; k--) {
			Long qat_id = qats.get(k);
			QAT qat = mDbHelper.getQAT(qat_id);
			//Log.d("QAT_DUMP", "TagClasses for this QAT: " + qat.getQText());
			//for (String s : qat.getAllTCs()) Log.d("QAT_DUMP", s);
			//Log.d("QAT_DUMP", "Subclasses for this QAT: " + qat.getQText());
			//for (String s : qat.getAllSCs()) Log.d("QAT_DUMP", s);
			Long[] qatFacts = mDbHelper.findAllFactsWithTagsWithinTime(qat.getAllTCs(), qat.getAllSCs(), qat.getAllSVs(), 
					hoursInPast*UtilityFuncs.HOUR_TO_MILLIS, "dynamic");
			//System.out.println(qat);
			//System.out.println(qatFacts.length);
			if (qatFacts.length == 0) {
				qats.remove(qat_id);
				continue;
			}
			String question = qat.getQText();
			HashMap<String,Float> answers = new HashMap<String,Float>();
			
			HashMap<Fact, Integer[]> factMatchesMap = new HashMap<Fact,Integer[]>();
			//add question meta
			String currTimeStamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", 
					System.currentTimeMillis());
			
			HashMap<String,String> question_metas = new HashMap<String,String>();
			long nextQID = getNextID(true);
			question_metas.put("qid", ""+ nextQID);
			question_metas.put("timestamp", currTimeStamp); // not asking ba
			question_metas.put("qatid", "" + qat.getID());
			for (Meta m : qat.getAllMetas()) 
				question_metas.put(m.getName(), m.getValue());
			
			ArrayList<String> acond_descs = new ArrayList<String>();
			for (String s : qat.getAcond().getAllDescriptions()) 
				acond_descs.add(s);
			
			boolean autocompl = (acond_descs.contains("autocompl"));
			HashSet<String> autoents = new HashSet<String>();
			String answerType = qat.getAnswerType();
			
			ArrayList<HashMap<String,String>> answer_metas = new ArrayList<HashMap<String,String>>();
			// For each qatFact, add answer meta
			for (Long l : qatFacts) {
				//System.out.println("Fact corresponding to id " + l);
				Fact fact = mDbHelper.getFact(l);
				//System.out.println(fact);
				Integer[] matches = null;
				for (Fact comp : factMatchesMap.keySet()) {
					if (fact.sameStructure(comp, true))
						matches = factMatchesMap.get(comp);
				}
				if (matches == null) {
					matches = qat.matches(fact);
					if (matches == null) continue; //should never happen
					factMatchesMap.put(fact, matches);
				}
				
				String theAnswer = (answerType.equalsIgnoreCase("Internet:Visited-Site-Url") ?
						UtilityFuncs.getURLHost(fact.getTagAt(matches[0]).getSV()) :
							fact.getTagAt(matches[0]).getSV());
				
				if (answers.containsKey(theAnswer)) continue;
				
				answers.put(theAnswer, 1.0f);
				
				long nextAID = getNextID(false);
				HashMap<String,String> answer_meta = new HashMap<String,String>();
				answer_meta.put("aid", ""+nextAID);
				answer_meta.put("timestamp", fact.getTimestamp());
				answer_meta.put("value", theAnswer);
				answer_meta.put("correct", "yes");
				answer_metas.add(answer_meta);
				
				
				if (autocompl) 
					autoents.add(theAnswer);
			}
			
			if (answerType.equalsIgnoreCase("Person:Contact")) {
				Cursor c = ctx.getContentResolver().query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
						new String[] { Phone.DISPLAY_NAME }, null, null, null);
				if (c.getCount() > 0) {
					c.moveToFirst();
					while (!c.isAfterLast()) {
						autoents.add(c.getString(0));
						c.moveToNext();
					}
				}
				c.close();
			} else if (answerType.split(":")[0].equalsIgnoreCase("Application")) {
				PackageManager pm = ctx.getPackageManager();
				List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
				for (ApplicationInfo pi : packages) 
					autoents.add(pm.getApplicationLabel(pi).toString());
			} else if (answerType.equalsIgnoreCase("Internet:Visited-Site-Url")) {
				String[] topSites = ctx.getResources().getStringArray(R.array.top_sites);
				for (String ts : topSites) autoents.add(ts);
				Cursor c = ctx.getContentResolver().query(Browser.BOOKMARKS_URI, Browser.HISTORY_PROJECTION, null, null, null);
				if (c != null && c.getCount() > 0) {
					c.moveToFirst();
					while (!c.isAfterLast()) {
						String tmpURL = UtilityFuncs.getURLHost(c.getString(Browser.HISTORY_PROJECTION_URL_INDEX));
						autoents.add(tmpURL);
						c.moveToNext();
					}
				}
				if (c != null) c.close();
			}
			
			// replace question text
			// currently no references because not attached
			//substruct time			
			question = question.replaceAll("\\(time:n hours\\)", hoursInPast + " hours");
			
			//Log.d("NameAQuestionType", "Question: " + question);
			//Log.d("NameAQuestionType", "All answers:");
			/*for (String answer : answers.keySet()) {
				Log.d("NameAQuestionType", answer);
			}*/
			//return new QuestionAnswerPair(question,answers,question_metas,answer_metas, 
			//		mapViewAnswer, autocompl, !recog, allAnswers, autoents);
			mQuestionsAsked.add(qat.getID());
			mDbHelper.setResetTimeOfQAT(qat.getID(), 
					System.currentTimeMillis() + 24*UtilityFuncs.HOUR_TO_MILLIS);
			ArrayList<String> autoentslist = new ArrayList<String>();
			for (String s : autoents) autoentslist.add(s);
			
			return new QuestionAnswerPair(question, answers, question_metas,
					answer_metas, false, autocompl, true, 
					new ArrayList<String>(), autoentslist,
					new ArrayList<Long>());
		}
		
		return null;
	}
	
	/** TODO: add persistence = persistent to filter 
	 *  TODO: only validate QATS with questionType fact_based*/
	public QuestionAnswerPair askQuestionFB(Context ctx) {
		/*
		 * Filter knowledge base facts by the past 24 hours
		 * Compress all filtered facts into distinct type
		 * Randomly select type of fact to ask about
		 * Randomly select specific fact out of the randomly selected type
		 * Find matching QATs
		 * Randomly select QAT out of those that match
		 * Fill in the blanks
		 */
		Long[] qats = mDbHelper.getAllQueryableQatsWithMeta("questionType","fact_based");
		Long[] afacts = mDbHelper.getAllFacts();
		ArrayList<Long> facts = new ArrayList<Long>();
		//System.out.println("length of afacts:" + afacts.length);
		//Long[] temp = mDbHelper.getFilteredFactsByTime(24*60*60*1000, mDbHelper.getFact(afacts[MyAuthActivity.r.nextInt(afacts.length)]).getTimestamp());
		String pivotTime = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", 
				System.currentTimeMillis() - 3*UtilityFuncs.HOUR_TO_MILLIS);

		Long[] temp = UtilityFuncs.sample(
				mDbHelper.getUnqueriedFilteredFactsByTime(
					24*UtilityFuncs.HOUR_TO_MILLIS, pivotTime, "dynamic"), 
				100);
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
		/*Log.d("Fact","Different types of facts:" + factCompression.size());
		for (Fact f : factCompression.keySet()) {
			Log.d("Fact", f.toString());
		}
		Log.d("Fact", "Number of qualifying facts: " + facts.size());*/
		
		while (facts.size() > 0 && factCompression.size() > 0) {
			int index = MyAuthActivity.r.nextInt(factCompression.size());
			Fact fc_key = factCompression.keySet().toArray(new Fact[factCompression.size()])[index];
			ArrayList<Long> simFactStructures = factCompression.get(fc_key);
			index = MyAuthActivity.r.nextInt(simFactStructures.size());
			Fact fact = mDbHelper.getFact(simFactStructures.get(index));
			HashMap<Long,Integer[]> fqats = new HashMap<Long,Integer[]>(); //qat index => matching matrix
			if (fact.getQueried() || UtilityFuncs.invalidTime(fact)) {
				facts.remove(fact); //removes fact from fact collection
				simFactStructures.remove(index);
				if (simFactStructures.size() <= 0)
					factCompression.remove(fc_key);
				continue;
			}
			for (Long l : qats) {
				Integer[] result = mDbHelper.getQAT(l).matches(fact);
				if (result != null) {
					fqats.put(l, result);
				}
			}
			if (fqats.size() == 0) { //no matches
				// System.out.println("couldn't find askable fact");
				facts.remove(fact);
				simFactStructures.remove(index);
				if (simFactStructures.size() <= 0)
					factCompression.remove(fc_key);
				continue;
			} else {
				//generate question answer pair
				//first pick qat at random
				HashSet<Fact> simFacts = new HashSet<Fact>();
				ArrayList<Long> possQats = new ArrayList<Long>(fqats.keySet());
				possQats = maximizeEntropySort(possQats.toArray(new Long[possQats.size()]));
				int qindex = possQats.size()-1;
				QAT theQAT = mDbHelper.getQAT(possQats.get(qindex));
				Integer[] matches = fqats.get(possQats.get(qindex));
				//now that we have the QAT and fact tag matches, start filling in the blanks
				String question = theQAT.getQText();
				HashMap<String,Float> answers = new HashMap<String,Float>();
				String answerType = fact.getTagAt(matches[0]).getTC() + ":" + fact.getTagAt(matches[0]).getSC();
				String theAnswer = (answerType.equalsIgnoreCase("Internet:Visited-Site-Url") ?
						UtilityFuncs.getURLHost(fact.getTagAt(matches[0]).getSV()) :
							fact.getTagAt(matches[0]).getSV());
				//System.out.println(answerType);
				answers.put(theAnswer, 1.0f);
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

				//should also check if acond_descs contain mapview or autocompl hints
				boolean mapViewAnswer = acond_descs.contains("mapview");
				boolean autocompl = (acond_descs.contains("autocompl"));

				boolean shouldBeRecogd = MyAuthActivity.r.nextInt(2) == 0;
				boolean recog = acond_descs.contains("recogable") && shouldBeRecogd;

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
				long nextQID = getNextID(true);

				question_metas.put("qid", ""+ nextQID);
				question_metas.put("timestamp", fact.getTimestamp());
				question_metas.put("qatid", "" + theQAT.getID());
				for (Meta m : theQAT.getAllMetas()) 
					question_metas.put(m.getName(), m.getValue());
				

				ArrayList<HashMap<String,String>> answer_metas = new ArrayList<HashMap<String,String>>();
				HashMap<String,String> answer_meta = new HashMap<String,String>();
				ArrayList<String> allAnswers = new ArrayList<String>();
				long nextAID = getNextID(false);

				HashSet<String> autoents = new HashSet<String>();
				if (answerType.equalsIgnoreCase("Person:Contact")) {
					Cursor c = ctx.getContentResolver().query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
							new String[] { Phone.DISPLAY_NAME }, null, null, null);
					if (c.getCount() > 0) {
						c.moveToFirst();
						while (!c.isAfterLast()) {
							autoents.add(c.getString(0));
							c.moveToNext();
						}
					}
					c.close();
				} else if (answerType.split(":")[0].equalsIgnoreCase("Application")) {
					PackageManager pm = ctx.getPackageManager();
					List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
					for (ApplicationInfo pi : packages) autoents.add(pm.getApplicationLabel(pi).toString());
				} else if (answerType.equalsIgnoreCase("Internet:Visited-Site-Url")) {
					String[] topSites = ctx.getResources().getStringArray(R.array.top_sites);
					for (String ts : topSites) autoents.add(ts);
					autoents.add(theAnswer);
					Cursor c = ctx.getContentResolver().query(Browser.BOOKMARKS_URI, Browser.HISTORY_PROJECTION, null, null, null);
					if (c != null && c.getCount() > 0) {
						c.moveToFirst();
						while (!c.isAfterLast()) {
							String tmpURL = UtilityFuncs.getURLHost(c.getString(Browser.HISTORY_PROJECTION_URL_INDEX));
							autoents.add(tmpURL);
							c.moveToNext();
						}
					}
					if (c != null) c.close();
				} else if (answerType.equalsIgnoreCase("Audio:SongTitle")) {
					String[] topMusic = ctx.getResources().getStringArray(R.array.top_music);
					for (String tm : topMusic) autoents.add(tm);
					autoents.add(theAnswer);
					Cursor c = ctx.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { 
						MediaStore.Audio.AudioColumns.TITLE
					}, null, null, null);
					if (c != null && c.getCount() > 0) {
						c.moveToFirst();
						while (!c.isAfterLast()) {
							autoents.add(c.getString(0));
							c.moveToNext();
						}
						c.close();
					}
				}
				
				String currTimeStamp = (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", 
						System.currentTimeMillis());
				
				ArrayList<Long> factsToMarkAsQueried = new ArrayList<Long>();
				factsToMarkAsQueried.add(fact.getID());
				long factTime = UtilityFuncs.getLongTimeForFact(fact);
				//long origFactTS = (new DateFormat())
				if (recog) {
					ArrayList<String> dupAutoEnts = new ArrayList<String>(autoents);
					int numAnswers = (MyAuthActivity.r.nextInt(10) >= 5 ? 5 : 10);
					allAnswers.add(theAnswer);
					answer_meta.put("aid", ""+nextAID);
					answer_meta.put("timestamp",fact.getTimestamp());
					answer_meta.put("value", theAnswer);
					answer_meta.put("correct", "yes");
					answer_metas.add(answer_meta);
					int added = 0;
					for (Fact f : simFacts) {
						if (Math.abs(factTime - UtilityFuncs.getLongTimeForFact(f)) < 
								MARK_AS_QUERIED_THRESHOLD && !factsToMarkAsQueried.contains(f.getID()))
							factsToMarkAsQueried.add(f.getID());
						String tmpAnswer = (answerType.equalsIgnoreCase("Internet:Visited-Site-Url") ?
								UtilityFuncs.getURLHost(f.getTagAt(matches[0]).getSV()) :
									f.getTagAt(matches[0]).getSV());
						if (allAnswers.contains(tmpAnswer))
							continue;
						allAnswers.add(tmpAnswer);
						nextAID = getNextID(false);
						answer_meta = new HashMap<String,String>();
						answer_meta.put("aid", ""+nextAID);
						answer_meta.put("timestamp", f.getTimestamp());
						answer_meta.put("value", tmpAnswer);
						answer_meta.put("correct", "nearMiss");
						answer_metas.add(answer_meta);
						if (++added == (numAnswers-1)/2) break;
					}
					dupAutoEnts.removeAll(allAnswers);
					for (int i = allAnswers.size(); i < numAnswers; i++) {
						if (dupAutoEnts.size() == 0) break;
						int addIndex = MyAuthActivity.r.nextInt(dupAutoEnts.size());
						allAnswers.add(dupAutoEnts.get(addIndex));
						nextAID = getNextID(false);
						answer_meta = new HashMap<String,String>();
						answer_meta.put("aid", ""+nextAID);
						answer_meta.put("timestamp", currTimeStamp);
						answer_meta.put("value", dupAutoEnts.get(addIndex));
						answer_meta.put("correct", "random");
						answer_metas.add(answer_meta);
						dupAutoEnts.remove(addIndex);
					}
				} else {
					answer_meta.put("aid", ""+nextAID);
					answer_meta.put("timestamp",fact.getTimestamp());
					answer_meta.put("value", theAnswer);
					answer_meta.put("correct", "yes");
					answer_meta.put("tag_metas", UtilityFuncs.JSONify(fact.getAllExcept(matches[0])));
					answer_metas.add(answer_meta);
					HashMap<String, Fact> uniqueAnswers = new HashMap<String,Fact>();
					
					for (Fact f : simFacts) {
						if (Math.abs(factTime - UtilityFuncs.getLongTimeForFact(f)) < 
								MARK_AS_QUERIED_THRESHOLD && !factsToMarkAsQueried.contains(f.getID()))
							factsToMarkAsQueried.add(f.getID());
						String tmpAnswer = (answerType.equalsIgnoreCase("Internet:Visited-Site-Url") ?
								UtilityFuncs.getURLHost(f.getTagAt(matches[0]).getSV()) :
									f.getTagAt(matches[0]).getSV());
						if (!tmpAnswer.equalsIgnoreCase(theAnswer)) {
							Fact comp = uniqueAnswers.get(tmpAnswer);
							if (comp == null || f.getTimestamp().compareTo(comp.getTimestamp()) > 0) {
								uniqueAnswers.put(tmpAnswer, f); //most recent fact represents the answer
							}
						}
					}
					
					for (String key : uniqueAnswers.keySet()) {
						Fact f = uniqueAnswers.get(key);
						nextAID = getNextID(false);
						answer_meta = new HashMap<String,String>();
						answer_meta.put("aid", ""+nextAID);
						answer_meta.put("timestamp", f.getTimestamp());
						answer_meta.put("value", key);
						answer_meta.put("correct", "no");
						answer_meta.put("tag_metas", UtilityFuncs.JSONify(f.getAllExcept(matches[0])));
						answer_metas.add(answer_meta);
					}
				}

				//mDbHelper.registerFactAsQueried(fact.getID());
				mQuestionsAsked.add(theQAT.getID());
				if (UtilityFuncs.lastNSame(mQuestionsAsked, 1))
					mDbHelper.setResetTimeOfQAT(theQAT.getID(), 
							System.currentTimeMillis() + 15l*UtilityFuncs.MIN_TO_MILLIS);
				
				
				return new QuestionAnswerPair(question,answers,question_metas,answer_metas, 
						mapViewAnswer, autocompl, !recog, allAnswers, new ArrayList<String>(autoents),
						factsToMarkAsQueried);
			}
		}

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
				String[] tsplit = fact.getTimestamp().split("[T ]")[1].split(":");
				String hod = tsplit[0];
				String min = tsplit[1];
				int i = Integer.parseInt(hod);
				int minInt = Integer.parseInt(min);
				String ampm = "am";
				if (i >= 12) {
					ampm = "pm";
					i -= 12;
				}
				/*
				i = (minInt >= 45 ? i + 1 : i);
				boolean half = (minInt > 20 && minInt < 45);
				if (half) {
					String toAppend = (i == 0 ? 12 : i) + ":30 " + ampm;
					retVal.append(toAppend);
				} else
					retVal.append((i == 0 ? "midnight" : (i == 12 ? "noon" : i + ampm)));
				*/
				retVal.append((i == 0 ? 12 : i) + ":" + 
							  (minInt < 10 ? "0" + minInt : minInt) + 
							  " " + ampm);
			} else {
				retVal.append(s);
			}
		}
		return retVal.toString();
	}
}
