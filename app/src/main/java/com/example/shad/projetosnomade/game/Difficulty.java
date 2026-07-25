package com.example.shad.projetosnomade.game;

import com.example.shad.projetosnomade.R;

public enum Difficulty {
    EASY("easy", R.string.difficulty_easy, 3, 120, 4, 60, 15),
    MEDIUM("medium", R.string.difficulty_medium, 5, 220, 8, 140, 30),
    HARD("hard", R.string.difficulty_hard, 6, 360, 12, 280, 50);

    public final String id;
    public final int labelRes;
    public final int defaultGridSize;
    public final int startScore;
    public final int movePenalty;
    public final int finishBonus;
    public final int quickMoveBonus;

    Difficulty(String id, int labelRes, int defaultGridSize, int startScore,
               int movePenalty, int finishBonus, int quickMoveBonus) {
        this.id = id;
        this.labelRes = labelRes;
        this.defaultGridSize = defaultGridSize;
        this.startScore = startScore;
        this.movePenalty = movePenalty;
        this.finishBonus = finishBonus;
        this.quickMoveBonus = quickMoveBonus;
    }

    public static Difficulty fromId(String id) {
        for (Difficulty difficulty : values()) {
            if (difficulty.id.equals(id)) {
                return difficulty;
            }
        }
        return MEDIUM;
    }
}
