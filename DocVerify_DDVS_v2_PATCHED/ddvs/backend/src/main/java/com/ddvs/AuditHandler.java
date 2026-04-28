package com.ddvs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /api/audit
 * Returns all audit log entries as a JSON array.
 */
public class AuditHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Main.handlePreflight(ex)) return;

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            Main.sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        List<Registry.AuditEntry> log = Registry.get().getAuditLog();
        String json = log.stream()
                         .map(Registry.AuditEntry::toJson)
                         .collect(Collectors.joining(",", "[", "]"));

        Main.sendJson(ex, 200, json);
    }
}
