package com.ddvs;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        // Trigger Config load early so startup messages appear in order
        Config cfg = Config.get();

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/issue",         new IssueHandler());
        server.createContext("/api/verify",        new VerifyHandler());
        server.createContext("/api/audit",         new AuditHandler());
        server.createContext("/api/health",        new HealthHandler());
        server.createContext("/api/config-status", new ConfigStatusHandler());
        server.createContext("/",                  new StaticHandler());

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        // ✅ FIXED PRINTS (ASCII only)
        System.out.println("==========================================");
        System.out.println("   DocVerify Backend - Java REST Server   ");
        System.out.println("   Running at http://localhost:" + port);
        System.out.println("==========================================");
        System.out.println();
        System.out.println("Endpoints:");
        System.out.println("  POST /api/issue          - Issue & register a document");
        System.out.println("  POST /api/verify         - Verify document hash or credential");
        System.out.println("  GET  /api/audit          - View audit log");
        System.out.println("  GET  /api/health         - Health check");
        System.out.println("  GET  /api/config-status  - Auth & persistence status");
        System.out.println();
        System.out.println("Issuer auth : " + (cfg.isIssuerAuthEnabled() ? "ENABLED" : "DISABLED (set issuer.api.keys in ddvs.properties)"));
        System.out.println("Data file   : " + cfg.getDataFilePath());
        System.out.println();
        System.out.println("Open frontend/index.html in your browser.");
        System.out.println("Press Ctrl+C to stop.");
    }

    // ── Shared CORS helper ────────────────────────────────────────────────────
    public static void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-Api-Key");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    }

    public static boolean handlePreflight(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            addCorsHeaders(ex);
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    public static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        addCorsHeaders(ex);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}