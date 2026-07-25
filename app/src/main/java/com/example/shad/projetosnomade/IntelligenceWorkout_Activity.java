package com.example.shad.projetosnomade;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shad.projetosnomade.databinding.MainBinding;
import com.example.shad.projetosnomade.progress.ProgressStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Created by shad on 18/12/15.
 */
public class IntelligenceWorkout_Activity extends AppCompatActivity
        implements IntelligenceWorkoutView.GameStateListener {

    public static final String EXTRA_LEVEL_ID = "com.example.shad.projetosnomade.LEVEL_ID";

    private static final String STATE_LEVEL_ID = "levelId";
    private static final String STATE_GRID = "grid";
    private static final String STATE_MOVES = "moves";
    private static final String STATE_SCORE = "score";
    private static final String STATE_ELAPSED_MILLIS = "elapsedMillis";
    private static final String STATE_WON = "won";

    private MainBinding binding;
    private ProgressStore progressStore;
    private boolean victoryDialogShown;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            binding.gameTime.setText(getString(R.string.game_time_format, binding.view.getElapsedSeconds()));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progressStore = new ProgressStore(this);

        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.view.setGameStateListener(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_LEVEL_ID)) {
            binding.view.restoreState(
                    savedInstanceState.getString(STATE_LEVEL_ID),
                    savedInstanceState.getIntArray(STATE_GRID),
                    savedInstanceState.getInt(STATE_MOVES),
                    savedInstanceState.getInt(STATE_SCORE),
                    savedInstanceState.getLong(STATE_ELAPSED_MILLIS),
                    savedInstanceState.getBoolean(STATE_WON));
            victoryDialogShown = savedInstanceState.getBoolean(STATE_WON);
        } else {
            binding.view.setLevel(getIntent().getStringExtra(EXTRA_LEVEL_ID));
        }
        binding.view.setVisibility(View.VISIBLE);

        binding.resetButton.setOnClickListener(v -> {
            victoryDialogShown = false;
            binding.view.resetGame();
        });

        binding.homeButton.setOnClickListener(v -> finish());

        timerHandler.post(timerTick);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_LEVEL_ID, binding.view.getLevelId());
        outState.putIntArray(STATE_GRID, binding.view.captureFlatGrid());
        outState.putInt(STATE_MOVES, binding.view.getMoves());
        outState.putInt(STATE_SCORE, binding.view.getScore());
        outState.putLong(STATE_ELAPSED_MILLIS, binding.view.getElapsedMillis());
        outState.putBoolean(STATE_WON, binding.view.isGameWon());
    }

    @Override
    public void onGameStateChanged(String difficultyLabel, int score, int moves, int elapsedSeconds, boolean gameWon) {
        binding.gameDifficulty.setText(getString(R.string.game_level_format, difficultyLabel));
        binding.gameScore.setText(getString(R.string.game_score_format, score));
        binding.gameTime.setText(getString(R.string.game_time_format, elapsedSeconds));
        if (gameWon) {
            binding.gameStatus.setText(getString(R.string.game_status_won, moves, score));
            showVictoryDialog(difficultyLabel, score, moves, elapsedSeconds);
        } else {
            victoryDialogShown = false;
            binding.gameStatus.setText(getString(R.string.game_status_playing, moves));
        }
    }

    private void showVictoryDialog(String difficultyLabel, int score, int moves, int elapsedSeconds) {
        if (victoryDialogShown || isFinishing()) {
            return;
        }
        victoryDialogShown = true;

        progressStore.recordResult(binding.view.getLevelId(), binding.view.getStars(), score,
                binding.view.getElapsedMillis(), moves);

        String message = getString(R.string.victory_message, difficultyLabel, elapsedSeconds, moves, score);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.victory_title)
                .setMessage(message)
                .setPositiveButton(R.string.action_replay, (dialog, which) -> {
                    victoryDialogShown = false;
                    binding.view.resetGame();
                })
                .setNegativeButton(R.string.action_back_to_home, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacks(timerTick);
        super.onDestroy();
    }
}
