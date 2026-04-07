package com.example.reverseshell2;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Bound Service that exposes the DummyAuthenticator's IBinder.
 * Required by the Android account framework — the system binds
 * to this service to interact with our AccountAuthenticator.
 */
public class AuthenticatorService extends Service {

    private DummyAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        authenticator = new DummyAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}
