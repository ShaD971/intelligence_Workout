package com.example.shad.projetosnomade;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * Created by shad on 18/12/15.
 */
public class IntelligenceWorkout_Activity extends Activity {

    private IntelligenceWorkoutView intelligenceWorkoutView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // initialise notre activity avec le constructeur parent
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        // recuperation de la vue une voie cree � partir de son id
        intelligenceWorkoutView = (IntelligenceWorkoutView) findViewById(R.id.view);
        // rend visible la vue
        intelligenceWorkoutView.setVisibility(View.VISIBLE);
    }
}
