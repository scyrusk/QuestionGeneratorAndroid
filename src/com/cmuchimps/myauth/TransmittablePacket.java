package com.cmuchimps.myauth;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.params.HttpParams;

public abstract class TransmittablePacket {
	protected int typeid;
	
	public abstract HttpParams convertToParams();
	public abstract List<NameValuePair> convertToNVP();
}
