package com.ddvs;

import java.util.HashMap;
import java.util.Map;

/** Simple data container produced by MultipartParser. */
public class MultipartData {
    public byte[]             fileBytes   = null;
    public String             fileNameRaw = null;
    public Map<String,String> fields      = new HashMap<>();

    /** Get a text field value, with a fallback default. */
    public String get(String key, String defaultValue) {
        return fields.getOrDefault(key, defaultValue);
    }
}
