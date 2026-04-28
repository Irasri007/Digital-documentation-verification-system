package com.ddvs;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Cryptographic helpers used by the verification engine.
 *
 *  • SHA-256 hex digest of raw bytes
 *  • HMAC-SHA256 sign   (produces hex string)
 *  • HMAC-SHA256 verify (constant-time comparison)
 *  • Credential-ID generator  — uses SecureRandom (cryptographically strong)
 *  • HMAC key loaded from Config (env var / properties file — NOT hardcoded)
 */
public class CryptoUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] CRED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    // ── SHA-256 → lower-case hex string ──────────────────────────────────────
    public static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ── HMAC-SHA256 sign → lower-case hex string ──────────────────────────────
    public static String hmacSign(String data) throws Exception {
        String key = Config.get().getHmacKey();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder(64);
        for (byte b : raw) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ── HMAC-SHA256 verify (constant-time) ────────────────────────────────────
    public static boolean hmacVerify(String data, String expectedHex) throws Exception {
        String computed = hmacSign(data);
        // Constant-time comparison to prevent timing attacks
        if (computed.length() != expectedHex.length()) return false;
        int diff = 0;
        for (int i = 0; i < computed.length(); i++) {
            diff |= computed.charAt(i) ^ expectedHex.charAt(i);
        }
        return diff == 0;
    }

    // ── Unique Credential ID generator (SecureRandom) ─────────────────────────
    public static String generateCredentialId() {
        // 8 random alphanumeric chars from SecureRandom — cryptographically strong
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CRED_CHARS[SECURE_RANDOM.nextInt(CRED_CHARS.length)]);
        }
        int year = java.time.Year.now().getValue();
        return "CRED-GEU-" + year + "-" + sb;
    }
}
