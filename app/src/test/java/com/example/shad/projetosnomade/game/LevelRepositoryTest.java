package com.example.shad.projetosnomade.game;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LevelRepositoryTest {

    @Test
    public void thereAreNineLevelsInTotal() {
        assertEquals(9, LevelRepository.all().size());
    }

    @Test
    public void thereAreThreeLevelsPerDifficulty() {
        assertEquals(3, LevelRepository.forDifficulty(Difficulty.EASY).size());
        assertEquals(3, LevelRepository.forDifficulty(Difficulty.MEDIUM).size());
        assertEquals(3, LevelRepository.forDifficulty(Difficulty.HARD).size());
    }

    @Test
    public void levelIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (Level level : LevelRepository.all()) {
            assertTrue("Duplicate level id: " + level.id, ids.add(level.id));
        }
    }

    @Test
    public void byIdReturnsTheMatchingLevel() {
        Level level = LevelRepository.byId("hard_3");
        assertEquals(Difficulty.HARD, level.difficulty);
        assertEquals(6, level.gridSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void byIdThrowsForUnknownId() {
        LevelRepository.byId("does_not_exist");
    }

    @Test
    public void gridSizeGrowsWithinEachDifficultyOrder() {
        List<Level> medium = LevelRepository.forDifficulty(Difficulty.MEDIUM);
        assertEquals(4, medium.get(0).gridSize);
        assertEquals(5, medium.get(1).gridSize);
        assertEquals(5, medium.get(2).gridSize);
    }
}
