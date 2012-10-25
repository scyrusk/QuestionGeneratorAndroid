package com.cmuchimps.myauth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SubscriptionReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		//Log.d("SubscriptionReceiver","Attempting to update knowledge base");
		//Log.d("SubscriptionReceiver","Attempting to update knowledge base...");
		Intent updater = new Intent(context,KnowledgeTranslatorWrapper.class);
		//updater.putExtra("dueSubs", intent.getStringArrayExtra("dueSubs"));
		context.startService(updater);
	}
}
