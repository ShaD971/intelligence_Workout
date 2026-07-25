package com.example.shad.projetosnomade.game;

final class GridRotations {

    private GridRotations() {
    }

    static void rotateRowRight(int[][] grid, int row) {
        int size = grid[row].length;
        int last = grid[row][size - 1];
        for (int column = size - 1; column > 0; column--) {
            grid[row][column] = grid[row][column - 1];
        }
        grid[row][0] = last;
    }

    static void rotateRowLeft(int[][] grid, int row) {
        int size = grid[row].length;
        int first = grid[row][0];
        for (int column = 0; column < size - 1; column++) {
            grid[row][column] = grid[row][column + 1];
        }
        grid[row][size - 1] = first;
    }

    static void rotateColumnDown(int[][] grid, int column) {
        int size = grid.length;
        int last = grid[size - 1][column];
        for (int row = size - 1; row > 0; row--) {
            grid[row][column] = grid[row - 1][column];
        }
        grid[0][column] = last;
    }

    static void rotateColumnUp(int[][] grid, int column) {
        int size = grid.length;
        int first = grid[0][column];
        for (int row = 0; row < size - 1; row++) {
            grid[row][column] = grid[row + 1][column];
        }
        grid[size - 1][column] = first;
    }
}
