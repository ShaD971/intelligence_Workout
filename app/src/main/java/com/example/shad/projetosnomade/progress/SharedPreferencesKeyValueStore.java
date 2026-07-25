package com.example.shad.projetosnomade.progress;

import android.content.Context;
import android.content.SharedPreferences;

final class SharedPreferencesKeyValueStore implements KeyValueStore {

    private static final String PREFS_NAME = "iw_progress";

    private final SharedPreferences prefs;

    SharedPreferencesKeyValueStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    @Override
    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return prefs.getLong(key, defaultValue);
    }

    @Override
    public void putLong(String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }

    @Override
    public void clear() {
        prefs.edit().clear().apply();
    }
}
