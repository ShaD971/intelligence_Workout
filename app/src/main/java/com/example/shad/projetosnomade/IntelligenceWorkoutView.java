package com.example.shad.projetosnomade;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.example.shad.projetosnomade.game.Difficulty;
import com.example.shad.projetosnomade.game.GameEngine;
import com.example.shad.projetosnomade.game.Level;
import com.example.shad.projetosnomade.game.LevelRepository;

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

    private final Object engineLock = new Object();
    private final int blueStart;
    private final int blueEnd;
    private final int redStart;
    private final int redEnd;
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tileRect = new RectF();
    private final RectF shadowRect = new RectF();

    private GameEngine engine;
    private int tileSize;
    private float tileRadius;
    private float tileGap;
    private float shadowOffset;
    private LinearGradient blueGradient;
    private LinearGradient redGradient;
    private int carteTopAnchor;
    private int carteLeftAnchor;
    private int xchange;
    private int ychange;
    private int xtemp;
    private int ytemp;
    private GameStateListener gameStateListener;

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
        calculateAnchors();
        notifyGameState();
    }

    public void resetGame() {
        synchronized (engineLock) {
            engine.reset();
        }
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
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                drawTile(canvas, grid[row][column], carteLeftAnchor + column * tileSize,
                        carteTopAnchor + row * tileSize);
            }
        }
    }

    private void drawTile(Canvas canvas, int value, int left, int top) {
        canvas.save();
        canvas.translate(left, top);
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

        int gridSize = engine.getGridSize();
        xchange = Math.floorDiv((int) event.getX() - carteLeftAnchor, tileSize);
        ychange = Math.floorDiv((int) event.getY() - carteTopAnchor, tileSize);

        if (!isInsideGrid(xchange, ychange, gridSize)) {
            return true;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                xtemp = xchange;
                ytemp = ychange;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (xtemp != xchange) {
                    synchronized (engineLock) {
                        engine.rotateRow(ytemp, xchange - xtemp > 0 ? 1 : -1);
                    }
                    xtemp = xchange;
                    notifyGameState();
                }
                if (ytemp != ychange) {
                    synchronized (engineLock) {
                        engine.rotateColumn(xtemp, ychange - ytemp > 0 ? 1 : -1);
                    }
                    ytemp = ychange;
                    notifyGameState();
                }
                break;
            default:
                break;
        }

        return true;
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
    }

    private boolean isInsideGrid(int column, int row, int gridSize) {
        return column >= 0 && column < gridSize && row >= 0 && row < gridSize;
    }
}
