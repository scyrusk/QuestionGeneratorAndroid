package com.cmuchimps.myauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;

/**
 * 
 * @author sauvikd
 *
 */
public class KBXMLParser {
	//Will one day validate
	private Context mCtx;
	private KBDbAdapter mDbHelper;
	
	/**
	 * 
	 * @param ctx
	 * @param adap
	 */
	public KBXMLParser(Context ctx, KBDbAdapter adap) {
		this.mCtx = ctx;
		this.mDbHelper = adap;
	}
	
	/**
	 * 
	 * @param filename
	 */
	public void parseFactBase(String filename) {
		try {
        	DocumentBuilderFactory parserFac = DocumentBuilderFactory.newInstance();
        	parserFac.setNamespaceAware(true);
        	DocumentBuilder parser = parserFac.newDocumentBuilder();
        	Document document = parser.parse(mCtx.getAssets().open(filename));
        	NodeList nl = document.getElementsByTagName("tns:Fact");
        	//process each fact
        	for (int i = 0; i < nl.getLength(); i++) {
        		String timestamp = "", dayOfWeek = "";
        		ArrayList<HashMap<String,String>> tags = new ArrayList<HashMap<String,String>>();
        		ArrayList<HashMap<String,String>> metas = new ArrayList<HashMap<String,String>>();
        		
        		NodeList children = nl.item(i).getChildNodes();
        		for (int j = 0; j < children.getLength(); j++) {
        			//System.out.println(children.item(j).getNodeName() + ":" + (children.item(j).getNodeName().equalsIgnoreCase("tns:Timestamp")));
        			Node child = children.item(j);
        			if (child.getNodeName().equalsIgnoreCase("tns:Timestamp")) {
        				String[] timestamp_vals = this.parseTimestamp(child);
        				timestamp = timestamp_vals[0];
        				dayOfWeek = timestamp_vals[1];
        			} else if (child.getNodeName().equalsIgnoreCase("tns:Tags")) {
        				//parse tags node
        				tags = this.parseTags(child);
        			} else if (child.getNodeName().equalsIgnoreCase("tns:MetaInformation")) {
        				//parse metainfo node
        				metas = this.parseMetaInfo(child);
        			}
        		}
        		this.mDbHelper.createFact(timestamp, dayOfWeek, tags, metas);
        	}
        }
        catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param filename
	 */
	public void parseQATs(String filename) {
		try {
        	DocumentBuilderFactory parserFac = DocumentBuilderFactory.newInstance();
        	parserFac.setNamespaceAware(true);
        	DocumentBuilder parser = parserFac.newDocumentBuilder();
        	Document document = parser.parse(mCtx.getAssets().open(filename));
        	NodeList nl = document.getElementsByTagName("tns:QAT");
        	//process each fact
        	for (int i = 0; i < nl.getLength(); i++) {
        		String qtext = "";
        		long qat_id = 0l;
        		ArrayList<HashMap<String,String>> metas = new ArrayList<HashMap<String,String>>();
        		Node acond = null, qcond = null;
        		NodeList children = nl.item(i).getChildNodes();
        		for (int j = 0; j < children.getLength(); j++) {
        			//System.out.println(children.item(j).getNodeName() + ":" + (children.item(j).getNodeName().equalsIgnoreCase("tns:Timestamp")));
        			Node child = children.item(j);
        			if (child.getNodeName().equalsIgnoreCase("tns:QText")) {
        				qtext = child.getTextContent();
        			} else if (child.getNodeName().equalsIgnoreCase("tns:Acond")) {
        				acond = child;
        			} else if (child.getNodeName().equalsIgnoreCase("tns:Qconds")) {
        				qcond = child;
        			} else if (child.getNodeName().equalsIgnoreCase("tns:MetaInfo")) {
        				metas = this.parseMetaInfo(child);
        			}
        		}
        		
        		if (acond != null) {
        			qat_id = this.mDbHelper.createQAT(qtext, metas);
            		this.parseAcondNode(acond, qat_id);
            		if (qcond != null) this.parseQCondNode(qcond, qat_id);
        		}
        	}
        }
        catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void parseQCondNode(Node qcond, long qat_id) {
		// TODO Auto-generated method stub
		NodeList childtags = qcond.getChildNodes();
		for (int i = 0; i < childtags.getLength(); i++) {
			Node child = childtags.item(i);
			if (child.getNodeName().equalsIgnoreCase("tns:Qcond")) {
				ArrayList<HashMap<String,String>> tags = new ArrayList<HashMap<String,String>>();
				String refnum = "";
				NodeList qcondchildren = child.getChildNodes();
				for (int j = 0; j < qcondchildren.getLength(); j++) {
					Node qcond_child = qcondchildren.item(j);
					if (qcond_child.getNodeName().equalsIgnoreCase("tns:Tag")) {
						HashMap<String,String> tag = parseTagNode(qcond_child);
						if (tag != null) tags.add(tag);
					} else if (qcond_child.getNodeName().equalsIgnoreCase("tns:RefNum")) {
						refnum = qcond_child.getTextContent();
					}
				}
				this.mDbHelper.createQcond(qat_id, Integer.parseInt(refnum), tags);
			}
		}
	}

	private void parseAcondNode(Node acond, long qat_id) {
		// TODO Auto-generated method stub
		NodeList childtags = acond.getChildNodes();
		ArrayList<String> descs = new ArrayList<String>();
		ArrayList<HashMap<String,String>> tags = new ArrayList<HashMap<String,String>>();
		for (int i = 0; i < childtags.getLength(); i++) {
			Node child = childtags.item(i);
			if (child.getNodeName().equalsIgnoreCase("tns:Descriptions")) {
				descs = parseDescriptions(child);
			} else if (child.getNodeName().equalsIgnoreCase("tns:Tag")) {
				HashMap<String,String> tag = parseTagNode(child);
				if (tag != null) tags.add(tag);
			}
		}
		this.mDbHelper.createAcond(qat_id, descs, tags);
	}
	
	private ArrayList<String> parseDescriptions(Node descs) {
		ArrayList<String> retVal = new ArrayList<String>();
		NodeList children = descs.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equalsIgnoreCase("tns:Description")) {
				retVal.add(child.getTextContent());
			}
		}
		return retVal;
	}
	
	private HashMap<String,String> parseTagNode(Node tag) {
		HashMap<String,String> retVal = new HashMap<String,String>();
		NodeList children = tag.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equalsIgnoreCase("tns:Class")) {
				String tclass = child.getTextContent().replaceAll("(^ *)|([\n ]+$)","");
				retVal.put("tag_class", tclass);
			} else if (child.getNodeName().equalsIgnoreCase("tns:SubClass")) {
				retVal.put("subclass", child.getTextContent().replaceAll("(^ *)|([\n ]+$)",""));
			} else if (child.getNodeName().equalsIgnoreCase("tns:SubVal")) {
				retVal.put("subvalue", child.getTextContent().replaceAll("(^ *)|([\n ]+$)",""));
			}
		}
		return (retVal.keySet().contains("tag_class") ? retVal : null);
	}

	/**
	 * 
	 * @param ts
	 * @return
	 */
	private String[] parseTimestamp(Node ts) {
		String[] retVal = new String[2];
		NodeList children = ts.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equalsIgnoreCase("tns:Date")) {
				retVal[0] = child.getTextContent();
			} else if (child.getNodeName().equalsIgnoreCase("tns:DayOfWeek")) {
				retVal[1] = child.getTextContent();
 			}
		}
		return retVal;
	}
	
	/**
	 * 
	 * @param tags
	 * @return
	 */
	private ArrayList<HashMap<String,String>> parseTags(Node tags) {
		ArrayList<HashMap<String,String>> retVal = new ArrayList<HashMap<String,String>>();
		NodeList eachtag = tags.getChildNodes();
		for (int i = 0; i < eachtag.getLength(); i++) {
			Node child = eachtag.item(i);
			if (child.getNodeName().equalsIgnoreCase("tns:Tag")) {
				HashMap<String,String> currtag = new HashMap<String,String>();
				//parseTag node
				NodeList tagchilds = child.getChildNodes();
				for (int j = 0; j < tagchilds.getLength(); j++) {
					Node tagchild = tagchilds.item(j);
					if (tagchild.getNodeName().equalsIgnoreCase("tns:Class")) {
						String tclass = tagchild.getTextContent().replaceAll("(^ *)|([\n ]+$)","");
						currtag.put("tag_class", tclass);
					} else if (tagchild.getNodeName().equalsIgnoreCase("tns:SubClass")) {
						currtag.put("subclass", tagchild.getTextContent().replaceAll("(^ *)|([\n ]+$)",""));
					} else if (tagchild.getNodeName().equalsIgnoreCase("tns:SubVal")) {
						currtag.put("subvalue", tagchild.getTextContent().replaceAll("(^ *)|([\n ]+$)",""));
					}
				}
				retVal.add(currtag);
			}
		}
		return retVal;
	}
	
	/**
	 * 
	 * @param metas
	 * @return
	 */
	private ArrayList<HashMap<String,String>> parseMetaInfo(Node metas) {
		ArrayList<HashMap<String,String>> retVal = new ArrayList<HashMap<String,String>>();
		NodeList eachmeta = metas.getChildNodes();
		for (int i = 0; i < eachmeta.getLength(); i++) {
			Node child = eachmeta.item(i);
			if (child.getNodeName().equalsIgnoreCase("tns:Meta")) {
				HashMap<String,String> currMeta = new HashMap<String,String>();
				NodeList meta_childs = child.getChildNodes();
				for (int j = 0; j < meta_childs.getLength(); j++) {
					Node meta_child = meta_childs.item(j);
					if (meta_child.getNodeName().equalsIgnoreCase("tns:Name")) {
						currMeta.put("name", meta_child.getTextContent());
					} else if (meta_child.getNodeName().equalsIgnoreCase("tns:Value")) {
						currMeta.put("value", meta_child.getTextContent());
					}
				}
				retVal.add(currMeta);
			}
		}
		return retVal;
	}
	
	/**
	 * Because Android does not allow automated schema matching
	 * @param xml_file
	 * @param schema_file
	 */
	public void validateXML(String xml_file, String schema_file) {
		
	}
}
