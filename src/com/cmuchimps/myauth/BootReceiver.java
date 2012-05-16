package com.cmuchimps.myauth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*
 * Always called by BOOT_COMPLETED intent
 */
public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Intent updater = new Intent(context, SubscriptionReceiver.class);
    	PendingIntent recurringUpdate = PendingIntent.getBroadcast(context, 0, updater, PendingIntent.FLAG_CANCEL_CURRENT);
    	AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    	alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * UtilityFuncs.MIN_TO_MILLIS, 10l*UtilityFuncs.MIN_TO_MILLIS, recurringUpdate);
	}
}