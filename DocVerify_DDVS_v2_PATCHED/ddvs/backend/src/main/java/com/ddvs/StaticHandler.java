package com.ddvs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;

/**
 * Serves the frontend/index.html (and any static assets) when the
 * browser hits http://localhost:8080/.
 * This makes it easy to open the app without a separate server.
 */
public class StaticHandler implements HttpHandler {

    private static final Path FRONTEND_DIR;

    static {
        // Resolve relative to the JAR's working directory
        FRONTEND_DIR = Paths.get(System.getProperty("user.dir"))
                            .resolve("../frontend").normalize();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // Skip API routes — they have their own handlers
        String path = ex.getRequestURI().getPath();
        if (path.startsWith("/api/")) {
            Main.sendJson(ex, 404, "{\"error\":\"Not found\"}");
            return;
        }

        if (path.equals("/") || path.isEmpty()) path = "/index.html";
        if (path.equals("/verify")) path = "/verify.html";

        Path file = FRONTEND_DIR.resolve(path.substring(1)).normalize();

        // Security: prevent path traversal
        if (!file.startsWith(FRONTEND_DIR)) {
            ex.sendResponseHeaders(403, -1);
            return;
        }

        if (!Files.exists(file)) {
            String msg = "Frontend not found. Open frontend/index.html directly.";
            ex.sendResponseHeaders(404, msg.length());
            ex.getResponseBody().write(msg.getBytes());
            ex.getResponseBody().close();
            return;
        }

        String mime = guessMime(file.toString());
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private String guessMime(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css";
        if (name.endsWith(".js"))   return "application/javascript";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
