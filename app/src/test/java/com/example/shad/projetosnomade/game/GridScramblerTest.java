package com.example.shad.projetosnomade.game;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GridScramblerTest {

    @Test
    public void everyLevelIsSolvableByReplayingInverseMoves() {
        for (Level level : LevelRepository.all()) {
            assertTrue("Level " + level.id + " is not solvable", GridScrambler.verifySolvable(level));
        }
    }

    @Test
    public void everyLevelScramblesToSomethingDifferentFromTarget() {
        for (Level level : LevelRepository.all()) {
            int[][] scrambled = GridScrambler.scramble(level);
            assertFalse("Level " + level.id + " scrambled grid equals target",
                    gridsEqual(scrambled, level.target));
        }
    }

    @Test
    public void everyLevelPreservesValueCounts() {
        for (Level level : LevelRepository.all()) {
            int[][] scrambled = GridScrambler.scramble(level);
            assertEqualsCount(level.target, scrambled);
        }
    }

    @Test
    public void sameSeedProducesSameGrid() {
        Level level = LevelRepository.byId("hard_2");

        int[][] first = GridScrambler.scramble(level);
        int[][] second = GridScrambler.scramble(level);

        assertArrayEquals(first, second);
    }

    @Test
    public void scrambleNeverImmediatelyUndoesThePreviousMove() {
        Level level = LevelRepository.byId("medium_2");
        GridScrambler.ScrambleResult result = GridScrambler.scrambleWithHistory(level);
        List<GridScrambler.Move> moves = result.moves;

        for (int i = 1; i < moves.size(); i++) {
            GridScrambler.Move previous = moves.get(i - 1);
            GridScrambler.Move current = moves.get(i);
            boolean isReverse = previous.axis == current.axis
                    && previous.index == current.index
                    && previous.direction == -current.direction;
            assertFalse("Move " + i + " undoes move " + (i - 1) + " for level " + level.id, isReverse);
        }
    }

    private static void assertEqualsCount(int[][] a, int[][] b) {
        int countA = 0;
        int countB = 0;
        for (int[] row : a) {
            for (int value : row) {
                if (value == 1) {
                    countA++;
                }
            }
        }
        for (int[] row : b) {
            for (int value : row) {
                if (value == 1) {
                    countB++;
                }
            }
        }
        org.junit.Assert.assertEquals(countA, countB);
    }

    private static boolean gridsEqual(int[][] a, int[][] b) {
        for (int row = 0; row < a.length; row++) {
            for (int column = 0; column < a[row].length; column++) {
                if (a[row][column] != b[row][column]) {
                    return false;
                }
            }
        }
        return true;
    }
}
