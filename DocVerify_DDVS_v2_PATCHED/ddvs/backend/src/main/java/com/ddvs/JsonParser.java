package com.ddvs;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal JSON key→value extractor for flat objects only.
 * No external library required — keeps the project dependency-free.
 *
 * Supports:  {"key":"stringValue", "key2": 123, "key3": true}
 * Does NOT handle nested objects / arrays (not needed here).
 */
public class JsonParser {

    public static Map<String, String> parse(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;

        // Strip outer braces
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))   json = json.substring(0, json.length() - 1);

        // Split on commas that are NOT inside quotes
        // Simple state-machine approach
        boolean inString = false;
        int depth = 0;
        int start = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i-1) != '\\')) inString = !inString;
            if (!inString) {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;
                if (c == ',' && depth == 0) {
                    parsePair(json.substring(start, i).trim(), map);
                    start = i + 1;
                }
            }
        }
        parsePair(json.substring(start).trim(), map);
        return map;
    }

    private static void parsePair(String pair, Map<String, String> map) {
        int colon = pair.indexOf(':');
        if (colon < 0) return;
        String rawKey   = pair.substring(0, colon).trim().replaceAll("^\"|\"$", "");
        String rawValue = pair.substring(colon + 1).trim();
        // Strip quotes from string values; keep raw for numbers/booleans
        if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
            rawValue = rawValue.substring(1, rawValue.length() - 1)
                               .replace("\\\"","\"").replace("\\\\","\\")
                               .replace("\\n","\n").replace("\\r","\r");
        }
        map.put(rawKey, rawValue);
    }
}
