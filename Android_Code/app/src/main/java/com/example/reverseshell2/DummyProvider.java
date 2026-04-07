package com.example.reverseshell2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Empty ContentProvider — required by the SyncAdapter framework.
 * SyncAdapter needs a content authority to bind to.
 * This provider does nothing; it exists solely to satisfy the framework requirement.
 */
public class DummyProvider extends ContentProvider {

    public static final String AUTHORITY = "com.android.systemservice.provider";

    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] sa, String so) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] sa) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String s, String[] sa) { return 0; }
}
