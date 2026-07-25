package com.example.shad.projetosnomade;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shad.projetosnomade.databinding.ActivityLevelSelectBinding;
import com.example.shad.projetosnomade.game.Difficulty;
import com.example.shad.projetosnomade.game.LevelRepository;
import com.example.shad.projetosnomade.progress.ProgressStore;

public class LevelSelectActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "com.example.shad.projetosnomade.DIFFICULTY";

    private ActivityLevelSelectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLevelSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Difficulty difficulty = Difficulty.fromId(getIntent().getStringExtra(EXTRA_DIFFICULTY));
        binding.toolbar.setTitle(getString(R.string.level_select_title_format, getString(difficulty.labelRes)));
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ProgressStore progressStore = new ProgressStore(this);
        binding.levelList.setLayoutManager(new GridLayoutManager(this, 2));
        binding.levelList.setAdapter(new LevelAdapter(LevelRepository.forDifficulty(difficulty), progressStore,
                level -> {
                    Intent intent = new Intent(this, IntelligenceWorkout_Activity.class);
                    intent.putExtra(IntelligenceWorkout_Activity.EXTRA_LEVEL_ID, level.id);
                    startActivity(intent);
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        RecyclerView.Adapter<?> adapter = binding.levelList.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
