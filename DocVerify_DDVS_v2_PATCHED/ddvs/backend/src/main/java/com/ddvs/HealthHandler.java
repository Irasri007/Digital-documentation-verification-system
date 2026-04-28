package com.ddvs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.Instant;

/**
 * GET /api/health
 * Returns server status and registry stats.
 */
public class HealthHandler implements HttpHandler {

    private final long startTime = System.currentTimeMillis();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Main.handlePreflight(ex)) return;

        long uptimeSec = (System.currentTimeMillis() - startTime) / 1000;
        String json = String.format(
            "{\"status\":\"ok\",\"server\":\"DocVerify Java Backend\"," +
            "\"version\":\"1.0.0\",\"documentsIssued\":%d," +
            "\"auditEntries\":%d,\"uptimeSeconds\":%d,\"timestamp\":\"%s\"}",
            Registry.get().size(),
            Registry.get().getAuditLog().size(),
            uptimeSec,
            Instant.now().toString()
        );
        Main.sendJson(ex, 200, json);
    }
}
