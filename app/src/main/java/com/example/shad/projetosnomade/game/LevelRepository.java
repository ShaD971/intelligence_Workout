package com.example.shad.projetosnomade.game;

import com.example.shad.projetosnomade.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class LevelRepository {

    private static final int BLUE = 0;
    private static final int RED = 1;

    private static final List<Level> LEVELS = Collections.unmodifiableList(Arrays.asList(
            new Level("easy_1", Difficulty.EASY, new int[][]{
                    {BLUE, RED, BLUE},
                    {RED, RED, RED},
                    {BLUE, RED, BLUE}
            }, 3, 1001L, 3, R.string.level_name_easy_1),

            new Level("easy_2", Difficulty.EASY, new int[][]{
                    {RED, BLUE, BLUE},
                    {BLUE, RED, BLUE},
                    {BLUE, BLUE, RED}
            }, 4, 1002L, 4, R.string.level_name_easy_2),

            new Level("easy_3", Difficulty.EASY, new int[][]{
                    {RED, BLUE, RED},
                    {BLUE, RED, BLUE},
                    {RED, BLUE, RED}
            }, 5, 1003L, 5, R.string.level_name_easy_3),

            new Level("medium_1", Difficulty.MEDIUM, new int[][]{
                    {RED, RED, RED, RED},
                    {RED, BLUE, BLUE, RED},
                    {RED, BLUE, BLUE, RED},
                    {RED, RED, RED, RED}
            }, 5, 2001L, 5, R.string.level_name_medium_1),

            new Level("medium_2", Difficulty.MEDIUM, new int[][]{
                    {RED, RED, RED, RED, RED},
                    {BLUE, RED, RED, RED, BLUE},
                    {BLUE, BLUE, RED, BLUE, BLUE},
                    {BLUE, RED, RED, RED, BLUE},
                    {RED, RED, RED, RED, RED}
            }, 6, 2002L, 6, R.string.level_name_medium_2),

            new Level("medium_3", Difficulty.MEDIUM, new int[][]{
                    {BLUE, BLUE, RED, BLUE, BLUE},
                    {BLUE, BLUE, RED, BLUE, BLUE},
                    {RED, RED, RED, RED, RED},
                    {BLUE, BLUE, RED, BLUE, BLUE},
                    {BLUE, BLUE, RED, BLUE, BLUE}
            }, 7, 2003L, 7, R.string.level_name_medium_3),

            new Level("hard_1", Difficulty.HARD, new int[][]{
                    {RED, BLUE, BLUE, BLUE, BLUE, RED},
                    {BLUE, RED, BLUE, BLUE, RED, BLUE},
                    {BLUE, BLUE, RED, RED, BLUE, BLUE},
                    {BLUE, BLUE, RED, RED, BLUE, BLUE},
                    {BLUE, RED, BLUE, BLUE, RED, BLUE},
                    {RED, BLUE, BLUE, BLUE, BLUE, RED}
            }, 8, 3001L, 8, R.string.level_name_hard_1),

            new Level("hard_2", Difficulty.HARD, new int[][]{
                    {RED, RED, RED, RED, RED, RED},
                    {RED, BLUE, BLUE, BLUE, BLUE, BLUE},
                    {RED, BLUE, RED, RED, RED, BLUE},
                    {RED, BLUE, RED, BLUE, BLUE, BLUE},
                    {RED, BLUE, RED, RED, RED, RED},
                    {RED, BLUE, BLUE, BLUE, BLUE, BLUE}
            }, 10, 3002L, 10, R.string.level_name_hard_2),

            new Level("hard_3", Difficulty.HARD, new int[][]{
                    {RED, RED, BLUE, BLUE, RED, RED},
                    {RED, RED, BLUE, BLUE, RED, RED},
                    {BLUE, BLUE, RED, RED, BLUE, BLUE},
                    {BLUE, BLUE, RED, RED, BLUE, BLUE},
                    {RED, RED, BLUE, BLUE, RED, RED},
                    {RED, RED, BLUE, BLUE, RED, RED}
            }, 12, 3003L, 12, R.string.level_name_hard_3)
    ));

    private LevelRepository() {
    }

    public static List<Level> all() {
        return LEVELS;
    }

    public static List<Level> forDifficulty(Difficulty difficulty) {
        List<Level> result = new ArrayList<>();
        for (Level level : LEVELS) {
            if (level.difficulty == difficulty) {
                result.add(level);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Level byId(String id) {
        for (Level level : LEVELS) {
            if (level.id.equals(id)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown level id: " + id);
    }
}
