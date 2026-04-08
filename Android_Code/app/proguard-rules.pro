# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 1. Keep all API 36 / Baklava-specific lifecycle hooks
-keepclassmembers class * extends android.app.Service {
    public void onTimeout(int, int);
}

# 2. Prevent stripping of the 2026 Broadcast Receiver flags
-keep class android.content.Context {
    public android.content.Intent registerReceiver(android.content.BroadcastReceiver, android.content.IntentFilter, int);
}

# 3. SyncAdapter & AccountManager Persistence
-keep class * extends android.app.Service
-keep class * extends android.content.ContentProvider
-keep class * extends android.accounts.AbstractAccountAuthenticator
-keep class * extends android.content.AbstractThreadedSyncAdapter

# 4. EnvironmentGuard Protection
-keepclassmembers class com.example.reverseshell2.EnvironmentGuard {
    *** isEmulator(...);
    *** isDebuggerAttached(...);
}