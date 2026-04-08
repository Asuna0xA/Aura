# Aura: Next-Gen Android Surveillance Research Suite

[![version](https://img.shields.io/badge/version-3.2.1--Elite-blue.svg)](https://github.com/Asuna0xA/Aura)
[![Android Compatibility](https://img.shields.io/badge/Android-11.0%2B--15.0-green.svg)](https://developer.android.com/about/versions/15)
[![License](https://img.shields.io/badge/license-Non--Commercial-red.svg)](DISCLAIMER.md)

**Aura** is a modular, professional-grade Android surveillance framework designed as a Proof-of-Concept (PoC) for research into OS-level persistence and stealth. It transcends legacy RAT models by implementing commercial-standard asynchronous data syncing and advanced hacker persistence techniques.

---

## 🛡️ Aura vs. Commercial (FamiGuard/VigilKids)

Aura achieves feature parity with commercial "parental control" tools while offering advanced "hacker" bypasses for modern Android security.

| Feature | FamiGuard Pro | Aura (v3.2.1) |
| :--- | :---: | :---: |
| **Persistence Integration** | 🟡 (Service-based) | 🏆 **SyncAdapter + Alarm Pulse** |
| **Android 15 Survival** | ❌ (6h Wall) | 🏆 **Pulse Strategy (Active)** |
| **OEM Evasion** | ❌ (Doze/Sleep) | 🏆 **Passive Boot Delay** |

---

## 🏗️ Technical Architecture

```mermaid
graph TD
    subgraph "Target Device (Victim)"
        A[Android System] --> B[EnvironmentGuard]
        B -- "Safe" --> C[Aura Core]
        B -- "Hostile (Emulator/Frida)" --> D[Dormancy]
        C --> E[SyncAdapter]
        C --> F[Dead Drop Resolver]
        C --> G[Accessibility Service]
        G --> H[Universal Data Scraper]
        G --> I[Anti-Uninstall Hook]
        E -- "15m Periodic" --> J[HTTPS REST Sync]
    end

    subgraph "C2 Infrastructure (VPS)"
        K["DDR Endpoint (Gist/Paste)"]
        L[Flask API]
        M[("aura.db")]
        J -- "Resolved via K" --> L
        L --> M
    end

    subgraph "Analyst View (Browser)"
        N[Web Dashboard]
        N -- "Real-time" --> L
    end
```

---

## 🚀 Key Innovation Highlights

### 1. OS-Level Persistence (The Brain & Brawn)
Aura v3.2.1 introduces a dual-layer persistence model to survive the aggressive "Deep Sleep" heuristics of modern OEMs (Samsung, Honor).
- **The Brain (SyncAdapter)**: Registered as a fake system account. This ensures the app process is prioritized by the Android `system_server`.
- **The Brawn (Pulse FGS)**: A 10-minute periodic Foreground Service (FGS) that triggers only when necessary, bypassing Android 15's 6-hour aggregate daily limit for `dataSync` services.

### 2. Dead Drop Resolver (DDR)
To prevent C2 takedowns and forensic discovery, Aura utilizes a **Dead Drop Resolver**. The C2 IP address is never hardcoded. Instead, the implant fetches an encrypted string from a public service (like a GitHub Gist or Instagram comment), decodes it at runtime, and connects to the active VPS. 

### 3. EnvironmentGuard (Anti-Analysis)
Aura is "context-aware." Before activating any collection modules, it runs a suite of checks to detect emulators, Frida instrumentation, debuggers, and forensic tools (Cellebrite, Drozer). If detected, Aura enters a permanent "Dormancy" state to prevent analysis.

---

## 🛠️ Quick Setup (Research Only)

1.  **Configure DDR**: Use `server/ddr_helper.py` to encode your VPS IP and host it on a public Gist.
2.  **Deploy C2**: Run the Flask server on your VPS (Ubuntu 22.04+ recommended).
3.  **Build APK**: Configure the `DDR_URL` in `DeadDropResolver.java` and compile using Gradle.
4.  **Install**: Grant permissions (A11y + Notifications) on the target device.

> [!IMPORTANT]
> Detailed setup instructions can be found in [INSTALL.md](INSTALL.md).
> Technical protocol details are documented in [PROTOCOL.md](PROTOCOL.md).

---

## 🧪 Advanced Research Roadmap

Current version: **v3.1 (Stable)**

- [x] **Phase 3.2**: Elite Stabilization (Android 15 + OEM Evasion).
- [ ] **Phase 4.0**: GhostLoader - JNI/NDK Memory-resident execution.
- [ ] **Phase 4.1**: SQLCipher integration for on-disk forensic invisibility.

---

## ⚖️ Responsibility & Disclaimer

This project is intended for educational purposes and authorized penetration testing only. Unauthorized access to computer systems is illegal. Please read [DISCLAIMER.md](DISCLAIMER.md) before interacting with this tool.
