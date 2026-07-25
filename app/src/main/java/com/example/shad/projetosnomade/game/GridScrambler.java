package com.example.shad.projetosnomade.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates a level's starting grid by applying N deterministic random rotations to its
 * target, so every generated grid is solvable by construction (undo the same moves in reverse).
 */
public final class GridScrambler {

    enum Axis { ROW, COLUMN }

    static final class Move {
        final Axis axis;
        final int index;
        final int direction;

        Move(Axis axis, int index, int direction) {
            this.axis = axis;
            this.index = index;
            this.direction = direction;
        }

        boolean reverses(Move other) {
            return axis == other.axis && index == other.index && direction == -other.direction;
        }

        Move inverse() {
            return new Move(axis, index, -direction);
        }
    }

    static final class ScrambleResult {
        final int[][] grid;
        final List<Move> moves;

        ScrambleResult(int[][] grid, List<Move> moves) {
            this.grid = grid;
            this.moves = moves;
        }
    }

    private GridScrambler() {
    }

    public static int[][] scramble(Level level) {
        return scrambleWithHistory(level).grid;
    }

    public static boolean verifySolvable(Level level) {
        ScrambleResult result = scrambleWithHistory(level);
        int[][] grid = copy(result.grid);
        for (int i = result.moves.size() - 1; i >= 0; i--) {
            apply(grid, result.moves.get(i).inverse());
        }
        return gridsEqual(grid, level.target);
    }

    static ScrambleResult scrambleWithHistory(Level level) {
        long seed = level.seed;
        while (true) {
            List<Move> moves = new ArrayList<>();
            int[][] grid = copy(level.target);
            Random random = new Random(seed);
            Move previous = null;

            for (int i = 0; i < level.scrambleMoves; i++) {
                Move move;
                do {
                    Axis axis = random.nextBoolean() ? Axis.ROW : Axis.COLUMN;
                    int index = random.nextInt(level.gridSize);
                    int direction = random.nextBoolean() ? 1 : -1;
                    move = new Move(axis, index, direction);
                } while (previous != null && move.reverses(previous));

                apply(grid, move);
                moves.add(move);
                previous = move;
            }

            if (!gridsEqual(grid, level.target)) {
                return new ScrambleResult(grid, Collections.unmodifiableList(moves));
            }
            seed++;
        }
    }

    private static void apply(int[][] grid, Move move) {
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

    private static int[][] copy(int[][] source) {
        int[][] result = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            result[row] = source[row].clone();
        }
        return result;
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
