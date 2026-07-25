package com.example.shad.projetosnomade.game;

public final class Level {

    public final String id;
    public final Difficulty difficulty;
    public final int gridSize;
    public final int[][] target;
    public final int scrambleMoves;
    public final long seed;
    public final int parMoves;
    public final int labelRes;

    public Level(String id, Difficulty difficulty, int[][] target, int scrambleMoves,
                 long seed, int parMoves, int labelRes) {
        this.id = id;
        this.difficulty = difficulty;
        this.target = target;
        this.gridSize = target.length;
        this.scrambleMoves = scrambleMoves;
        this.seed = seed;
        this.parMoves = parMoves;
        this.labelRes = labelRes;
    }
}
