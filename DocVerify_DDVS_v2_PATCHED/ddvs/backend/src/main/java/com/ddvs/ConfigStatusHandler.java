package com.ddvs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;

/**
 * GET /api/config-status
 * Returns current server configuration state (auth enabled, data file path, etc.)
 * Does NOT expose secrets.
 */
public class ConfigStatusHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Main.handlePreflight(ex)) return;

        Config cfg = Config.get();
        boolean authEnabled = cfg.isIssuerAuthEnabled();
        boolean dataFileExists = Files.exists(cfg.getDataFilePath());
        String dataFilePath = cfg.getDataFilePath().toString();

        String json = String.format(
            "{\"issuerAuthEnabled\":%b,\"dataFileExists\":%b,\"dataFilePath\":\"%s\"," +
            "\"documentsIssued\":%d,\"auditEntries\":%d}",
            authEnabled,
            dataFileExists,
            esc(dataFilePath),
            Registry.get().size(),
            Registry.get().getAuditLog().size()
        );
        Main.sendJson(ex, 200, json);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "/").replace("\"", "\\\"");
    }
}
