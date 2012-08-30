package com.cmuchimps.myauth;

import android.os.AsyncTask;

public class CullingOldFactsTask extends AsyncTask<KBDbAdapter, Void, Void> {
	public final static int CULL_THRESHOLD_IN_DAYS = 3;
	@Override
	protected Void doInBackground(KBDbAdapter... params) {
		for (KBDbAdapter dbHelper : params) 
			dbHelper.cullOldFacts("*", CULL_THRESHOLD_IN_DAYS);
		return null;
	}

}
