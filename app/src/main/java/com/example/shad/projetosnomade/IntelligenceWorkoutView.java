package com.example.shad.projetosnomade;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

import androidx.core.content.ContextCompat;

import com.example.shad.projetosnomade.game.Difficulty;
import com.example.shad.projetosnomade.game.GameEngine;
import com.example.shad.projetosnomade.game.Level;
import com.example.shad.projetosnomade.game.LevelRepository;
import com.example.shad.projetosnomade.progress.ProgressStore;

/**
 * Created by shad on 18/12/15.
 */
public class IntelligenceWorkoutView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface GameStateListener {
        void onGameStateChanged(String difficultyLabel, int score, int moves, int elapsedSeconds, boolean gameWon);
    }

    public static final String DIFFICULTY_EASY = Difficulty.EASY.id;
    public static final String DIFFICULTY_MEDIUM = Difficulty.MEDIUM.id;
    public static final String DIFFICULTY_HARD = Difficulty.HARD.id;

    private static final int CST_ROUGE = 1;
    private static final long FRAME_BUDGET_NANOS = 1_000_000_000L / 60;
    private static final int SNAP_DURATION_MS = 180;
    private static final int HINT_BLINK_DURATION_MS = 900;
    private static final long VICTORY_STAGGER_MS = 25;
    private static final long VICTORY_POP_DURATION_MS = 220;
    private static final long VICTORY_FLASH_DURATION_MS = 150;

    private static final int AXIS_NONE = 0;
    private static final int AXIS_ROW = 1;
    private static final int AXIS_COLUMN = 2;

    /** Immutable snapshot of the in-progress drag, read by the render thread without a lock. */
    private static final class DragVisual {
        final int axis;
        final int index;
        final float offsetPx;

        DragVisual(int axis, int index, float offsetPx) {
            this.axis = axis;
            this.index = index;
            this.offsetPx = offsetPx;
        }
    }

    /** Immutable snapshot of the in-progress hint blink, read by the render thread without a lock. */
    private static final class HintVisual {
        final int axis;
        final int index;
        final float alpha;

        HintVisual(int axis, int index, float alpha) {
            this.axis = axis;
            this.index = index;
            this.alpha = alpha;
        }
    }

    private static final DragVisual NO_DRAG = new DragVisual(AXIS_NONE, -1, 0f);
    private static final HintVisual NO_HINT = new HintVisual(AXIS_NONE, -1, 0f);

    private final Object engineLock = new Object();
    private final int blueStart;
    private final int blueEnd;
    private final int redStart;
    private final int redEnd;
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flashPaint = new Paint();
    private final RectF tileRect = new RectF();
    private final RectF shadowRect = new RectF();
    private final int touchSlop;
    private final Vibrator vibrator;
    private final boolean hapticsEnabled;

    private GameEngine engine;
    private int tileSize;
    private float tileRadius;
    private float tileGap;
    private float shadowOffset;
    private LinearGradient blueGradient;
    private LinearGradient redGradient;
    private int carteTopAnchor;
    private int carteLeftAnchor;
    private GameStateListener gameStateListener;

    private boolean touchActive;
    private int pendingRow;
    private int pendingColumn;
    private int lastHapticStep;
    private float downX;
    private float downY;
    private volatile DragVisual dragVisual = NO_DRAG;
    private volatile HintVisual hintVisual = NO_HINT;
    private volatile long victoryAnimStartNanos;
    private ValueAnimator releaseAnimator;
    private ValueAnimator hintAnimator;

    private volatile boolean running;
    private Thread renderThread;
    private SurfaceHolder holder;

    public IntelligenceWorkoutView(Context context, AttributeSet attrs) {
        super(context, attrs);

        holder = getHolder();
        holder.addCallback(this);

        blueStart = ContextCompat.getColor(context, R.color.iw_tile_blue_start);
        blueEnd = ContextCompat.getColor(context, R.color.iw_tile_blue_end);
        redStart = ContextCompat.getColor(context, R.color.iw_tile_red_start);
        redEnd = ContextCompat.getColor(context, R.color.iw_tile_red_end);
        shadowPaint.setColor(0x33000000);
        hintPaint.setStyle(Paint.Style.STROKE);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        vibrator = createVibrator(context);
        hapticsEnabled = new ProgressStore(context).isHapticsEnabled();

        setFocusable(true);
        setDifficulty(DIFFICULTY_MEDIUM);
    }

    public void setGameStateListener(GameStateListener listener) {
        gameStateListener = listener;
        notifyGameState();
    }

    public void setDifficulty(String selectedDifficulty) {
        Level level = LevelRepository.forDifficulty(Difficulty.fromId(selectedDifficulty)).get(0);
        applyLevel(level);
    }

    public void setLevel(String levelId) {
        applyLevel(LevelRepository.byId(levelId));
    }

    private void applyLevel(Level level) {
        synchronized (engineLock) {
            engine = new GameEngine(level);
        }
        victoryAnimStartNanos = 0;
        calculateAnchors();
        notifyGameState();
    }

    public void resetGame() {
        synchronized (engineLock) {
            engine.reset();
        }
        victoryAnimStartNanos = 0;
        notifyGameState();
    }

    public void restoreState(String levelId, int[] flatGrid, int moves, int score,
                              long elapsedMillis, boolean won) {
        Level level = LevelRepository.byId(levelId);
        int gridSize = level.gridSize;
        int[][] grid = new int[gridSize][gridSize];
        for (int row = 0; row < gridSize; row++) {
            System.arraycopy(flatGrid, row * gridSize, grid[row], 0, gridSize);
        }
        synchronized (engineLock) {
            engine = GameEngine.restore(level, grid, moves, score, elapsedMillis, won);
        }
        victoryAnimStartNanos = 0;
        calculateAnchors();
        notifyGameState();
    }

    public int[] captureFlatGrid() {
        int[][] grid;
        int gridSize;
        synchronized (engineLock) {
            grid = engine.getGrid();
            gridSize = engine.getGridSize();
            int[] flat = new int[gridSize * gridSize];
            for (int row = 0; row < gridSize; row++) {
                System.arraycopy(grid[row], 0, flat, row * gridSize, gridSize);
            }
            return flat;
        }
    }

    public String getLevelId() {
        return engine.getLevel().id;
    }

    public int[][] getTarget() {
        return engine.getTarget();
    }

    public int getMoves() {
        return engine.getMoves();
    }

    public int getScore() {
        return engine.getScore();
    }

    public long getElapsedMillis() {
        return engine.getElapsedMillis();
    }

    public boolean isGameWon() {
        return engine.isWon();
    }

    public int getStars() {
        return engine.computeStars();
    }

    public boolean canUndo() {
        return engine.canUndo();
    }

    public void undo() {
        synchronized (engineLock) {
            engine.undo();
        }
        notifyGameState();
    }

    /** Blinks the row/column the engine judges closest to correct. Returns false if already solved. */
    public boolean requestHint() {
        GameEngine.HintTarget target;
        synchronized (engineLock) {
            target = engine.findHint();
        }
        if (target == null) {
            return false;
        }
        int axis = target.axis == GameEngine.Axis.ROW ? AXIS_ROW : AXIS_COLUMN;
        startHintBlink(axis, target.index);
        return true;
    }

    private void startHintBlink(int axis, int index) {
        if (hintAnimator != null) {
            hintAnimator.cancel();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f, 0f, 1f, 0f);
        animator.setDuration(HINT_BLINK_DURATION_MS);
        animator.addUpdateListener(a -> hintVisual = new HintVisual(axis, index, (float) a.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                hintVisual = NO_HINT;
            }
        });
        hintAnimator = animator;
        animator.start();
    }

    private void calculateAnchors() {
        int gridSize = engine.getGridSize();
        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);
        int availableWidth = Math.max(width - 40, 1);
        int availableHeight = Math.max(height - 40, 1);
        tileSize = Math.max(44, Math.min(availableWidth / gridSize, availableHeight / gridSize));
        carteLeftAnchor = (width - gridSize * tileSize) / 2;
        carteTopAnchor = (height - gridSize * tileSize) / 2;

        tileGap = tileSize * 0.06f;
        tileRadius = tileSize * 0.18f;
        shadowOffset = tileSize * 0.03f;
        tileRect.set(tileGap, tileGap, tileSize - tileGap, tileSize - tileGap);
        shadowRect.set(tileRect.left + shadowOffset, tileRect.top + shadowOffset,
                tileRect.right + shadowOffset, tileRect.bottom + shadowOffset);

        float size = tileSize - 2 * tileGap;
        blueGradient = new LinearGradient(0, 0, 0, size, blueStart, blueEnd, Shader.TileMode.CLAMP);
        redGradient = new LinearGradient(0, 0, 0, size, redStart, redEnd, Shader.TileMode.CLAMP);
    }

    private void paintCarte(Canvas canvas) {
        int[][] grid;
        int gridSize;
        synchronized (engineLock) {
            grid = engine.copyGrid();
            gridSize = engine.getGridSize();
        }
        DragVisual drag = dragVisual;
        long animStart = victoryAnimStartNanos;
        long victoryElapsedMs = animStart == 0 ? -1 : (System.nanoTime() - animStart) / 1_000_000L;

        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                int baseLeft = carteLeftAnchor + column * tileSize;
                int baseTop = carteTopAnchor + row * tileSize;
                boolean dragged = (drag.axis == AXIS_ROW && row == drag.index)
                        || (drag.axis == AXIS_COLUMN && column == drag.index);
                if (dragged) {
                    drawWrappedTile(canvas, grid[row][column], baseLeft, baseTop, drag.offsetPx,
                            drag.axis == AXIS_ROW, gridSize);
                } else {
                    float scale = victoryPopScale(victoryElapsedMs, row, column);
                    drawTile(canvas, grid[row][column], baseLeft, baseTop, scale);
                }
            }
        }

        paintHintOverlay(canvas, gridSize);
        paintVictoryFlash(canvas, victoryElapsedMs, gridSize);
    }

    private float victoryPopScale(long victoryElapsedMs, int row, int column) {
        if (victoryElapsedMs < 0) {
            return 1f;
        }
        long tileDelay = (row + column) * VICTORY_STAGGER_MS;
        long localT = victoryElapsedMs - tileDelay;
        if (localT < 0 || localT > VICTORY_POP_DURATION_MS) {
            return 1f;
        }
        double phase = Math.PI * localT / (double) VICTORY_POP_DURATION_MS;
        return 1f + 0.12f * (float) Math.sin(phase);
    }

    private void paintHintOverlay(Canvas canvas, int gridSize) {
        HintVisual hint = hintVisual;
        if (hint.axis == AXIS_NONE || hint.alpha <= 0f) {
            return;
        }
        hintPaint.setStrokeWidth(tileSize * 0.06f);
        hintPaint.setColor(withAlpha(0xFFFFFFFF, hint.alpha));
        for (int i = 0; i < gridSize; i++) {
            int row = hint.axis == AXIS_ROW ? hint.index : i;
            int column = hint.axis == AXIS_ROW ? i : hint.index;
            canvas.save();
            canvas.translate(carteLeftAnchor + column * tileSize, carteTopAnchor + row * tileSize);
            canvas.drawRoundRect(tileRect, tileRadius, tileRadius, hintPaint);
            canvas.restore();
        }
    }

    private void paintVictoryFlash(Canvas canvas, long victoryElapsedMs, int gridSize) {
        if (victoryElapsedMs < 0) {
            return;
        }
        long maxStagger = 2L * (gridSize - 1) * VICTORY_STAGGER_MS;
        long flashElapsed = victoryElapsedMs - (maxStagger + VICTORY_POP_DURATION_MS);
        if (flashElapsed < 0 || flashElapsed > VICTORY_FLASH_DURATION_MS) {
            if (flashElapsed > VICTORY_FLASH_DURATION_MS) {
                victoryAnimStartNanos = 0;
            }
            return;
        }
        float flashAlpha = 1f - flashElapsed / (float) VICTORY_FLASH_DURATION_MS;
        flashPaint.setColor(withAlpha(0xFFFFFFFF, flashAlpha * 0.6f));
        canvas.drawRect(carteLeftAnchor, carteTopAnchor,
                carteLeftAnchor + gridSize * tileSize, carteTopAnchor + gridSize * tileSize, flashPaint);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private void drawWrappedTile(Canvas canvas, int value, int baseLeft, int baseTop, float offsetPx,
                                  boolean horizontal, int gridSize) {
        int span = gridSize * tileSize;
        float wrapped = offsetPx > 0 ? offsetPx - span : offsetPx + span;
        if (horizontal) {
            drawTile(canvas, value, Math.round(baseLeft + offsetPx), baseTop, 1f);
            drawTile(canvas, value, Math.round(baseLeft + wrapped), baseTop, 1f);
        } else {
            drawTile(canvas, value, baseLeft, Math.round(baseTop + offsetPx), 1f);
            drawTile(canvas, value, baseLeft, Math.round(baseTop + wrapped), 1f);
        }
    }

    private void drawTile(Canvas canvas, int value, int left, int top, float scale) {
        canvas.save();
        canvas.translate(left, top);
        if (scale != 1f) {
            canvas.scale(scale, scale, tileSize / 2f, tileSize / 2f);
        }
        canvas.drawRoundRect(shadowRect, tileRadius, tileRadius, shadowPaint);
        tilePaint.setShader(value == CST_ROUGE ? redGradient : blueGradient);
        canvas.drawRoundRect(tileRect, tileRadius, tileRadius, tilePaint);
        canvas.restore();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        renderThread = new Thread(this);
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        calculateAnchors();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (renderThread != null) {
            try {
                renderThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            renderThread = null;
        }
    }

    @Override
    public void run() {
        long lastFrameTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            synchronized (engineLock) {
                engine.tick((now - lastFrameTime) / 1_000_000L);
            }
            lastFrameTime = now;

            Canvas c = null;
            try {
                c = holder.lockCanvas(null);
                if (c != null) {
                    nDraw(c);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }

            long frameNanos = System.nanoTime() - now;
            long sleepMillis = (FRAME_BUDGET_NANOS - frameNanos) / 1_000_000L;
            if (sleepMillis > 0) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }

    private void nDraw(Canvas canvas) {
        canvas.drawRGB(32, 38, 44);
        paintCarte(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (engine.isWon()) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                onTouchEnd(true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                onTouchEnd(false);
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void onTouchDown(MotionEvent event) {
        if (releaseAnimator != null) {
            releaseAnimator.cancel();
        }
        downX = event.getX();
        downY = event.getY();
        pendingColumn = Math.floorDiv((int) downX - carteLeftAnchor, tileSize);
        pendingRow = Math.floorDiv((int) downY - carteTopAnchor, tileSize);
        touchActive = isInsideGrid(pendingColumn, pendingRow, engine.getGridSize());
        lastHapticStep = 0;
        dragVisual = NO_DRAG;
    }

    private void onTouchMove(MotionEvent event) {
        if (!touchActive) {
            return;
        }
        float dx = event.getX() - downX;
        float dy = event.getY() - downY;
        DragVisual visual = dragVisual;

        if (visual.axis == AXIS_NONE) {
            if (Math.abs(dx) < touchSlop && Math.abs(dy) < touchSlop) {
                return;
            }
            int axis = Math.abs(dx) >= Math.abs(dy) ? AXIS_ROW : AXIS_COLUMN;
            int index = axis == AXIS_ROW ? pendingRow : pendingColumn;
            dragVisual = new DragVisual(axis, index, 0f);
            return;
        }

        float rawOffset = visual.axis == AXIS_ROW ? dx : dy;
        float maxOffset = engine.getGridSize() * (float) tileSize;
        float clamped = Math.max(-maxOffset, Math.min(maxOffset, rawOffset));
        dragVisual = new DragVisual(visual.axis, visual.index, clamped);

        int step = Math.round(clamped / tileSize);
        if (step != lastHapticStep) {
            lastHapticStep = step;
            if (hapticsEnabled) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
        }
    }

    private void onTouchEnd(boolean commit) {
        touchActive = false;
        DragVisual visual = dragVisual;
        if (visual.axis == AXIS_NONE) {
            return;
        }

        int steps = commit ? Math.round(visual.offsetPx / tileSize) : 0;
        float target = steps * (float) tileSize;

        ValueAnimator animator = ValueAnimator.ofFloat(visual.offsetPx, target);
        animator.setDuration(SNAP_DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator(1.6f));
        animator.addUpdateListener(a -> dragVisual = new DragVisual(visual.axis, visual.index, (float) a.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (steps != 0) {
                    boolean justWon;
                    synchronized (engineLock) {
                        boolean wasWon = engine.isWon();
                        if (visual.axis == AXIS_ROW) {
                            engine.rotateRow(visual.index, steps);
                        } else {
                            engine.rotateColumn(visual.index, steps);
                        }
                        justWon = !wasWon && engine.isWon();
                    }
                    vibrate(justWon ? new long[]{0, 60, 80, 60} : new long[]{0, 20});
                    if (justWon) {
                        victoryAnimStartNanos = System.nanoTime();
                    }
                    notifyGameState();
                }
                dragVisual = NO_DRAG;
            }
        });
        releaseAnimator = animator;
        animator.start();
    }

    @SuppressWarnings("deprecation")
    private static Vibrator createVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return manager.getDefaultVibrator();
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @SuppressWarnings("deprecation")
    private void vibrate(long[] pattern) {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    public int getElapsedSeconds() {
        return engine.getElapsedSeconds();
    }

    private void notifyGameState() {
        if (gameStateListener != null) {
            String difficultyLabel = getContext().getString(engine.getLevel().difficulty.labelRes);
            gameStateListener.onGameStateChanged(difficultyLabel, engine.getScore(), engine.getMoves(),
                    engine.getElapsedSeconds(), engine.isWon());
        }
        updateAccessibilityDescription();
    }

    private void updateAccessibilityDescription() {
        int gridSize = engine.getGridSize();
        int correct = engine.countCorrectCells();
        setContentDescription(getContext().getString(R.string.content_description_grid_format,
                gridSize, gridSize, correct, gridSize * gridSize));
    }

    private boolean isInsideGrid(int column, int row, int gridSize) {
        return column >= 0 && column < gridSize && row >= 0 && row < gridSize;
    }
}
