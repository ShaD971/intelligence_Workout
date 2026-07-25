package com.example.shad.projetosnomade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shad.projetosnomade.game.Level;
import com.example.shad.projetosnomade.progress.ProgressStore;
import com.example.shad.projetosnomade.view.LevelPreviewView;
import com.example.shad.projetosnomade.view.StarRowView;

import java.util.List;

final class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.ViewHolder> {

    interface OnLevelClickListener {
        void onLevelClick(Level level);
    }

    private final List<Level> levels;
    private final ProgressStore progressStore;
    private final OnLevelClickListener listener;

    LevelAdapter(List<Level> levels, ProgressStore progressStore, OnLevelClickListener listener) {
        this.levels = levels;
        this.progressStore = progressStore;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_level_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Level level = levels.get(position);
        boolean unlocked = progressStore.isUnlocked(level.id);
        int stars = progressStore.getStars(level.id);
        int bestScore = progressStore.getBestScore(level.id);

        holder.levelName.setText(holder.itemView.getContext().getString(R.string.level_card_title_format,
                position + 1, holder.itemView.getContext().getString(level.labelRes)));
        holder.levelPreview.setTarget(level.target);
        holder.levelStars.setStars(stars);
        if (bestScore > 0) {
            holder.levelBestScore.setText(holder.itemView.getContext().getString(R.string.best_score_format, bestScore));
            holder.levelBestScore.setVisibility(View.VISIBLE);
        } else {
            holder.levelBestScore.setVisibility(View.INVISIBLE);
        }
        holder.lockOverlay.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(unlocked ? v -> listener.onLevelClick(level) : null);
    }

    @Override
    public int getItemCount() {
        return levels.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView levelName;
        final LevelPreviewView levelPreview;
        final StarRowView levelStars;
        final TextView levelBestScore;
        final FrameLayout lockOverlay;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            levelName = itemView.findViewById(R.id.levelName);
            levelPreview = itemView.findViewById(R.id.levelPreview);
            levelStars = itemView.findViewById(R.id.levelStars);
            levelBestScore = itemView.findViewById(R.id.levelBestScore);
            lockOverlay = itemView.findViewById(R.id.lockOverlay);
        }
    }
}
