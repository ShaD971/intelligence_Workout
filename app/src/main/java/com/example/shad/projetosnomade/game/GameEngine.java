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

        public Move(Axis axis, int index, int direction) {
            this.axis = axis;
            this.index = index;
            this.direction = direction;
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

    public void rotateRow(int row, int direction) {
        applyMove(new Move(Axis.ROW, row, direction));
    }

    public void rotateColumn(int column, int direction) {
        applyMove(new Move(Axis.COLUMN, column, direction));
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }
        HistoryEntry entry = history.pop();
        rotate(new Move(entry.move.axis, entry.move.index, -entry.move.direction));
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

    private void applyMove(Move move) {
        if (won) {
            return;
        }
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
