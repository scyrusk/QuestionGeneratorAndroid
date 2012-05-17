package com.cmuchimps.myauth;

import org.apache.http.params.HttpParams;

public abstract class TransmittablePacket {
	protected int typeid;
	
	public abstract HttpParams convertToParams();
}
