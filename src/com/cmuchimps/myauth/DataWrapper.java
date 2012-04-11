package com.cmuchimps.myauth;

import java.util.ArrayList;
import java.util.HashMap;

public class DataWrapper {
	private KBDbAdapter mDbHelper;
	
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
	}
	
	public class Fact extends Component {
		private String timestamp;
		private String dayOfWeek;
		private Meta[] metas;
		private Tag[] tags;
		
		private Long[] meta_indices; //lazy evaluation for memory efficiency
		private Long[] tag_indices; //lazy evaluation for memory efficieny
		
		
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
			for (Long l : meta_indices) {
				//metas[counter++] = mDbHelper.getMeta(l);
			}
			return null;
		}
		
		public Meta getMeta(long id) {
			return null;
		}
		
		public Tag[] getAllTags() {
			return null;
		}
		
		public Tag getTag(long id) {
			return null;
		}
	}
	
	public class Acond extends Component {
		public String[] descriptions;
		public Tag[] tags;
		
		public Long[] desc_indices;
		public Long[] tag_indices;
	}
	
	public class Qcond extends Component {
		public String refnum;
		public Tag[] tags;
		
		public Long[] tag_indices;
		
		
	}
	
	public class QAT extends Component {
		
	}
	
	public HashMap<String,String> atomicElements; //key => single value
	public HashMap<String,ArrayList<String>> descriptors; //key => list of single values
	public HashMap<String,HashMap<String,String>> complexTypes; //key => hashmap of complex types
	
}
