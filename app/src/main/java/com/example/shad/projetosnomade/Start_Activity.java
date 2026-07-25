package com.example.shad.projetosnomade;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import com.example.shad.projetosnomade.databinding.ActivityMainBinding;
import com.example.shad.projetosnomade.game.Difficulty;
import com.example.shad.projetosnomade.game.Level;
import com.example.shad.projetosnomade.game.LevelRepository;
import com.example.shad.projetosnomade.progress.ProgressStore;
import com.example.shad.projetosnomade.view.StarRowView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Created by shad on 08/01/16.
 */
public class Start_Activity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ProgressStore progressStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressStore = new ProgressStore(this);

        binding.easyCard.setOnClickListener(v -> openLevelSelect(Difficulty.EASY));
        binding.mediumCard.setOnClickListener(v -> openLevelSelect(Difficulty.MEDIUM));
        binding.hardCard.setOnClickListener(v -> openLevelSelect(Difficulty.HARD));
        binding.howToPlayButton.setOnClickListener(v -> showHowToPlay());
        binding.resetProgressButton.setOnClickListener(v -> confirmResetProgress());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProgress();
    }

    private void openLevelSelect(Difficulty difficulty) {
        Intent intent = new Intent(this, LevelSelectActivity.class);
        intent.putExtra(LevelSelectActivity.EXTRA_DIFFICULTY, difficulty.id);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                this, android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent, options.toBundle());
    }

    private void showHowToPlay() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_how_to_play);
        dialog.show();
    }

    private void confirmResetProgress() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_progress_confirm_title)
                .setMessage(R.string.reset_progress_confirm_message)
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> {
                    progressStore.resetProgress();
                    refreshProgress();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void refreshProgress() {
        binding.totalStarsText.setText(getString(R.string.stars_total_format, progressStore.getTotalStars(), 27));

        bindTier(Difficulty.EASY, binding.easyTierProgress,
                new FrameLayout[]{binding.easyPip1, binding.easyPip2, binding.easyPip3});
        bindTier(Difficulty.MEDIUM, binding.mediumTierProgress,
                new FrameLayout[]{binding.mediumPip1, binding.mediumPip2, binding.mediumPip3});
        bindTier(Difficulty.HARD, binding.hardTierProgress,
                new FrameLayout[]{binding.hardPip1, binding.hardPip2, binding.hardPip3});
    }

    private void bindTier(Difficulty difficulty, TextView progressText, FrameLayout[] pips) {
        List<Level> levels = LevelRepository.forDifficulty(difficulty);
        int tierStars = 0;
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            tierStars += progressStore.getStars(level.id);
            bindPip(pips[i], level);
        }
        progressText.setText(getString(R.string.tier_progress_format, tierStars));
    }

    private void bindPip(FrameLayout container, Level level) {
        container.removeAllViews();

        if (progressStore.isUnlocked(level.id)) {
            StarRowView starRow = new StarRowView(this, null);
            starRow.setStars(progressStore.getStars(level.id));
            container.addView(starRow, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        } else {
            ImageView lock = new ImageView(this);
            lock.setImageResource(R.drawable.ic_lock);
            lock.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.iw_locked)));
            lock.setContentDescription(getString(R.string.content_description_lock));
            int size = dp(20);
            container.addView(lock, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
