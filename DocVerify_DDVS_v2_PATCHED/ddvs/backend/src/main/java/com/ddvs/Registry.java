package com.ddvs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe credential registry and audit log with JSON file persistence.
 *
 * Data is written to ddvs-data.json (path from Config) on every store/log
 * and loaded back automatically on startup — so records survive server restarts.
 *
 * Format:
 * {
 *   "documents": [ { ...DocumentRecord fields... }, ... ],
 *   "auditLog":  [ { ...AuditEntry fields... }, ... ]
 * }
 */
public class Registry {

    private static final Registry INSTANCE = new Registry();
    public  static Registry get() { return INSTANCE; }

    private final Map<String, DocumentRecord> byCredentialId = new ConcurrentHashMap<>();
    private final Map<String, DocumentRecord> byHash         = new ConcurrentHashMap<>();
    private final List<AuditEntry>            auditLog       = Collections.synchronizedList(new ArrayList<>());
    private final ReentrantLock               fileLock       = new ReentrantLock();

    private Registry() {
        loadFromDisk();
    }

    // ── Store a newly issued document ─────────────────────────────────────────
    public void store(DocumentRecord rec) {
        byCredentialId.put(rec.credentialId, rec);
        byHash.put(rec.hash, rec);
        saveToDisk();
    }

    // ── Lookup by credential ID ───────────────────────────────────────────────
    public DocumentRecord findByCredentialId(String credId) {
        return byCredentialId.get(credId);
    }

    // ── Lookup by SHA-256 hash ────────────────────────────────────────────────
    public DocumentRecord findByHash(String hash) {
        return byHash.get(hash);
    }

    public int size() { return byCredentialId.size(); }

    // ── Audit log ─────────────────────────────────────────────────────────────
    public void log(AuditEntry entry) {
        auditLog.add(entry);
        saveToDisk();
    }

    public List<AuditEntry> getAuditLog() {
        return Collections.unmodifiableList(auditLog);
    }

    // ── Persistence: save to JSON file ────────────────────────────────────────
    private void saveToDisk() {
        fileLock.lock();
        try {
            Path path = Config.get().getDataFilePath();
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"documents\": [\n");
            List<DocumentRecord> docs = new ArrayList<>(byCredentialId.values());
            for (int i = 0; i < docs.size(); i++) {
                sb.append("    ").append(docs.get(i).toJson());
                if (i < docs.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n  \"auditLog\": [\n");
            List<AuditEntry> entries = new ArrayList<>(auditLog);
            for (int i = 0; i < entries.size(); i++) {
                sb.append("    ").append(entries.get(i).toJson());
                if (i < entries.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n}");
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("[Registry] Failed to save data: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    // ── Persistence: load from JSON file ─────────────────────────────────────
    private void loadFromDisk() {
        Path path = Config.get().getDataFilePath();
        if (!Files.exists(path)) {
            System.out.println("[Registry] No data file found at " + path + " — starting fresh.");
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            // Parse documents array
            String docsSection = extractArraySection(json, "documents");
            if (docsSection != null) {
                List<String> docObjects = splitJsonObjects(docsSection);
                for (String obj : docObjects) {
                    DocumentRecord rec = parseDocumentRecord(obj);
                    if (rec != null && rec.credentialId != null && rec.hash != null) {
                        byCredentialId.put(rec.credentialId, rec);
                        byHash.put(rec.hash, rec);
                    }
                }
            }
            // Parse auditLog array
            String auditSection = extractArraySection(json, "auditLog");
            if (auditSection != null) {
                List<String> auditObjects = splitJsonObjects(auditSection);
                for (String obj : auditObjects) {
                    AuditEntry entry = parseAuditEntry(obj);
                    if (entry != null) auditLog.add(entry);
                }
            }
            System.out.println("[Registry] Loaded " + byCredentialId.size() +
                " document(s) and " + auditLog.size() + " audit entry(s) from " + path);
        } catch (Exception e) {
            System.err.println("[Registry] Failed to load data: " + e.getMessage());
        }
    }

    // ── Minimal JSON array section extractor ─────────────────────────────────
    private static String extractArraySection(String json, String key) {
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        int bracketStart = json.indexOf('[', idx);
        if (bracketStart < 0) return null;
        int depth = 0;
        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return json.substring(bracketStart + 1, i); }
        }
        return null;
    }

    // ── Split JSON array content into individual {...} objects ────────────────
    private static List<String> splitJsonObjects(String arrayContent) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start >= 0) { result.add(arrayContent.substring(start, i + 1)); start = -1; } }
        }
        return result;
    }

    // ── Parse a DocumentRecord from a JSON object string ─────────────────────
    private static DocumentRecord parseDocumentRecord(String json) {
        try {
            Map<String, String> m = JsonParser.parse(json);
            DocumentRecord rec = new DocumentRecord();
            rec.credentialId = m.get("credentialId");
            rec.hash         = m.get("hash");
            rec.signature    = m.get("signature");
            rec.fileName     = m.get("fileName");
            rec.fileType     = m.get("fileType");
            rec.issuedAt     = m.get("issuedAt");
            rec.issuer       = m.get("issuer");
            String fs = m.get("fileSize");
            rec.fileSize = (fs != null && !fs.isEmpty()) ? Long.parseLong(fs) : 0;
            return rec;
        } catch (Exception e) { return null; }
    }

    // ── Parse an AuditEntry from a JSON object string ────────────────────────
    private static AuditEntry parseAuditEntry(String json) {
        try {
            Map<String, String> m = JsonParser.parse(json);
            return new AuditEntry(m.get("action"), m.get("credentialId"),
                m.get("fileName"), m.get("result"), m.get("at"));
        } catch (Exception e) { return null; }
    }

    // ── Audit entry inner class ───────────────────────────────────────────────
    public static class AuditEntry {
        public final String action;
        public final String credentialId;
        public final String fileName;
        public final String result;
        public final String at;

        public AuditEntry(String action, String credentialId,
                          String fileName, String result, String at) {
            this.action       = action;
            this.credentialId = credentialId;
            this.fileName     = fileName;
            this.result       = result;
            this.at           = at;
        }

        public String toJson() {
            return String.format(
                "{\"action\":\"%s\",\"credentialId\":\"%s\",\"fileName\":\"%s\"," +
                "\"result\":\"%s\",\"at\":\"%s\"}",
                esc(action), esc(credentialId), esc(fileName), esc(result), esc(at));
        }

        private static String esc(String s) {
            if (s == null) return "";
            return s.replace("\\","\\\\").replace("\"","\\\"");
        }
    }
}
