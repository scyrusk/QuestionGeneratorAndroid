package com.cmuchimps.myauth;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
	private final String NOTE_TITLE = "myAuth Reminder";
	private final String NOTE_DESC = "Answer your questions about your day!";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		//Log.d("NotificationReceiver","Attempting to set notification");
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification note = new Notification(R.drawable.authentication, "Answer questions about your day", 
				System.currentTimeMillis());
		note.flags |= Notification.FLAG_AUTO_CANCEL;
		
		Intent i = new Intent(context, MyAuthActivity.class);
		PendingIntent activity = PendingIntent.getActivity(context, 0, i, 0);
		note.setLatestEventInfo(context, NOTE_TITLE, NOTE_DESC, activity);
		nm.notify(0, note);
	}
}