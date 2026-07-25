package com.example.shad.projetosnomade;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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

    private Bitmap bleu;
    private Bitmap rouge;
    private Bitmap minibleu;
    private Bitmap minirouge;
    private Bitmap win;

    private GameEngine engine;
    private int tileSize;
    private int miniTileSize;
    private int carteTopAnchor;
    private int carteLeftAnchor;
    private int miniTopAnchor;
    private int miniLeftAnchor;
    private int xchange;
    private int ychange;
    private int xtemp;
    private int ytemp;
    private GameStateListener gameStateListener;

    private boolean in = true;
    private Thread cvThread;
    private SurfaceHolder holder;
    private Paint paint;

    public IntelligenceWorkoutView(Context context, AttributeSet attrs) {
        super(context, attrs);

        holder = getHolder();
        holder.addCallback(this);

        Resources res = context.getResources();
        bleu = BitmapFactory.decodeResource(res, R.mipmap.blue);
        rouge = BitmapFactory.decodeResource(res, R.mipmap.red);
        minibleu = BitmapFactory.decodeResource(res, R.mipmap.miniblue);
        minirouge = BitmapFactory.decodeResource(res, R.mipmap.minired);
        win = BitmapFactory.decodeResource(res, R.mipmap.win);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.LEFT);

        cvThread = new Thread(this);
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
        engine = new GameEngine(level);
        calculateAnchors();
        notifyGameState();

        if (cvThread != null && !cvThread.isAlive()) {
            cvThread.start();
        }
    }

    public void resetGame() {
        engine.reset();
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
        engine = GameEngine.restore(level, grid, moves, score, elapsedMillis, won);
        calculateAnchors();
        notifyGameState();

        if (cvThread != null && !cvThread.isAlive()) {
            cvThread.start();
        }
    }

    public int[] captureFlatGrid() {
        int[][] grid = engine.getGrid();
        int gridSize = engine.getGridSize();
        int[] flat = new int[gridSize * gridSize];
        for (int row = 0; row < gridSize; row++) {
            System.arraycopy(grid[row], 0, flat, row * gridSize, gridSize);
        }
        return flat;
    }

    public String getLevelId() {
        return engine.getLevel().id;
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

    private void calculateAnchors() {
        int gridSize = engine.getGridSize();
        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);
        int availableWidth = Math.max(width - 40, 1);
        int availableHeight = Math.max(height - 220, 1);
        tileSize = Math.max(44, Math.min(availableWidth / gridSize, availableHeight / gridSize));
        miniTileSize = Math.max(22, Math.min(44, tileSize / 3));
        carteLeftAnchor = (width - gridSize * tileSize) / 2;
        carteTopAnchor = Math.max(170, height - gridSize * tileSize - 30);
        miniLeftAnchor = (width - gridSize * miniTileSize) / 2;
        miniTopAnchor = 48;
    }

    private void paintCarte(Canvas canvas) {
        int[][] grid = engine.getGrid();
        int gridSize = engine.getGridSize();
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                drawTile(canvas, grid[row][column], carteLeftAnchor + column * tileSize,
                        carteTopAnchor + row * tileSize, tileSize, false);
            }
        }
    }

    private void paintMiniCarte(Canvas canvas) {
        int[][] target = engine.getTarget();
        int gridSize = engine.getGridSize();
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        canvas.drawText("Cible", miniLeftAnchor, Math.max(28, miniTopAnchor - 14), paint);
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                drawTile(canvas, target[row][column], miniLeftAnchor + column * miniTileSize,
                        miniTopAnchor + row * miniTileSize, miniTileSize, true);
            }
        }
    }

    private void drawTile(Canvas canvas, int value, int left, int top, int size, boolean mini) {
        Bitmap bitmap;
        if (value == CST_ROUGE) {
            bitmap = mini ? minirouge : rouge;
        } else {
            bitmap = mini ? minibleu : bleu;
        }
        canvas.drawBitmap(bitmap, null, new Rect(left, top, left + size, top + size), null);
    }

    private void paintWin(Canvas canvas) {
        int width = Math.max(getWidth(), 1);
        int imageWidth = Math.min(width - 80, 520);
        int imageHeight = Math.max(120, imageWidth / 2);
        int left = (width - imageWidth) / 2;
        canvas.drawBitmap(win, null, new Rect(left, 150, left + imageWidth, 150 + imageHeight), null);

        paint.setColor(Color.WHITE);
        paint.setTextSize(34);
        canvas.drawText("Score final : " + engine.getScore(), left, 150 + imageHeight + 44, paint);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        calculateAnchors();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void run() {
        long lastFrameTime = System.currentTimeMillis();
        Canvas c = null;
        while (in) {
            try {
                Thread.sleep(40);

                long now = System.currentTimeMillis();
                engine.tick(now - lastFrameTime);
                lastFrameTime = now;

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
            } catch (Exception ignored) {
            }
        }
    }

    private void nDraw(Canvas canvas) {
        canvas.drawRGB(32, 38, 44);
        paintMiniCarte(canvas);
        paintCarte(canvas);
        if (engine.isWon()) {
            paintWin(canvas);
        }
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
                    engine.rotateRow(ytemp, xchange - xtemp > 0 ? 1 : -1);
                    xtemp = xchange;
                    notifyGameState();
                }
                if (ytemp != ychange) {
                    engine.rotateColumn(xtemp, ychange - ytemp > 0 ? 1 : -1);
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
