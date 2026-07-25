package com.example.shad.projetosnomade.progress;

import java.util.HashMap;
import java.util.Map;

final class InMemoryKeyValueStore implements KeyValueStore {

    private final Map<String, Object> values = new HashMap<>();

    @Override
    public int getInt(String key, int defaultValue) {
        Object value = values.get(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    @Override
    public void putInt(String key, int value) {
        values.put(key, value);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        Object value = values.get(key);
        return value instanceof Long ? (Long) value : defaultValue;
    }

    @Override
    public void putLong(String key, long value) {
        values.put(key, value);
    }

    @Override
    public void clear() {
        values.clear();
    }
}
