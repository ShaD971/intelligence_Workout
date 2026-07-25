package com.example.shad.projetosnomade.game;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Owns all mutable game state (grid, score, moves, undo history). No Android dependency,
 * so it is directly testable on the JVM.
 */
public final class GameEngine {

    public enum Axis { ROW, COLUMN }

    public static final class Move {
        public final Axis axis;
        public final int index;
        public final int direction;
        public final int stepCount;

        public Move(Axis axis, int index, int direction, int stepCount) {
            this.axis = axis;
            this.index = index;
            this.direction = direction;
            this.stepCount = stepCount;
        }
    }

    private static final class HistoryEntry {
        final Move move;
        final int scoreBefore;
        final int movesBefore;

        HistoryEntry(Move move, int scoreBefore, int movesBefore) {
            this.move = move;
            this.scoreBefore = scoreBefore;
            this.movesBefore = movesBefore;
        }
    }

    private final Level level;
    private int[][] grid;
    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private int moves;
    private int score;
    private long elapsedMillis;
    private boolean won;

    public GameEngine(Level level) {
        this(level, GridScrambler.scramble(level), 0, level.difficulty.startScore, 0L, false);
    }

    private GameEngine(Level level, int[][] grid, int moves, int score, long elapsedMillis, boolean won) {
        this.level = level;
        this.grid = grid;
        this.moves = moves;
        this.score = score;
        this.elapsedMillis = elapsedMillis;
        this.won = won;
    }

    /**
     * Restores an in-progress game (e.g. across a screen rotation). Undo history is not
     * preserved, since only the grid/score/moves/time are persisted across the restart.
     */
    public static GameEngine restore(Level level, int[][] grid, int moves, int score,
                                      long elapsedMillis, boolean won) {
        return new GameEngine(level, grid, moves, score, elapsedMillis, won);
    }

    public Level getLevel() {
        return level;
    }

    public int[][] getGrid() {
        return grid;
    }

    /** Defensive copy for callers that read the grid from a different thread than the one mutating it. */
    public int[][] copyGrid() {
        int[][] copy = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            copy[row] = grid[row].clone();
        }
        return copy;
    }

    public int[][] getTarget() {
        return level.target;
    }

    public int getGridSize() {
        return level.gridSize;
    }

    public int getMoves() {
        return moves;
    }

    public int getScore() {
        return score;
    }

    public int getElapsedSeconds() {
        return (int) (elapsedMillis / 1000);
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public boolean isWon() {
        return won;
    }

    public void tick(long deltaMillis) {
        if (!won) {
            elapsedMillis += deltaMillis;
        }
    }

    /** Rotates a row by the given number of single-cell steps (sign = direction) as one move. */
    public void rotateRow(int row, int steps) {
        applyMove(Axis.ROW, row, steps);
    }

    /** Rotates a column by the given number of single-cell steps (sign = direction) as one move. */
    public void rotateColumn(int column, int steps) {
        applyMove(Axis.COLUMN, column, steps);
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }
        HistoryEntry entry = history.pop();
        Move move = entry.move;
        rotate(new Move(move.axis, move.index, -move.direction, move.stepCount));
        score = entry.scoreBefore;
        moves = entry.movesBefore;
        won = false;
    }

    public void reset() {
        grid = GridScrambler.scramble(level);
        history.clear();
        moves = 0;
        score = level.difficulty.startScore;
        elapsedMillis = 0;
        won = false;
    }

    public int computeStars() {
        if (!won) {
            return 0;
        }
        if (moves <= level.parMoves) {
            return 3;
        }
        if (moves <= level.parMoves * 1.5) {
            return 2;
        }
        return 1;
    }

    private void applyMove(Axis axis, int index, int steps) {
        if (won || steps == 0) {
            return;
        }
        int direction = steps > 0 ? 1 : -1;
        Move move = new Move(axis, index, direction, Math.abs(steps));

        int scoreBefore = score;
        int movesBefore = moves;
        rotate(move);
        moves++;
        score = Math.max(0, score - level.difficulty.movePenalty);
        checkForWin();
        history.push(new HistoryEntry(move, scoreBefore, movesBefore));
    }

    private void checkForWin() {
        if (won || !matchesTarget()) {
            return;
        }
        int fastFinishBonus = Math.max(0, (level.parMoves - moves + 1) * level.difficulty.quickMoveBonus);
        score += level.difficulty.finishBonus + fastFinishBonus;
        won = true;
    }

    private boolean matchesTarget() {
        int[][] target = level.target;
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length; column++) {
                if (grid[row][column] != target[row][column]) {
                    return false;
                }
            }
        }
        return true;
    }

    private void rotate(Move move) {
        for (int i = 0; i < move.stepCount; i++) {
            if (move.axis == Axis.ROW) {
                if (move.direction > 0) {
                    GridRotations.rotateRowRight(grid, move.index);
                } else {
                    GridRotations.rotateRowLeft(grid, move.index);
                }
            } else {
                if (move.direction > 0) {
                    GridRotations.rotateColumnDown(grid, move.index);
                } else {
                    GridRotations.rotateColumnUp(grid, move.index);
                }
            }
        }
    }
}
