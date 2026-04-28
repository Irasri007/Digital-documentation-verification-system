package com.ddvs;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Loads configuration from ddvs.properties (next to the run script).
 *
 * Properties read:
 *   hmac.key          — secret key for HMAC-SHA256 signatures
 *   issuer.api.keys   — comma-separated list of valid API keys for /api/issue
 *   data.file         — path to the JSON persistence file (default: ddvs-data.json)
 *
 * Environment variables override properties file:
 *   DDVS_HMAC_KEY, DDVS_ISSUER_API_KEYS, DDVS_DATA_FILE
 */
public class Config {

    private static final Config INSTANCE = new Config();
    public static Config get() { return INSTANCE; }

    private final Properties props = new Properties();

    private Config() {
        // 1. Try to load ddvs.properties from working directory
        Path cfgPath = Paths.get(System.getProperty("user.dir")).resolve("ddvs.properties");
        if (Files.exists(cfgPath)) {
            try (InputStream is = Files.newInputStream(cfgPath)) {
                props.load(is);
                System.out.println("[Config] Loaded " + cfgPath);
            } catch (Exception e) {
                System.err.println("[Config] Could not load ddvs.properties: " + e.getMessage());
            }
        } else {
            System.out.println("[Config] ddvs.properties not found at " + cfgPath + " — using defaults / env vars");
        }
    }

    // ── HMAC key ──────────────────────────────────────────────────────────────
    public String getHmacKey() {
        String env = System.getenv("DDVS_HMAC_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        return props.getProperty("hmac.key", "docverify-secret-key-geu-2024");
    }

    // ── Issuer API keys ───────────────────────────────────────────────────────
    public Set<String> getIssuerApiKeys() {
        String env = System.getenv("DDVS_ISSUER_API_KEYS");
        String raw = (env != null && !env.isBlank())
            ? env
            : props.getProperty("issuer.api.keys", "");

        Set<String> keys = new HashSet<>();
        if (raw == null || raw.isBlank()) return keys;
        for (String k : raw.split(",")) {
            String t = k.trim();
            if (!t.isEmpty()) keys.add(t);
        }
        return keys;
    }

    public boolean isIssuerAuthEnabled() {
        return !getIssuerApiKeys().isEmpty();
    }

    public boolean isValidApiKey(String key) {
        if (!isIssuerAuthEnabled()) return true; // auth disabled — allow all
        return key != null && getIssuerApiKeys().contains(key.trim());
    }

    // ── Data file path ────────────────────────────────────────────────────────
    public Path getDataFilePath() {
        String env = System.getenv("DDVS_DATA_FILE");
        String raw = (env != null && !env.isBlank())
            ? env
            : props.getProperty("data.file", "ddvs-data.json");
        return Paths.get(System.getProperty("user.dir")).resolve(raw);
    }
}
