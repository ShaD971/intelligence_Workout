package com.example.shad.projetosnomade.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.shad.projetosnomade.R;

/**
 * Draws a small static preview of a level's target grid (used on level-select cards).
 */
public class LevelPreviewView extends View {

    private static final int RED = 1;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tileRect = new RectF();
    private final int blueColor;
    private final int redColor;
    private int[][] target = new int[0][0];

    public LevelPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        blueColor = ContextCompat.getColor(context, R.color.iw_tile_blue_start);
        redColor = ContextCompat.getColor(context, R.color.iw_tile_red_start);
    }

    public void setTarget(int[][] target) {
        this.target = target;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int gridSize = target.length;
        if (gridSize == 0) {
            return;
        }
        float size = Math.min(getWidth(), getHeight()) / (float) gridSize;
        float gap = size * 0.08f;
        float radius = size * 0.15f;
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                float left = column * size;
                float top = row * size;
                tileRect.set(left + gap, top + gap, left + size - gap, top + size - gap);
                paint.setColor(target[row][column] == RED ? redColor : blueColor);
                canvas.drawRoundRect(tileRect, radius, radius, paint);
            }
        }
    }
}
