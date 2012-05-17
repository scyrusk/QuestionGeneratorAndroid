package com.cmuchimps.myauth;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class User extends TransmittablePacket {
	public final static String USER_FILE = "myAuthUser.json";
	public String name;
	public String email;
	public int age;
	public String ethnicity;
	public String gender;
	public String unique_id;
	private File filesDir;
	
	public User() {
		initialize(null,"","",0,"","");
	}
	
	public User(File f,String n, String em, int a, String e, String g) throws IOException {
		initialize(f,em,n,a,e,g);
		serialize();
	}
	
	private void initialize(File f,String n, String em, int a, String e, String g) {
		typeid = 0;
		filesDir = f;
		name = n;
		email = em;
		age = a;
		ethnicity = e;
		gender = g;
		unique_id = UtilityFuncs.getHash(name);
	}
	
	private void serialize() throws IOException {
		Writer writer = new BufferedWriter(new FileWriter(new File(filesDir, USER_FILE)));
		try {
			new JSONSerializer().exclude("filesDir").deepSerialize(this,writer);
			writer.flush();
		} finally {
			writer.close();
		}
	}
	
	public static boolean exists(File filesDir) {
		return new File(filesDir, USER_FILE).exists();
	}
	
	public static User load(File filesDir) throws IOException {
		if (User.exists(filesDir)) {
			return new JSONDeserializer<User>().deserialize(new FileReader(new File(filesDir, USER_FILE)), User.class);
		}
		return null;
	}
	
	public HttpParams convertToParams() {
		HttpParams retVal = new BasicHttpParams();
		retVal.setParameter("type", typeid);
		retVal.setParameter("name", name);
		retVal.setParameter("email", email);
		retVal.setParameter("ethnicity", ethnicity);
		retVal.setParameter("gender", gender);
		retVal.setParameter("unique_id", unique_id);
		retVal.setParameter("age", age);
		return retVal;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("User\n{\n");
		sb.append("\tType: " + typeid + "\n");
		sb.append("\tName: " + name + "\n");
		sb.append("\tEmail: " + email + "\n");
		sb.append("\tEthnicity: " + ethnicity + "\n");
		sb.append("\tGender: " + gender + "\n");
		sb.append("\tAge: " + age + "\n");
		sb.append("\tUnique ID: " + unique_id + "\n");
		sb.append("}\n");
		return sb.toString();
	}
}