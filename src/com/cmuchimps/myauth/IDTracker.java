package com.cmuchimps.myauth;

public class IDTracker {
	private long nextQID = 0;
	private long nextAID = 0;
	
	public IDTracker() {
		nextQID = MyAuthActivity.r.nextLong();
		nextAID = MyAuthActivity.r.nextLong();
	}
	
	public long getNextQID() {
		long temp = nextQID;
		nextQID = MyAuthActivity.r.nextLong();
		return temp;
	}
	
	public long getNextAID() {
		long temp = nextAID;
		nextAID = MyAuthActivity.r.nextLong();
		return temp;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("IDTracker::{");
		sb.append("\tnextQID: " + nextQID);
		sb.append("\tnextAID: " + nextAID);
		return sb.toString();
	}
}
