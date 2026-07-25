package com.example.shad.projetosnomade;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shad.projetosnomade.databinding.MainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Created by shad on 18/12/15.
 */
public class IntelligenceWorkout_Activity extends AppCompatActivity
        implements IntelligenceWorkoutView.GameStateListener {

    public static final String EXTRA_DIFFICULTY = "com.example.shad.projetosnomade.DIFFICULTY";

    private MainBinding binding;
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

        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.view.setGameStateListener(this);
        binding.view.setDifficulty(getIntent().getStringExtra(EXTRA_DIFFICULTY));
        binding.view.setVisibility(View.VISIBLE);

        binding.resetButton.setOnClickListener(v -> {
            victoryDialogShown = false;
            binding.view.resetGame();
        });

        binding.homeButton.setOnClickListener(v -> finish());

        timerHandler.post(timerTick);
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
