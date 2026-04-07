package com.example.reverseshell2;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Bound Service that exposes the SyncAdapter's IBinder.
 * The system's SyncManager binds to this service to trigger syncs.
 */
public class SyncService extends Service {

    private static SyncAdapterImpl syncAdapter = null;
    private static final Object lock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (lock) {
            if (syncAdapter == null) {
                syncAdapter = new SyncAdapterImpl(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
