package com.ddvs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.time.Instant;
import java.util.Map;

/**
 * POST /api/verify
 *
 * Two modes:
 *   1. multipart/form-data  → file upload  → compute hash → lookup by hash
 *   2. application/json     → { "mode":"credential", "credentialId":"CRED-..." }
 *
 * Response JSON fields consumed by the frontend:
 *   status          : "verified" | "tampered" | "not_issued" | "invalid"
 *   hashMatch       : boolean
 *   signatureValid  : boolean
 *   credentialFound : boolean
 *   issuerTrusted   : boolean
 *   computedHash    : string  (hex, file mode only)
 *   storedHash      : string  (hex)
 *   credentialId    : string
 *   fileName        : string
 *   fileSize        : number
 *   issuedAt        : string
 *   issuer          : string
 *   signature       : string
 */
public class VerifyHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Main.handlePreflight(ex)) return;

        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            Main.sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");

            // ── MODE 1: File upload (multipart) ──────────────────────────────
            if (contentType != null && contentType.contains("multipart/form-data")) {
                String boundary = extractBoundary(contentType);
                byte[] body     = ex.getRequestBody().readAllBytes();
                MultipartData mp = MultipartParser.parse(body, boundary);

                byte[] fileBytes = mp.fileBytes;
                String fileName  = mp.get("fileName", "unknown");

                if (fileBytes == null || fileBytes.length == 0) {
                    Main.sendJson(ex, 400, "{\"error\":\"No file data received\"}");
                    return;
                }

                // Compute hash of uploaded bytes
                String computedHash = CryptoUtil.sha256Hex(fileBytes);

                // Look up in registry by hash
                DocumentRecord rec = Registry.get().findByHash(computedHash);

                String status;
                boolean hashMatch       = false;
                boolean signatureValid  = false;
                boolean credentialFound = false;
                boolean issuerTrusted   = false;

                if (rec == null) {
                    // Hash not in registry → never issued
                    status = "not_issued";
                } else {
                    // Hash matched a stored record
                    hashMatch       = true;
                    credentialFound = true;
                    issuerTrusted   = true;
                    signatureValid  = CryptoUtil.hmacVerify(rec.hash, rec.signature);
                    status          = signatureValid ? "verified" : "tampered";
                }

                // Audit log
                Registry.get().log(new Registry.AuditEntry(
                    "VERIFY_FILE",
                    rec != null ? rec.credentialId : "N/A",
                    fileName,
                    status.toUpperCase(),
                    Instant.now().toString()
                ));

                System.out.printf("[VERIFY] FILE | %s | hash:%s… | %s%n",
                    fileName,
                    computedHash.substring(0, 16),
                    status.toUpperCase());

                Main.sendJson(ex, 200, buildResponse(
                    status, hashMatch, signatureValid, credentialFound, issuerTrusted,
                    computedHash, rec != null ? rec.hash : null, rec
                ));

            // ── MODE 2: Credential ID lookup (JSON) ───────────────────────────
            } else {
                String bodyStr = new String(ex.getRequestBody().readAllBytes(), "UTF-8");
                Map<String, String> json = JsonParser.parse(bodyStr);
                String credId = json.getOrDefault("credentialId", "").trim().toUpperCase();

                if (credId.isEmpty()) {
                    Main.sendJson(ex, 400, "{\"error\":\"credentialId is required\"}");
                    return;
                }

                DocumentRecord rec = Registry.get().findByCredentialId(credId);

                String  status;
                boolean hashMatch       = false;
                boolean signatureValid  = false;
                boolean credentialFound = false;
                boolean issuerTrusted   = false;

                if (rec == null) {
                    status = "invalid";
                } else {
                    credentialFound = true;
                    issuerTrusted   = true;
                    hashMatch       = true; // hash is the stored one, trivially matches itself
                    signatureValid  = CryptoUtil.hmacVerify(rec.hash, rec.signature);
                    status          = signatureValid ? "verified" : "tampered";
                }

                Registry.get().log(new Registry.AuditEntry(
                    "VERIFY_CRED",
                    credId,
                    rec != null ? rec.fileName : "unknown",
                    status.toUpperCase(),
                    Instant.now().toString()
                ));

                System.out.printf("[VERIFY] CRED | %s | %s%n", credId, status.toUpperCase());

                Main.sendJson(ex, 200, buildResponse(
                    status, hashMatch, signatureValid, credentialFound, issuerTrusted,
                    rec != null ? rec.hash : null,
                    rec != null ? rec.hash : null,
                    rec
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Main.sendJson(ex, 500,
                "{\"error\":\"Internal error: " + esc(e.getMessage()) + "\"}");
        }
    }

    // ── Build JSON response ────────────────────────────────────────────────────
    private static String buildResponse(
            String status, boolean hashMatch, boolean signatureValid,
            boolean credentialFound, boolean issuerTrusted,
            String computedHash, String storedHash, DocumentRecord rec) {

        return String.format(
            "{\"status\":\"%s\"," +
            "\"hashMatch\":%b," +
            "\"signatureValid\":%b," +
            "\"credentialFound\":%b," +
            "\"issuerTrusted\":%b," +
            "\"computedHash\":\"%s\"," +
            "\"storedHash\":\"%s\"," +
            "\"credentialId\":\"%s\"," +
            "\"fileName\":\"%s\"," +
            "\"fileSize\":%d," +
            "\"issuedAt\":\"%s\"," +
            "\"issuer\":\"%s\"," +
            "\"signature\":\"%s\"}",
            status,
            hashMatch,
            signatureValid,
            credentialFound,
            issuerTrusted,
            computedHash  != null ? computedHash  : "",
            storedHash    != null ? storedHash    : "",
            rec != null ? esc(rec.credentialId) : "",
            rec != null ? esc(rec.fileName)     : "",
            rec != null ? rec.fileSize          : 0,
            rec != null ? esc(rec.issuedAt)     : "",
            rec != null ? esc(rec.issuer)       : "",
            rec != null ? esc(rec.signature)    : ""
        );
    }

    private static String extractBoundary(String ct) {
        for (String part : ct.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary="))
                return part.substring("boundary=".length()).trim();
        }
        return "";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}