package com.cmuchimps.myauth;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

public class NewUserActivity extends Activity {
	private EditText name;
	private EditText email;
	private EditText age;
	
	private RadioGroup radioGender;
	private RadioGroup radioEthnicity;
	
	private Button submit;
	
	private String mName="";
	private String mEmail="";
	private int mAge;
	private String mGender="";
	private String mEthnicity="";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (User.exists(getFilesDir()))
			finish();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user);
		
		name = (EditText)this.findViewById(R.id.name_entry);
		email = (EditText)this.findViewById(R.id.email_entry);
		age = (EditText)this.findViewById(R.id.age_entry);
		radioGender = (RadioGroup)this.findViewById(R.id.radioGender);
		radioEthnicity = (RadioGroup)this.findViewById(R.id.radioEthnicity);
		submit = (Button)this.findViewById(R.id.submit);
		
		radioGender.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				mGender = ((RadioButton)findViewById(checkedId)).getText().toString();
			}
		});
		
		radioEthnicity.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				mEthnicity = ((RadioButton)findViewById(checkedId)).getText().toString();
			}
		});
		
		submit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Validate information
				mName = name.getText().toString();
				mEmail = email.getText().toString();
				String age_check = age.getText().toString();
				
				if (mName.length() <= 0 || mEmail.length() <= 0 || age_check.length() <= 0 || mGender.length() <= 0 || mEthnicity.length() <= 0) {
					Toast.makeText(getApplicationContext(), "Please enter a value for all of the fields in the form", Toast.LENGTH_SHORT).show();
					return;
				}
				try {
					mAge = Integer.parseInt(age_check);
				} catch (NumberFormatException e) {
					Toast.makeText(getApplicationContext(), "Sorry, it seems you entered an invalid age. You must enter a number.", Toast.LENGTH_SHORT).show();
					return;
				}
				if (mAge < 18 || mAge > 99) {
					Toast.makeText(getApplicationContext(), "Sorry, participants in this study must be at least 18.", Toast.LENGTH_SHORT).show();
					return;
				}
				
				//save user.json
				try {
					User user = new User(getApplicationContext().getFilesDir(),mName,mEmail,mAge,mEthnicity,mGender);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), getApplicationContext().getFilesDir().getPath(), Toast.LENGTH_LONG).show();
					//Toast.makeText(getApplicationContext(), "Sorry, it seems that your device doesn't enable file storage. Please contact sauvik@cmu.edu for assistance if you wish to continue.", Toast.LENGTH_LONG).show();
					return;
				}
				
				//return to main activity if good
				setResult(Activity.RESULT_OK);
				finish();
			}
		});
	}
	
}
