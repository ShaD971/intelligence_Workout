package com.example.shad.projetosnomade;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shad.projetosnomade.databinding.ActivityMainBinding;

/**
 * Created by shad on 08/01/16.
 */
public class Start_Activity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;
    private String selectedDifficulty = IntelligenceWorkoutView.DIFFICULTY_MEDIUM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.difficultyEasy.setOnClickListener(this);
        binding.difficultyMedium.setOnClickListener(this);
        binding.difficultyHard.setOnClickListener(this);
        binding.startButton.setOnClickListener(this);
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
        if (id == R.id.startButton) {
            Intent i = new Intent(this, IntelligenceWorkout_Activity.class);
            i.putExtra(IntelligenceWorkout_Activity.EXTRA_DIFFICULTY, selectedDifficulty);
            startActivity(i);
        }
    }

    private void updateDifficultyButtons() {
        binding.difficultyEasy.setBackgroundResource(IntelligenceWorkoutView.DIFFICULTY_EASY.equals(selectedDifficulty)
                ? R.drawable.button_selected : R.drawable.button_secondary);
        binding.difficultyMedium.setBackgroundResource(IntelligenceWorkoutView.DIFFICULTY_MEDIUM.equals(selectedDifficulty)
                ? R.drawable.button_selected : R.drawable.button_secondary);
        binding.difficultyHard.setBackgroundResource(IntelligenceWorkoutView.DIFFICULTY_HARD.equals(selectedDifficulty)
                ? R.drawable.button_selected : R.drawable.button_secondary);
    }
}
