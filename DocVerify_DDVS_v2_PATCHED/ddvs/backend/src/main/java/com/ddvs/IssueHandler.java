package com.ddvs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.time.Instant;
import java.util.Map;

public class IssueHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Main.handlePreflight(ex)) return;

        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            Main.sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        // ── Issuer authentication ──────────────────────────────────────────────
        if (Config.get().isIssuerAuthEnabled()) {
            String apiKey = ex.getRequestHeaders().getFirst("X-Api-Key");
            if (!Config.get().isValidApiKey(apiKey)) {
                Main.sendJson(ex, 401,
                    "{\"error\":\"Unauthorized. Provide a valid X-Api-Key header.\"}");
                System.out.println("[ISSUE] Rejected — invalid or missing API key");
                return;
            }
        }

        try {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");

            byte[] fileBytes;
            String fileName, fileType, issuer;
            long   fileSize;

            if (contentType != null && contentType.contains("multipart/form-data")) {
                String boundary = extractBoundary(contentType);
                byte[] body     = ex.getRequestBody().readAllBytes();
                MultipartData mp = MultipartParser.parse(body, boundary);

                fileBytes = mp.fileBytes;
                fileName  = mp.get("fileName", "unknown");
                fileType  = mp.get("fileType",  "application/octet-stream");
                issuer    = IssuerDetector.detect(fileName, fileType, mp.get("issuer", "Graphic Era University"));
                fileSize  = fileBytes != null ? fileBytes.length : 0;

            } else {
                String body = new String(ex.getRequestBody().readAllBytes(), "UTF-8");
                Map<String,String> json = JsonParser.parse(body);
                String b64  = json.getOrDefault("fileBase64", "");
                fileBytes   = java.util.Base64.getDecoder().decode(b64);
                fileName    = json.getOrDefault("fileName",  "unknown");
                fileType    = json.getOrDefault("fileType",  "application/octet-stream");
                issuer      = IssuerDetector.detect(fileName, fileType, json.getOrDefault("issuer", "Graphic Era University"));
                fileSize    = fileBytes.length;
            }

            if (fileBytes == null || fileBytes.length == 0) {
                Main.sendJson(ex, 400, "{\"error\":\"No file data received\"}");
                return;
            }

            // 1. SHA-256 the raw bytes
            String hash        = CryptoUtil.sha256Hex(fileBytes);
            // 2. HMAC-SHA256 sign the hash
            String signature   = CryptoUtil.hmacSign(hash);
            // 3. Generate unique Credential ID
            String credId      = CryptoUtil.generateCredentialId();
            String issuedAt    = Instant.now().toString();

            //  ADDED: Generate verification URL
            String verifyUrl = "http://localhost:8080/verify.html?id=" + credId;

            //  ADDED: Generate QR (base64)
            String qrBase64 = QRUtil.generateQRBase64(verifyUrl);

            // 4. Store in registry
            DocumentRecord rec = new DocumentRecord(
                credId, hash, signature, fileName,
                fileSize, fileType, issuedAt, issuer
            );
            Registry.get().store(rec);

            // 5. Audit log
            Registry.get().log(new Registry.AuditEntry(
                "ISSUE", credId, fileName, "SUCCESS", issuedAt
            ));

            System.out.printf("[ISSUE] %s | %s | %s%n", credId, fileName, hash.substring(0,16) + "…");

            //  MODIFIED: added "qr" field
            String resp = String.format(
                "{\"success\":true,\"credentialId\":\"%s\",\"hash\":\"%s\"," +
                "\"signature\":\"%s\",\"issuedAt\":\"%s\",\"issuer\":\"%s\"," +
                "\"fileName\":\"%s\",\"fileSize\":%d," +
                "\"qr\":\"%s\"}",
                credId, hash, signature, issuedAt,
                esc(issuer), esc(fileName), fileSize,
                qrBase64   //  ADDED HERE
            );

            Main.sendJson(ex, 200, resp);

        } catch (Exception e) {
            e.printStackTrace();
            Main.sendJson(ex, 500,
                "{\"error\":\"Internal error: " + esc(e.getMessage()) + "\"}");
        }
    }

    private static String extractBoundary(String ct) {
        for (String part : ct.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                return part.substring("boundary=".length()).trim();
            }
        }
        return "";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}