package com.example.aftersale.common.observability;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;

public final class MdcScope implements AutoCloseable {

    private final Map<String, String> previousValues = new LinkedHashMap<>();
    private final Set<String> absentKeys = new LinkedHashSet<>();

    private MdcScope(Map<String, ?> values) {
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = stringValue(entry.getValue());
            if (key == null || key.isBlank() || value.isBlank()) {
                continue;
            }
            String previousValue = MDC.get(key);
            if (previousValue == null) {
                absentKeys.add(key);
            } else {
                previousValues.put(key, previousValue);
            }
            MDC.put(key, value);
        }
    }

    public static MdcScope put(String key, Object value) {
        return new MdcScope(Map.of(key, value));
    }

    public static MdcScope putAll(Map<String, ?> values) {
        return new MdcScope(values);
    }

    @Override
    public void close() {
        for (String key : absentKeys) {
            MDC.remove(key);
        }
        for (Map.Entry<String, String> entry : previousValues.entrySet()) {
            MDC.put(entry.getKey(), entry.getValue());
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
