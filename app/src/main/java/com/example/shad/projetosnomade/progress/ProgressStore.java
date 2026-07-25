package com.example.shad.projetosnomade.progress;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.example.shad.projetosnomade.game.Level;
import com.example.shad.projetosnomade.game.LevelRepository;

import java.util.List;

public final class ProgressStore {

    private final KeyValueStore store;

    public ProgressStore(Context context) {
        this(new SharedPreferencesKeyValueStore(context));
    }

    @VisibleForTesting
    ProgressStore(KeyValueStore store) {
        this.store = store;
    }

    public int getStars(String levelId) {
        return store.getInt(key(levelId, "stars"), 0);
    }

    @VisibleForTesting
    public void setStars(String levelId, int stars) {
        if (stars > getStars(levelId)) {
            store.putInt(key(levelId, "stars"), stars);
        }
    }

    public int getBestScore(String levelId) {
        return store.getInt(key(levelId, "bestScore"), 0);
    }

    @VisibleForTesting
    public void setBestScore(String levelId, int score) {
        if (score > getBestScore(levelId)) {
            store.putInt(key(levelId, "bestScore"), score);
        }
    }

    public long getBestTimeMillis(String levelId) {
        return store.getLong(key(levelId, "bestTimeMillis"), Long.MAX_VALUE);
    }

    @VisibleForTesting
    public void setBestTimeMillis(String levelId, long timeMillis) {
        if (timeMillis < getBestTimeMillis(levelId)) {
            store.putLong(key(levelId, "bestTimeMillis"), timeMillis);
        }
    }

    public int getBestMoves(String levelId) {
        return store.getInt(key(levelId, "bestMoves"), Integer.MAX_VALUE);
    }

    @VisibleForTesting
    public void setBestMoves(String levelId, int moves) {
        if (moves < getBestMoves(levelId)) {
            store.putInt(key(levelId, "bestMoves"), moves);
        }
    }

    public void recordResult(String levelId, int stars, int score, long timeMillis, int moves) {
        setStars(levelId, stars);
        setBestScore(levelId, score);
        setBestTimeMillis(levelId, timeMillis);
        setBestMoves(levelId, moves);
    }

    public boolean isUnlocked(String levelId) {
        Level level = LevelRepository.byId(levelId);
        List<Level> tier = LevelRepository.forDifficulty(level.difficulty);
        int index = tier.indexOf(level);
        if (index <= 0) {
            return true;
        }
        return getStars(tier.get(index - 1).id) >= 1;
    }

    public int getTotalStars() {
        int total = 0;
        for (Level level : LevelRepository.all()) {
            total += getStars(level.id);
        }
        return total;
    }

    public void resetProgress() {
        store.clear();
    }

    private static String key(String levelId, String field) {
        return levelId + "." + field;
    }
}
