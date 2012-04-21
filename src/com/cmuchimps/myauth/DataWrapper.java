package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DataWrapper {
	private KBDbAdapter mDbHelper;
	
	public DataWrapper(KBDbAdapter mDb) {
		// TODO Auto-generated constructor stub
		this.mDbHelper = mDb;
		this.mDbHelper.setDW(this);
	}
	
	/**
	 * All complex fields will be evaluated in a lazy manner (initialized only when called upon)
	 * @author sauvikd
	 *
	 */
	public abstract class Component {
		protected long _id;
		
		public long getID() { return _id; }
	}
	
	public class Tag extends Component {
		private String tag_class;
		private String subclass;
		private String subvalue;
		
		public Tag(long id, String tc, String sc, String sv) {
			_id = id;
			tag_class = tc;
			subclass = sc;
			subvalue = sv;
		}
		
		public String getTC() { return tag_class; }
		public String getSC() { return subclass; }
		public String getSV() { return subvalue; }

		public Object toString(int tabprepend) {
			StringBuffer prepend = new StringBuffer();
			for (int i = 0; i < tabprepend; i++) prepend.append("\t");
			String compiled = prepend.toString();
			StringBuffer retVal = new StringBuffer(compiled + "tag::" + _id + "{\n");
			retVal.append(compiled + "\tTag Class: " + tag_class + "\n");
			retVal.append(compiled + "\tSubclass: " + subclass + "\n");
			if (UtilityFuncs.isMeaningfulString(subvalue)) retVal.append(compiled + "\tSubvalue: " + subvalue + "\n");
			return retVal.append(compiled + "}\n").toString();
		}
	}
	
	public class Meta extends Component {
		private String name;
		private String value;

		public Meta(long id, String n, String v) {
			_id = id;
			name = n;
			value = v;
		}
		
		public String getName() { return name; }
		public String getValue() { return value; }

		public String toString(int tabprepend) {
			StringBuffer prepend = new StringBuffer();
			for (int i = 0; i < tabprepend; i++) prepend.append("\t");
			String compiled = prepend.toString();
			StringBuffer retVal = new StringBuffer(compiled + "meta::" + _id + "{\n");
			retVal.append(compiled + "\tName: " + name + "\n");
			if (value != null && !value.equalsIgnoreCase("")) retVal.append(compiled + "\tValue: " + value + "\n");
			return retVal.append(compiled + "}\n").toString();
		}
	}
	
	/**
	 * 
	 * @author sauvikd
	 *
	 */
	public class Fact extends Component {
		private String timestamp;
		private String dayOfWeek;
		private Meta[] metas;
		private Tag[] tags;
		
		private Long[] meta_indices; //lazy evaluation for memory efficiency
		private Long[] tag_indices; //lazy evaluation for memory efficieny
		
		public Fact() {
			initialize(-1, "", "", new Long[0], new Long[0]);
		}
		
		public Fact(long id, String ts, String dow, Long[] mi, Long[] ti) {
			initialize(id, ts, dow, mi, ti);
		}
		
		private void initialize(long id,String ts, String dow, Long[] mi, Long[] ti) {
			_id = id;
			timestamp = ts;
			dayOfWeek = dow;
			meta_indices = new Long[mi.length];
			for (int i = 0; i < mi.length; i++) meta_indices[i] = mi[i];
			tag_indices = new Long[ti.length];
			for (int i = 0; i < ti.length; i++) tag_indices[i] = ti[i];
			metas = new Meta[mi.length];
			tags = new Tag[ti.length];
		}
		
		public String getTimestamp() { return timestamp; }
		public String getDayOfWeek() { return dayOfWeek; }
		
		public Meta[] getAllMetas() {
			int counter = 0;
			for (Long l : meta_indices) 
				if (metas[counter++] == null) metas[counter-1] = mDbHelper.getMeta(l);
			return metas;
		}
		
		public Meta getMetaAt(int index) {
			if (index >= meta_indices.length) return null;
			if (metas[index] == null) metas[index] = mDbHelper.getMeta(meta_indices[index]);
			return metas[index];
		}
		
		public Tag[] getAllTags() {
			int counter = 0;
			for (Long l : tag_indices) 
				if (tags[counter++] == null) tags[counter - 1] = mDbHelper.getTag(l);
			return tags;
		}
		
		public Tag getTagAt(int index) {
			if (index >= tag_indices.length) return null;
			if (tags[index] == null) tags[index] = mDbHelper.getTag(tag_indices[index]);
			return tags[index];
		}
		
		public HashMap<String,HashMap<String,Integer>> createTagCounter() {
			HashMap<String,HashMap<String,Integer>> tagCounter = new HashMap<String,HashMap<String,Integer>>();
			Tag[] tags = getAllTags();
			for (Tag t : tags) {
				HashMap<String,Integer> tagMap = (tagCounter.get(t.tag_class) == null ? new HashMap<String,Integer>() : tagCounter.get(t.tag_class));
				int counter = (tagMap.get(t.subclass) == null ? 0 : tagMap.get(t.subclass));
				tagMap.put(t.subclass, counter + 1);
				tagCounter.put(t.tag_class, tagMap);
			}
			return tagCounter;
		}
		
		public boolean sameStructure(Fact other) {
			HashMap<String,HashMap<String,Integer>> myTC = this.createTagCounter();
			HashMap<String,HashMap<String,Integer>> otherTC = other.createTagCounter();
			if (myTC.keySet().size() != otherTC.keySet().size()) return false;
			for (String key : myTC.keySet()) {
				HashMap<String,Integer> ot = null, my = myTC.get(key);
				if ((ot = otherTC.get(key)) == null) return false;
				for (String subkey : my.keySet()) {
					Integer myCount = my.get(subkey), oCount;
					if ((oCount = ot.get(subkey)) == null || myCount != oCount) return false;
				}
			}
			return true; //passed all filters
		}
		
		public boolean sameStructure(Fact other, boolean consider_sub) {
			if (consider_sub) return sameStructure(other);
			else {
				HashMap<String,HashMap<String,Integer>> myTC = this.createTagCounter();
				HashMap<String,HashMap<String,Integer>> otherTC = other.createTagCounter();
				//Create tag_class only tag counter
				if (myTC.keySet().size() != otherTC.keySet().size()) return false;
				for (String s : myTC.keySet()) {
					if (otherTC.get(s) == null) return false;
					int myCount = 0;
					for (String subkey : myTC.get(s).keySet()) {
						myCount += myTC.get(s).get(subkey);
					}
					int oCount = 0;
					for (String subkey : otherTC.get(s).keySet()) {
						oCount += otherTC.get(s).get(subkey);
					}
					if (myCount != oCount) return false;
				}
				return true; //passed all filters
			}
		}
		
		public String toString(int tabprepend) {
			StringBuffer prepend = new StringBuffer();
			for (int i = 0; i < tabprepend; i++) prepend.append("\t");
			String compiled = prepend.toString();
			StringBuffer retVal = new StringBuffer(compiled + "fact::" + _id + "{\n");
			retVal.append(compiled + "\tTimestamp: " + this.timestamp + "\n");
			retVal.append(compiled + "\tDay Of Week: " + this.dayOfWeek + "\n");
			if (metas.length > 0) {
				retVal.append(compiled + "\tMetas:\n");
				Meta[] temp = getAllMetas();
				for (Meta m : temp) retVal.append(m.toString(tabprepend+1));
			}
			if (tags.length > 0) {
				retVal.append(compiled + "\tTags:\n");
				Tag[] temp = getAllTags();
				for (Tag m : temp) retVal.append(m.toString(tabprepend+1));
			}
			return retVal.append(compiled + "}\n").toString();
		}
		
		public String toString() {
			return toString(0);
		}
	}
	
	/**
	 * 
	 * @author sauvikd
	 *
	 */
	public class Acond extends Component {
		public String[] descriptions;
		public Tag[] tags;
		
		public Long[] desc_indices;
		public Long[] tag_indices;
		
		public Acond(long id, Long[] di, Long[] ti) {
			initialize(id, di,ti);
		}
		
		public Acond(long id, Long[] ti) {
			initialize(id, new Long[0], ti);
		}
		
		private void initialize(long id, Long[] di, Long[] ti) {
			_id = id;
			desc_indices = new Long[di.length];
			for (int i = 0; i < di.length; i++) desc_indices[i] = di[i];
			tag_indices = new Long[ti.length];
			for (int i = 0; i < ti.length; i++) tag_indices[i] = ti[i];
			descriptions = new String[desc_indices.length];
			tags = new Tag[tag_indices.length];
		}
		
		public String getDescription(long id) {
			int finalIndex = UtilityFuncs.search(desc_indices, id);
			if (finalIndex == -1) return null;
			if (descriptions[finalIndex] == null) descriptions[finalIndex] = mDbHelper.getDescription(id);
			return descriptions[finalIndex];
		}
		
		public String[] getAllDescriptions() {
			int counter = 0;
			for (Long l : desc_indices) 
				if (descriptions[counter++] == null) descriptions[counter - 1] = mDbHelper.getDescription(l);
			return descriptions;
		}
		
		public Tag[] getAllTags() {
			int counter = 0;
			for (Long l : tag_indices) 
				if (tags[counter++] == null) tags[counter - 1] = mDbHelper.getTag(l);
			return tags;
		}
		
		public Tag getTagAt(int index) {
			if (index >= tag_indices.length) return null;
			if (tags[index] == null) tags[index] = mDbHelper.getTag(tag_indices[index]);
			return tags[index];
		}
		
		public String toString(int tabprepend) {
			StringBuffer prepend = new StringBuffer();
			for (int i = 0; i < tabprepend; i++) prepend.append("\t");
			String compiled = prepend.toString();
			StringBuffer retVal = new StringBuffer(compiled + "acond::" + _id + "{\n");
			if (descriptions.length > 0) {
				retVal.append(compiled + "\tDescriptions:\n");
				String[] temp = getAllDescriptions();
				for (String s : temp) retVal.append(compiled + "\t\tDescription: " + s + "\n");
			}
			if (tags.length > 0) {
				retVal.append(compiled + "\tTags:\n");
				Tag[] temp = getAllTags();
				for (Tag m : temp) retVal.append(m.toString(tabprepend+1));
			}
			return retVal.append(compiled + "}\n").toString();
		}
	}
	
	/**
	 * 
	 * @author sauvikd
	 *
	 */
	public class Qcond extends Component {
		public int refnum;
		public Tag[] tags;
		
		public Long[] tag_indices;
		
		public Qcond(long id, int rn, Long[] ti) {
			initialize(id, rn, ti);
		}
		
		public Qcond(long id) {
			initialize(id, -1, new Long[0]);
		}
		
		private void initialize(long id, int rn, Long[] ti) {
			_id = id;
			refnum = rn;
			tag_indices = new Long[ti.length];
			for (int i = 0; i< ti.length; i++) tag_indices[i] = ti[i];
			tags = new Tag[ti.length];
		}
		
		public int getRefNum() { return refnum; }
		
		public Tag[] getAllTags() {
			int counter = 0;
			for (Long l : tag_indices) 
				if (tags[counter++] == null) tags[counter - 1] = mDbHelper.getTag(l);
			return tags;
		}
		
		public Tag getTagAt(int index) {
			if (index >= tag_indices.length) return null;
			if (tags[index] == null) tags[index] = mDbHelper.getTag(tag_indices[index]);
			return tags[index];
		}
		
		public String toString(int tabprepend) {
			StringBuffer prepend = new StringBuffer();
			for (int i = 0; i < tabprepend; i++) prepend.append("\t");
			String compiled = prepend.toString();
			StringBuffer retVal = new StringBuffer(compiled + "acond::" + _id + "{\n");
			retVal.append(compiled + "\tRefNum: " + refnum + "\n");
			if (tags.length > 0) {
				retVal.append(compiled + "\tTags:\n");
				Tag[] temp = getAllTags();
				for (Tag m : temp) retVal.append(m.toString(tabprepend+1));
			}
			return retVal.append(compiled + "}\n").toString();
		}
	}
	
	/**
	 * 
	 * @author sauvikd
	 *
	 */
	public class QAT extends Component {
		private String qtext;
		private Acond acond;
		private Qcond[] qconds;
		private Meta[] metas;
		
		private long acond_index;
		private Long[] qcond_indices;
		private Long[] meta_indices;
		
		public QAT() {
			initialize(-1, "", -1, new Long[0], new Long[0]);	
		}
		
		public QAT(long id, String qt, long ac_ind, Long[] qi, Long[] mi) {
			initialize(id, qt, ac_ind, qi, mi);
		}
		
		private void initialize(long id, String qt, long ac_ind, Long[] qi, Long[] mi) {
			_id = id;
			qtext = qt;
			acond_index = ac_ind;
			qcond_indices = new Long[qi.length];
			for (int i = 0; i < qi.length; i++) qcond_indices[i] = qi[i];
			meta_indices = new Long[mi.length];
			for (int i = 0; i < mi.length; i++) meta_indices[i] = mi[i];
			
			qconds = new Qcond[qi.length];
			metas = new Meta[mi.length];
		}
		
		public String getQText() { return qtext; }
		
		public Acond getAcond() {
			if (acond == null) acond = mDbHelper.getAcond(acond_index);
			return acond;
		}
		
		public Qcond getQcondAt(int index) {
			if (index >= qcond_indices.length) return null;
			if (qconds[index] == null) qconds[index] = mDbHelper.getQcond(qcond_indices[index]);
			return qconds[index];
		}
		
		public Qcond[] getAllQconds() {
			int counter = 0;
			for (Long l : qcond_indices) 
				if (qconds[counter++] == null) qconds[counter-1] = mDbHelper.getQcond(l);
			return qconds;
		}
		
		public Meta[] getAllMetas() {
			int counter = 0;
			for (Long l : meta_indices) 
				if (metas[counter++] == null) metas[counter-1] = mDbHelper.getMeta(l);
			return metas;
		}
		
		public Meta getMetaAt(int index) {
			if (index >= meta_indices.length) return null;
			if (metas[index] == null) metas[index] = mDbHelper.getMeta(meta_indices[index]);
			return metas[index];
		}
		
		/**
		 * Matches a question answer template to a fact. Untested!
		 * @param fact
		 * @return
		 */
		public Integer[] matches(Fact fact) {
			HashMap<String,HashMap<String,Integer>> tagCounter = fact.createTagCounter();
			//match all possible qconds/aconds with one possible instantiation right away
			//match flexible ones using dynamic programming
			//create all conds
			int count = getAcond().getAllTags().length;
			for (int i = 0; i < getAllQconds().length; i++) count += qconds[i].getAllTags().length;
			Condition[] allconds = new Condition[count];
			allconds[0] = new Condition(acond);
			for (int i = 1; i < count; i++) allconds[i] = new Condition(getQcondAt(i-1));
			
			Integer[] matches = new Integer[count]; //will contain corresponding match for each condition
			for (int i = 0; i < count; i++) matches[i] = -1;
			
			//create QAT condition x Fact Tag binary match table
			int[][] qatbyfact = new int[allconds.length][fact.tags.length];
			for (int i = 0; i < qatbyfact.length; i++) {
				for (int j = 0; j < qatbyfact[0].length; j++) {
					qatbyfact[i][j] = (allconds[i].matches(fact.getTagAt(j)) ? 1 : 0);
				}
			}
			
			//set all definite matches by finding rows in qatbyfact that only sum up to one
			HashMap<Integer,Integer[]> mults = new HashMap<Integer,Integer[]>();
			for (int i = 0; i < qatbyfact.length; i++) {
				Integer[] matchingIdxs = UtilityFuncs.getMatches(qatbyfact[i]);
				if (matchingIdxs.length == 0) {
					System.out.println("No matching tag for qcond:" + (i == 0 ? getAcond().toString(0) : getQcondAt(i-1).toString(0)));
					return null; //no matches found
				} else if (matchingIdxs.length == 1) { //exactly one match found, must set to this
					//decrement tag counter
					Tag matched = fact.getTagAt(matchingIdxs[0]);
					tagCounter.get(matched.tag_class).put(matched.subclass,tagCounter.get(matched.tag_class).get(matched.subclass) - 1); //not sure if this works, may need to put result back in parent HashMap
					matches[i] = matchingIdxs[0];
				} else { //multiple matches found
					mults.put(i, matchingIdxs);
				}
			}
			
			//use dynamic programming to match multiple matches ideally
			//first check if any multiple matches even exist
			if (mults.keySet().size() == 0) {
				boolean allmatched = true;
				for (int i = 0; i < matches.length; i++)
					allmatched = allmatched && matches[i] > 0;
				if (allmatched) return matches; //all matches found and no multiple matches
			}
			
			Integer[] keys = mults.keySet().toArray(new Integer[mults.keySet().size()]);
			Arrays.sort(keys);
			int[] minCounter = new int[keys.length];
			for (int i = 0; i < minCounter.length; i++) minCounter[i] = 0;
			int currentRow = 0; //current row of matrix
			ArrayList<Integer> taken = new ArrayList<Integer>();
			//keys contains the indices of all multiple matched conditions
			while (minCounter[0] < mults.get(keys[0]).length) {
				//first check if we need to backtrack
				if (minCounter[currentRow] >= mults.get(keys[currentRow]).length) {
					//must backtrack, tried all possibilities for this row in this config
					taken.remove(taken.size()-1);
					for (int i = currentRow; i < keys.length; i++) { //reset this row and all rows below
						minCounter[i] = 0;
					}
					minCounter[--currentRow]++; //increment counter for previous row
					continue;
				}
				
				//now check if current condition matches a remaining fact tag
				int tag_index_of_fact = mults.get(keys[currentRow])[minCounter[currentRow]];
				if (taken.contains(tag_index_of_fact)) {
					//if current fact tag already taken
					minCounter[currentRow]++;
					continue;
				} else {
					matches[keys[currentRow]] = tag_index_of_fact;
					taken.add(tag_index_of_fact);
					currentRow++;
				}
				
				if (currentRow == keys.length) return matches;
			}
			
			System.out.println("No matches found..no matches for mults");
			return null;
		}
		
		private class Condition {
			Tag[] conds;
			int counter;
			
			public Condition() {
				initialize(new Tag[0], -1);
			}
			
			public Condition(Qcond qcond) {
				initialize(qcond.getAllTags(),0);
			}
			
			public Condition(Acond acond) {
				initialize(acond.getAllTags(),0);
			}
			
			private void initialize(Tag[] cs, int c) {
				conds = new Tag[cs.length];
				for (int i = 0; i < cs.length; i++) conds[i] = cs[i];
				counter = c;
			}
			
			public boolean matches(Tag c) {
				for (Tag cond : conds) {
					if (cond.tag_class.equalsIgnoreCase(c.tag_class)) {
						if (cond.subclass.equalsIgnoreCase("*") || cond.subclass.equalsIgnoreCase(c.subclass))
							return true;
					}
				}
				return false;
			}
			
			public Tag getCurrent() {
				return (finished() ? null : conds[counter]);
			}
			
			public int length() {
				return conds.length;
			}
			
			public void reset() {
				counter = 0;
			}
			
			public boolean finished() {
				return counter >= conds.length;
			}
		}
		
		/**
		 * 
		 * @param tabprepend
		 * @return
		 */
		public String toString(int tabprepend) {
			StringBuffer prepend = new StringBuffer();
			for (int i = 0; i < tabprepend; i++) prepend.append("\t");
			String compiled = prepend.toString();
			StringBuffer retVal = new StringBuffer(compiled + "qat::" + _id + "{\n");
			retVal.append(compiled + "\tQText: " + this.qtext + "\n");
			retVal.append(this.getAcond().toString(tabprepend+1));
			retVal.append(compiled + "");
			if (qconds.length > 0) {
				retVal.append(compiled + "\tQconds:\n");
				Qcond[] temp = getAllQconds();
				for (Qcond m : temp) retVal.append(m.toString(tabprepend+1));
			}
			if (metas.length > 0) {
				retVal.append(compiled + "\tMetas:\n");
				Meta[] temp = getAllMetas();
				for (Meta m : temp) retVal.append(m.toString(tabprepend+1));
			}
			return retVal.append(compiled + "}\n").toString();
		}
		
		/**
		 * 
		 */
		public String toString() {
			return toString(0);
		}
	}
}
