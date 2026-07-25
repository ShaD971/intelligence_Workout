package com.example.shad.projetosnomade;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shad.projetosnomade.databinding.MainBinding;
import com.example.shad.projetosnomade.game.Level;
import com.example.shad.projetosnomade.game.LevelRepository;
import com.example.shad.projetosnomade.progress.ProgressStore;
import com.example.shad.projetosnomade.view.StarRowView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.List;

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
            binding.gameTimeValue.setText(getString(R.string.game_time_value_format, binding.view.getElapsedSeconds()));
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
        binding.targetPreview.setTarget(binding.view.getTarget());

        binding.undoButton.setOnClickListener(v -> binding.view.undo());
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
        binding.gameMovesValue.setText(String.valueOf(moves));
        binding.gameScoreValue.setText(String.valueOf(score));
        binding.gameTimeValue.setText(getString(R.string.game_time_value_format, elapsedSeconds));
        binding.undoButton.setEnabled(binding.view.canUndo());
        if (gameWon) {
            binding.gameStatus.setText(getString(R.string.game_status_won, moves, score));
            showVictoryDialog(score, moves, elapsedSeconds);
        } else {
            victoryDialogShown = false;
            binding.gameStatus.setText(getString(R.string.game_status_playing, moves));
        }
    }

    private void showVictoryDialog(int score, int moves, int elapsedSeconds) {
        if (victoryDialogShown || isFinishing()) {
            return;
        }
        victoryDialogShown = true;

        String levelId = binding.view.getLevelId();
        Level level = LevelRepository.byId(levelId);
        int stars = binding.view.getStars();
        boolean isNewRecord = score > progressStore.getBestScore(levelId);

        progressStore.recordResult(levelId, stars, score, binding.view.getElapsedMillis(), moves);

        List<Level> tier = LevelRepository.forDifficulty(level.difficulty);
        int index = tier.indexOf(level);
        Level nextLevel = index >= 0 && index < tier.size() - 1 ? tier.get(index + 1) : null;

        View sheetView = getLayoutInflater().inflate(R.layout.dialog_victory, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheetView);
        dialog.setCancelable(false);

        StarRowView starsView = sheetView.findViewById(R.id.victoryStars);
        TextView levelNameView = sheetView.findViewById(R.id.victoryLevelName);
        TextView timeView = sheetView.findViewById(R.id.victoryTime);
        TextView movesView = sheetView.findViewById(R.id.victoryMoves);
        TextView scoreView = sheetView.findViewById(R.id.victoryScore);
        TextView newRecordView = sheetView.findViewById(R.id.victoryNewRecord);
        MaterialButton nextLevelButton = sheetView.findViewById(R.id.nextLevelButton);
        MaterialButton replayButton = sheetView.findViewById(R.id.replayButton);
        MaterialButton menuButton = sheetView.findViewById(R.id.menuButton);

        starsView.setStars(stars);
        levelNameView.setText(getString(level.labelRes));
        timeView.setText(getString(R.string.victory_time_format, elapsedSeconds));
        movesView.setText(getString(R.string.victory_moves_format, moves));
        scoreView.setText(getString(R.string.victory_score_format, score));
        newRecordView.setVisibility(isNewRecord ? View.VISIBLE : View.GONE);

        if (nextLevel != null) {
            Level target = nextLevel;
            nextLevelButton.setVisibility(View.VISIBLE);
            nextLevelButton.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, IntelligenceWorkout_Activity.class);
                intent.putExtra(EXTRA_LEVEL_ID, target.id);
                startActivity(intent);
                finish();
            });
        } else {
            nextLevelButton.setVisibility(View.GONE);
        }

        replayButton.setOnClickListener(v -> {
            dialog.dismiss();
            victoryDialogShown = false;
            binding.view.resetGame();
        });

        menuButton.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacks(timerTick);
        super.onDestroy();
    }
}
