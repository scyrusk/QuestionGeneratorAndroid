package com.cmuchimps.myauth;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MyAuthActivity extends Activity {
    /** Called when the activity is first created. */
	private KBXMLParser mParser;
	private KBDbAdapter mDbHelper;
	private DataWrapper dw;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView output = (TextView)this.findViewById(R.id.output);
        this.mDbHelper = new KBDbAdapter(this);
        this.mDbHelper.open();
        this.dw = new DataWrapper(mDbHelper);
        
        Long[] qats = mDbHelper.getAllQATs();
        Long[] facts = mDbHelper.getAllFacts();
        
        //Long[] ff = mDbHelper.getFilteredFactsByTime(24*60*60*1000,mDbHelper.getFact(facts[1]).getTimestamp());
        //Long[] ff = mDbHelper.getFilteredFactsByTag(new String[] {"Application:Game", "Application:Communication"});
        Long[] fq = mDbHelper.getFilteredQats(null, new String[] { "Application", "Location" });
        System.out.println("Count: " + fq.length);
        for (Long l : fq) {
            System.out.println(mDbHelper.getQAT(l));
        }
        System.out.println("");
        //System.out.println(mDbHelper.getQAT(2));
        this.mDbHelper.close();
    }
    
    private void printFacts(Long[] facts) {
    	System.out.println("ALL FACTS:\n");
        for (Long l : facts) {
        	System.out.println(mDbHelper.getFact(l));
        }
    }
    
    private void printQATS(Long[] qats) {
        System.out.println("ALL QATS:\n");
        for (Long l : qats) { 
        	System.out.println(mDbHelper.getQAT(l));
        }
    }
    
    private void repopulateDB() {
    	this.deleteDatabase(mDbHelper.DATABASE_NAME);
        try {
			this.populateTagClasses(this.getAssets().open("qg_other_files/tag_classes.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        this.mParser = new KBXMLParser(this,mDbHelper);
        mParser.parseFactBase("qg_xml/knowledgebase.xml");
        mParser.parseQATs("qg_xml/qats.xml");
    }
    
    /**
     * 
     * @param file
     */
    private void populateTagClasses(InputStream file) {
    	try {
			BufferedReader br = new BufferedReader(new InputStreamReader(file));
			String input;
			while ((input = br.readLine()) != null) {
				this.mDbHelper.createTagClass(input.replace("\n", ""));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}