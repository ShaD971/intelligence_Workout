package com.example.shad.projetosnomade;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

/**
 * Created by shad on 08/01/16.
 */
public class Start_Activity extends Activity implements View.OnClickListener {

    private Button easyButton;
    private Button mediumButton;
    private Button hardButton;
    private String selectedDifficulty = IntelligenceWorkoutView.DIFFICULTY_MEDIUM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        easyButton = (Button) findViewById(R.id.difficultyEasy);
        mediumButton = (Button) findViewById(R.id.difficultyMedium);
        hardButton = (Button) findViewById(R.id.difficultyHard);

        easyButton.setOnClickListener(this);
        mediumButton.setOnClickListener(this);
        hardButton.setOnClickListener(this);
        findViewById(R.id.button28).setOnClickListener(this);
        updateDifficultyButtons();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.difficultyEasy) {
            selectedDifficulty = IntelligenceWorkoutView.DIFFICULTY_EASY;
            updateDifficultyButtons();
            return;
        }
        if (id == R.id.difficultyMedium) {
            selectedDifficulty = IntelligenceWorkoutView.DIFFICULTY_MEDIUM;
            updateDifficultyButtons();
            return;
        }
        if (id == R.id.difficultyHard) {
            selectedDifficulty = IntelligenceWorkoutView.DIFFICULTY_HARD;
            updateDifficultyButtons();
            return;
        }
        if (id == R.id.button28) {
            Intent i = new Intent(this, IntelligenceWorkout_Activity.class);
            i.putExtra(IntelligenceWorkout_Activity.EXTRA_DIFFICULTY, selectedDifficulty);
            startActivity(i);
        }
    }

    private void updateDifficultyButtons() {
        easyButton.setBackgroundResource(IntelligenceWorkoutView.DIFFICULTY_EASY.equals(selectedDifficulty)
                ? R.drawable.button_selected : R.drawable.button_secondary);
        mediumButton.setBackgroundResource(IntelligenceWorkoutView.DIFFICULTY_MEDIUM.equals(selectedDifficulty)
                ? R.drawable.button_selected : R.drawable.button_secondary);
        hardButton.setBackgroundResource(IntelligenceWorkoutView.DIFFICULTY_HARD.equals(selectedDifficulty)
                ? R.drawable.button_selected : R.drawable.button_secondary);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
