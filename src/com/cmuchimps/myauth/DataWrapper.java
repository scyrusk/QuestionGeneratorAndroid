package com.cmuchimps.myauth;

import java.util.ArrayList;
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
