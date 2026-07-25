package com.example.shad.projetosnomade.progress;

interface KeyValueStore {
    int getInt(String key, int defaultValue);

    void putInt(String key, int value);

    long getLong(String key, long defaultValue);

    void putLong(String key, long value);

    void clear();
}
