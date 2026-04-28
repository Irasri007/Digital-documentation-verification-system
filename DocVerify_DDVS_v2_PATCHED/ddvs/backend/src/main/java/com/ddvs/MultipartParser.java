package com.ddvs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal multipart/form-data parser.
 * Extracts the binary file part and named text fields.
 * No external library required.
 */
public class MultipartParser {

    public static MultipartData parse(byte[] body, String boundary) throws Exception {
        MultipartData data = new MultipartData();
        byte[] delimiter   = ("--" + boundary).getBytes("UTF-8");

        // Split body on boundary
        int[] parts = findAll(body, delimiter);
        for (int i = 0; i < parts.length - 1; i++) {
            int start = parts[i] + delimiter.length;
            int end   = parts[i + 1];
            if (start >= end) continue;

            // Skip CRLF after boundary
            if (start + 2 <= body.length
                    && body[start] == '\r' && body[start+1] == '\n') start += 2;

            // Split headers from content on first blank line (\r\n\r\n)
            byte[] blankLine = "\r\n\r\n".getBytes("UTF-8");
            int sep = indexOf(body, blankLine, start, end);
            if (sep < 0) continue;

            String headers  = new String(body, start, sep - start, "UTF-8");
            int    bodyStart = sep + blankLine.length;
            int    bodyEnd   = end;
            // Strip trailing \r\n before next boundary
            if (bodyEnd > 1 && body[bodyEnd-2] == '\r' && body[bodyEnd-1] == '\n') bodyEnd -= 2;

            // Parse Content-Disposition
            String name = extractParam(headers, "name");
            String fname = extractParam(headers, "filename");

            if (fname != null && !fname.isEmpty()) {
                // This is the file part
                data.fileBytes   = Arrays.copyOfRange(body, bodyStart, bodyEnd);
                data.fileNameRaw = fname;
            } else if (name != null) {
                // Text field
                String value = new String(body, bodyStart, bodyEnd - bodyStart, "UTF-8");
                data.fields.put(name, value);
            }
        }
        return data;
    }

    private static String extractParam(String headers, String paramName) {
        String lower = headers.toLowerCase();
        String search = paramName + "=\"";
        int idx = lower.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        int end   = headers.indexOf('"', start);
        if (end < 0) return null;
        return headers.substring(start, end);
    }

    private static int[] findAll(byte[] data, byte[] pattern) {
        java.util.List<Integer> positions = new java.util.ArrayList<>();
        positions.add(0);
        for (int i = 0; i <= data.length - pattern.length; i++) {
            if (matches(data, i, pattern)) {
                positions.add(i);
            }
        }
        positions.add(data.length);
        return positions.stream().mapToInt(x -> x).toArray();
    }

    private static int indexOf(byte[] data, byte[] pattern, int from, int to) {
        outer:
        for (int i = from; i <= to - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static boolean matches(byte[] data, int offset, byte[] pattern) {
        if (offset + pattern.length > data.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) return false;
        }
        return true;
    }
}
