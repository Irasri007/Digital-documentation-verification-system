# DocVerify — Digital Documentation Verification System
**Team LogicLoop** · Suhani · Avni · Ira · BTECH CSE JAVA-IV-T117 · GEU

## Quick Start

### Prerequisites
Java JDK 17+  (https://adoptium.net/) + VS Code with Extension Pack for Java

### Step 1 — Configure (optional but recommended)
Edit `ddvs.properties` in the project root before starting:
- Set a strong `hmac.key`
- Add `issuer.api.keys` to enable issuer authentication

### Step 2 — Start the Java Backend
**Windows:** Double-click `run.bat`
**Mac/Linux:** `chmod +x run.sh && ./run.sh`

You'll see: `Running at http://localhost:8080`

### Step 3 — Open the Frontend
Open `frontend/index.html` in your browser.
A **green pill** (bottom-right) confirms the backend is connected.

---

## Project Structure
```
ddvs/
├── ddvs.properties              ← configuration (HMAC key, API keys, data path)
├── ddvs-data.json               ← auto-created: persisted documents & audit log
├── run.bat / run.sh             ← compile + start server
├── frontend/index.html          ← full UI
└── backend/src/main/java/com/ddvs/
    ├── Main.java                ← HTTP server (port 8080)
    ├── Config.java              ← loads ddvs.properties + env vars   [NEW]
    ├── IssueHandler.java        ← POST /api/issue  (+ API key auth)  [UPDATED]
    ├── VerifyHandler.java       ← POST /api/verify
    ├── AuditHandler.java        ← GET /api/audit
    ├── HealthHandler.java       ← GET /api/health
    ├── ConfigStatusHandler.java ← GET /api/config-status             [NEW]
    ├── Registry.java            ← file-persisted store + audit log   [UPDATED]
    ├── CryptoUtil.java          ← SHA-256 + HMAC-SHA256              [UPDATED]
    ├── DocumentRecord.java      ← data model
    ├── MultipartParser.java     ← multipart parser (no libs)
    ├── MultipartData.java
    ├── JsonParser.java          ← JSON parser (no libs)
    └── StaticHandler.java       ← serves frontend files
```

---

## Phase 2 Features Added

| Feature | Details |
|---------|---------|
| **Persistent storage** | All issued documents + audit log saved to `ddvs-data.json`. Survives server restarts. No external database needed. |
| **Issuer authentication** | `POST /api/issue` requires `X-Api-Key` header when `issuer.api.keys` is set in config. |
| **SecureRandom credential IDs** | Credential IDs now use `java.security.SecureRandom` — cryptographically strong. |
| **Externalised HMAC key** | Secret key loaded from `ddvs.properties` or env var — not hardcoded in source. |
| **Config status endpoint** | `GET /api/config-status` reports auth state, data file path, and record counts. |

---

## Configuration (`ddvs.properties`)

```properties
# HMAC secret key — change this before use
hmac.key=your-strong-secret-key-here

# Comma-separated issuer API keys (leave blank to disable auth)
issuer.api.keys=GEU-ISSUER-KEY-2024

# Path to data persistence file
data.file=ddvs-data.json
```

Override with environment variables: `DDVS_HMAC_KEY`, `DDVS_ISSUER_API_KEYS`, `DDVS_DATA_FILE`

---

## API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/issue` | X-Api-Key (if enabled) | Issue & register a document |
| POST | `/api/verify` | — | Verify by file or credential ID |
| GET | `/api/audit` | — | Full audit log |
| GET | `/api/health` | — | Server uptime & stats |
| GET | `/api/config-status` | — | Auth & persistence status |

---

## Cryptography
| Component  | Algorithm   | Java API            |
|------------|-------------|---------------------|
| Hash       | SHA-256     | MessageDigest       |
| Signature  | HMAC-SHA256 | javax.crypto.Mac    |
| Random IDs | SecureRandom| java.security       |

*No external libraries — pure Java JDK standard library only.*
