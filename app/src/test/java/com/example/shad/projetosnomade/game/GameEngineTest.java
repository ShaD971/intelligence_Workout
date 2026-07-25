package com.example.shad.projetosnomade.game;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameEngineTest {

    private static Level testLevel() {
        return new Level("test", Difficulty.EASY, new int[][]{
                {0, 1, 0},
                {1, 1, 1},
                {0, 1, 0}
        }, 3, 42L, 3, 0);
    }

    @Test
    public void rotateRowRightThenLeftRestoresGrid() {
        GameEngine engine = new GameEngine(testLevel());
        int[][] before = deepCopy(engine.getGrid());

        engine.rotateRow(0, 1);
        engine.rotateRow(0, -1);

        assertArrayEquals(before, engine.getGrid());
    }

    @Test
    public void rotateColumnDownThenUpRestoresGrid() {
        GameEngine engine = new GameEngine(testLevel());
        int[][] before = deepCopy(engine.getGrid());

        engine.rotateColumn(1, 1);
        engine.rotateColumn(1, -1);

        assertArrayEquals(before, engine.getGrid());
    }

    @Test
    public void rotationsPreserveValueCounts() {
        GameEngine engine = new GameEngine(testLevel());
        int before = countRed(engine.getGrid());

        engine.rotateRow(1, 1);
        engine.rotateColumn(2, -1);

        assertEquals(before, countRed(engine.getGrid()));
    }

    @Test
    public void undoRestoresExactPreviousState() {
        GameEngine engine = new GameEngine(testLevel());
        int[][] before = deepCopy(engine.getGrid());
        int scoreBefore = engine.getScore();

        engine.rotateRow(0, 1);
        engine.undo();

        assertArrayEquals(before, engine.getGrid());
        assertEquals(scoreBefore, engine.getScore());
        assertFalse(engine.canUndo());
    }

    @Test
    public void reachingTargetSetsWonAndAwardsBonus() {
        Level level = testLevel();
        GameEngine engine = new GameEngine(level);

        // Drive the engine to the target by replaying the inverse of the scramble.
        GridScrambler.ScrambleResult result = GridScrambler.scrambleWithHistory(level);
        for (int i = result.moves.size() - 1; i >= 0; i--) {
            GridScrambler.Move move = result.moves.get(i);
            if (move.axis == GridScrambler.Axis.ROW) {
                engine.rotateRow(move.index, -move.direction);
            } else {
                engine.rotateColumn(move.index, -move.direction);
            }
        }

        assertTrue(engine.isWon());
        assertTrue(engine.getScore() > 0);
    }

    @Test
    public void computeStarsMatchesParMovesThresholds() {
        Level level = new Level("stars", Difficulty.EASY, new int[][]{
                {0, 1},
                {1, 0}
        }, 1, 7L, 2, 0);
        GameEngine engine = new GameEngine(level);

        assertEquals(0, engine.computeStars());
    }

    private static int[][] deepCopy(int[][] source) {
        int[][] result = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].clone();
        }
        return result;
    }

    private static int countRed(int[][] grid) {
        int count = 0;
        for (int[] row : grid) {
            for (int value : row) {
                if (value == 1) {
                    count++;
                }
            }
        }
        return count;
    }
}
