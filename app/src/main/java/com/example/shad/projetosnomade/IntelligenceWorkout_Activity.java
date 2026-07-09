package com.example.shad.projetosnomade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

/**
 * Created by shad on 18/12/15.
 */
public class IntelligenceWorkout_Activity extends Activity
        implements IntelligenceWorkoutView.GameStateListener {

    public static final String EXTRA_DIFFICULTY = "com.example.shad.projetosnomade.DIFFICULTY";

    private IntelligenceWorkoutView intelligenceWorkoutView;
    private TextView difficultyText;
    private TextView scoreText;
    private TextView timeText;
    private TextView statusText;
    private boolean victoryDialogShown;
    private final Handler timerHandler = new Handler();
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            if (intelligenceWorkoutView != null) {
                timeText.setText("Temps : " + intelligenceWorkoutView.getElapsedSeconds() + "s");
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        difficultyText = (TextView) findViewById(R.id.gameDifficulty);
        scoreText = (TextView) findViewById(R.id.gameScore);
        timeText = (TextView) findViewById(R.id.gameTime);
        statusText = (TextView) findViewById(R.id.gameStatus);

        intelligenceWorkoutView = (IntelligenceWorkoutView) findViewById(R.id.view);
        intelligenceWorkoutView.setGameStateListener(this);
        intelligenceWorkoutView.setDifficulty(getIntent().getStringExtra(EXTRA_DIFFICULTY));
        intelligenceWorkoutView.setVisibility(View.VISIBLE);

        findViewById(R.id.resetButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                victoryDialogShown = false;
                intelligenceWorkoutView.resetGame();
            }
        });

        findViewById(R.id.homeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        timerHandler.post(timerTick);
    }

    @Override
    public void onGameStateChanged(String difficultyLabel, int score, int moves, int elapsedSeconds, boolean gameWon) {
        difficultyText.setText("Niveau : " + difficultyLabel);
        scoreText.setText("Score : " + score);
        timeText.setText("Temps : " + elapsedSeconds + "s");
        if (gameWon) {
            statusText.setText("Reussi en " + moves + " mouvements. Score final : " + score);
            showVictoryDialog(difficultyLabel, score, moves, elapsedSeconds);
        } else {
            victoryDialogShown = false;
            statusText.setText("Mouvements : " + moves + " - reproduisez la cible");
        }
    }

    private void showVictoryDialog(String difficultyLabel, int score, int moves, int elapsedSeconds) {
        if (victoryDialogShown || isFinishing()) {
            return;
        }
        victoryDialogShown = true;

        String message = "Bravo, vous avez reussi le niveau !\n\n"
                + "Niveau : " + difficultyLabel + "\n"
                + "Temps : " + elapsedSeconds + "s\n"
                + "Mouvements : " + moves + "\n"
                + "Score final : " + score;

        new AlertDialog.Builder(this)
                .setTitle("Bravo !")
                .setMessage(message)
                .setPositiveButton("Rejouer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        victoryDialogShown = false;
                        intelligenceWorkoutView.resetGame();
                    }
                })
                .setNegativeButton("Retour accueil", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacks(timerTick);
        super.onDestroy();
    }
}
