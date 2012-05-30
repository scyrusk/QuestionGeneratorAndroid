package com.cmuchimps.myauth;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import flexjson.JSONSerializer;

public class ConsentFormActivity extends Activity {
	public final static String CONSENT_FILE = "consent.json";
	private CheckBox understand;
	private CheckBox participate;
	private Button accept;
	private Button reject;
	
	private boolean understand_checked = false;
	private boolean participate_checked = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.consent);
		
		if (new File(getFilesDir(),ConsentFormActivity.CONSENT_FILE).exists()) {
			setResult(Activity.RESULT_OK);
			finish();
		}
		understand = (CheckBox)this.findViewById(R.id.understood);
		participate = (CheckBox)this.findViewById(R.id.participate);
		accept = (Button)this.findViewById(R.id.accept);
		reject = (Button)this.findViewById(R.id.reject);
		
		understand.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				understand_checked = isChecked;
				accept.setEnabled(understand_checked && participate_checked);
			}
		});
		
		participate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				participate_checked = isChecked;
				accept.setEnabled(understand_checked && participate_checked);
			}
		});
		
		reject.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(Activity.RESULT_CANCELED);
				finish();
				Toast.makeText(getApplicationContext(), "Sorry to hear that! Looks like you can't participate in the study.", Toast.LENGTH_LONG);
			}
		});
		
		accept.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//save consent.json
				Writer writer = null;
				try {
					writer =  new FileWriter(new File(getFilesDir(),CONSENT_FILE));
					new JSONSerializer().deepSerialize(new String[] { "I have read and understand the information above.", "I want to participate in this research and continue with the study."},writer);
					writer.flush();
					writer.close();
					setResult(Activity.RESULT_OK);
					finish();
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Sorry, it seems that your device doesn't enable file storage. Please contact sauvik@cmu.edu for assistance if you wish to continue.", Toast.LENGTH_LONG).show();
				}
			}
		});
	}
}
