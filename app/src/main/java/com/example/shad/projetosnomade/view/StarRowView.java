package com.example.shad.projetosnomade.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.example.shad.projetosnomade.R;

/**
 * Row of 3 star icons (filled up to the earned count, outline beyond it). Reused on the
 * home screen tier pips, the level-select cards, and the victory sheet.
 */
public class StarRowView extends LinearLayout {

    private static final int STAR_COUNT = 3;

    private final ImageView[] stars = new ImageView[STAR_COUNT];

    public StarRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);

        float density = getResources().getDisplayMetrics().density;
        int size = Math.round(13 * density);
        int spacing = Math.round(1 * density);

        for (int i = 0; i < STAR_COUNT; i++) {
            ImageView star = new ImageView(context);
            LayoutParams params = new LayoutParams(size, size);
            if (i > 0) {
                params.leftMargin = spacing;
            }
            addView(star, params);
            stars[i] = star;
        }
        setStars(0);
    }

    public void setStars(int count) {
        int gold = ContextCompat.getColor(getContext(), R.color.iw_gold);
        int muted = ContextCompat.getColor(getContext(), R.color.iw_on_surface_muted);
        for (int i = 0; i < STAR_COUNT; i++) {
            boolean filled = i < count;
            stars[i].setImageResource(filled ? R.drawable.ic_star : R.drawable.ic_star_outline);
            stars[i].setImageTintList(ColorStateList.valueOf(filled ? gold : muted));
        }
    }
}
