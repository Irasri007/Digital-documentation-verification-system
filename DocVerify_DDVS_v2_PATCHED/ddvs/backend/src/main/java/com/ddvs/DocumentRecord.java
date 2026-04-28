package com.ddvs;

public class DocumentRecord {
    public String credentialId;
    public String hash;
    public String signature;
    public String fileName;
    public long   fileSize;
    public String fileType;
    public String issuedAt;
    public String issuer;

    public DocumentRecord() {}

    public DocumentRecord(String credentialId, String hash, String signature,
                          String fileName, long fileSize, String fileType,
                          String issuedAt, String issuer) {
        this.credentialId = credentialId;
        this.hash         = hash;
        this.signature    = signature;
        this.fileName     = fileName;
        this.fileSize     = fileSize;
        this.fileType     = fileType;
        this.issuedAt     = issuedAt;
        this.issuer       = issuer;
    }

    /** Serialise to a JSON object string (no external library needed). */
    public String toJson() {
        return String.format(
            "{\"credentialId\":\"%s\",\"hash\":\"%s\",\"signature\":\"%s\"," +
            "\"fileName\":\"%s\",\"fileSize\":%d,\"fileType\":\"%s\"," +
            "\"issuedAt\":\"%s\",\"issuer\":\"%s\"}",
            esc(credentialId), esc(hash), esc(signature),
            esc(fileName), fileSize, esc(fileType),
            esc(issuedAt), esc(issuer)
        );
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}
