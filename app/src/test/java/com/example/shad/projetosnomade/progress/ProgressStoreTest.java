package com.example.shad.projetosnomade.progress;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgressStoreTest {

    private ProgressStore progressStore;

    @Before
    public void setUp() {
        progressStore = new ProgressStore(new InMemoryKeyValueStore());
    }

    @Test
    public void firstLevelOfEachTierIsAlwaysUnlocked() {
        assertTrue(progressStore.isUnlocked("easy_1"));
        assertTrue(progressStore.isUnlocked("medium_1"));
        assertTrue(progressStore.isUnlocked("hard_1"));
    }

    @Test
    public void secondLevelLocksUntilFirstHasAtLeastOneStar() {
        assertFalse(progressStore.isUnlocked("easy_2"));

        progressStore.setStars("easy_1", 1);

        assertTrue(progressStore.isUnlocked("easy_2"));
    }

    @Test
    public void unlockCascadesThroughTheWholeTier() {
        assertFalse(progressStore.isUnlocked("easy_3"));

        progressStore.setStars("easy_1", 1);
        progressStore.setStars("easy_2", 1);

        assertTrue(progressStore.isUnlocked("easy_3"));
    }

    @Test
    public void starsNeverDecrease() {
        progressStore.setStars("easy_1", 3);
        progressStore.setStars("easy_1", 1);

        assertEquals(3, progressStore.getStars("easy_1"));
    }

    @Test
    public void bestScoreOnlyImprovesUpward() {
        progressStore.setBestScore("easy_1", 100);
        progressStore.setBestScore("easy_1", 50);

        assertEquals(100, progressStore.getBestScore("easy_1"));
    }

    @Test
    public void bestTimeAndMovesOnlyImproveDownward() {
        progressStore.setBestTimeMillis("easy_1", 5000L);
        progressStore.setBestTimeMillis("easy_1", 8000L);
        progressStore.setBestMoves("easy_1", 10);
        progressStore.setBestMoves("easy_1", 20);

        assertEquals(5000L, progressStore.getBestTimeMillis("easy_1"));
        assertEquals(10, progressStore.getBestMoves("easy_1"));
    }

    @Test
    public void totalStarsIsOutOfTwentySeven() {
        assertEquals(0, progressStore.getTotalStars());

        for (String levelId : new String[]{"easy_1", "easy_2", "easy_3"}) {
            progressStore.setStars(levelId, 3);
        }

        assertEquals(9, progressStore.getTotalStars());
    }

    @Test
    public void resetProgressClearsStarsAndRelocksLaterLevels() {
        progressStore.setStars("easy_1", 3);
        progressStore.setStars("easy_2", 3);

        progressStore.resetProgress();

        assertEquals(0, progressStore.getStars("easy_1"));
        assertFalse(progressStore.isUnlocked("easy_2"));
    }

    @Test
    public void hapticsAreEnabledByDefaultAndRespectExplicitSetting() {
        assertTrue(progressStore.isHapticsEnabled());

        progressStore.setHapticsEnabled(false);

        assertFalse(progressStore.isHapticsEnabled());
    }
}
