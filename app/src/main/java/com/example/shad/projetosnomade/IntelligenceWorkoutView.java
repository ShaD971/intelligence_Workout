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
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by shad on 18/12/15.
 */
public class IntelligenceWorkoutView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface GameStateListener {
        void onGameStateChanged(String difficultyLabel, int score, int moves, int elapsedSeconds, boolean gameWon);
    }

    public static final String DIFFICULTY_EASY = "easy";
    public static final String DIFFICULTY_MEDIUM = "medium";
    public static final String DIFFICULTY_HARD = "hard";

    private static final int CST_BLEU = 0;
    private static final int CST_ROUGE = 1;

    private Bitmap bleu;
    private Bitmap rouge;
    private Bitmap minibleu;
    private Bitmap minirouge;
    private Bitmap win;

    private int[][] carte;
    private int[][] initialCarte;
    private int[][] minicarte;
    private int gridSize;
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
    private int score;
    private int moves;
    private int startScore;
    private int movePenalty;
    private int parMoves;
    private int finishBonus;
    private int quickMoveBonus;
    private long startTimeMillis;
    private int finalElapsedSeconds;
    private boolean gameWon;
    private String difficulty = DIFFICULTY_MEDIUM;
    private String difficultyLabel = "Moyen";
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

        initParameters();
        cvThread = new Thread(this);
        setFocusable(true);
    }

    public void setGameStateListener(GameStateListener listener) {
        gameStateListener = listener;
        notifyGameState();
    }

    public void setDifficulty(String selectedDifficulty) {
        if (DIFFICULTY_EASY.equals(selectedDifficulty)) {
            difficulty = DIFFICULTY_EASY;
            difficultyLabel = "Facile";
            startScore = 120;
            movePenalty = 4;
            parMoves = 2;
            finishBonus = 60;
            quickMoveBonus = 15;
        } else if (DIFFICULTY_HARD.equals(selectedDifficulty)) {
            difficulty = DIFFICULTY_HARD;
            difficultyLabel = "Difficile";
            startScore = 360;
            movePenalty = 12;
            parMoves = 6;
            finishBonus = 280;
            quickMoveBonus = 50;
        } else {
            difficulty = DIFFICULTY_MEDIUM;
            difficultyLabel = "Moyen";
            startScore = 220;
            movePenalty = 8;
            parMoves = 4;
            finishBonus = 140;
            quickMoveBonus = 30;
        }
        initParameters();
    }

    public void resetGame() {
        carte = copyGrid(initialCarte);
        score = startScore;
        moves = 0;
        startTimeMillis = System.currentTimeMillis();
        finalElapsedSeconds = 0;
        gameWon = false;
        notifyGameState();
    }

    public void initParameters() {
        configureScoreIfNeeded();

        int[][] target = targetForDifficulty();
        int[][] start = startForDifficulty();
        gridSize = target.length;
        minicarte = copyGrid(target);
        initialCarte = copyGrid(start);
        carte = copyGrid(start);
        score = startScore;
        moves = 0;
        startTimeMillis = System.currentTimeMillis();
        finalElapsedSeconds = 0;
        gameWon = false;

        calculateAnchors();
        notifyGameState();

        if ((cvThread != null) && (!cvThread.isAlive())) {
            cvThread.start();
            Log.e("-FCT-", "cv_thread.start()");
        }
    }

    private void configureScoreIfNeeded() {
        if (startScore > 0) {
            return;
        }
        startScore = 220;
        movePenalty = 8;
        parMoves = 4;
        finishBonus = 140;
        quickMoveBonus = 30;
    }

    private int[][] targetForDifficulty() {
        if (DIFFICULTY_EASY.equals(difficulty)) {
            return new int[][]{
                    {CST_BLEU, CST_ROUGE, CST_BLEU},
                    {CST_ROUGE, CST_ROUGE, CST_ROUGE},
                    {CST_BLEU, CST_ROUGE, CST_BLEU}
            };
        }
        if (DIFFICULTY_HARD.equals(difficulty)) {
            return new int[][]{
                    {CST_ROUGE, CST_BLEU, CST_BLEU, CST_BLEU, CST_BLEU, CST_ROUGE},
                    {CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU},
                    {CST_BLEU, CST_BLEU, CST_ROUGE, CST_ROUGE, CST_BLEU, CST_BLEU},
                    {CST_BLEU, CST_BLEU, CST_ROUGE, CST_ROUGE, CST_BLEU, CST_BLEU},
                    {CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU},
                    {CST_ROUGE, CST_BLEU, CST_BLEU, CST_BLEU, CST_BLEU, CST_ROUGE}
            };
        }
        return new int[][]{
                {CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU},
                {CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU},
                {CST_ROUGE, CST_ROUGE, CST_ROUGE, CST_ROUGE, CST_ROUGE},
                {CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU},
                {CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU}
        };
    }

    private int[][] startForDifficulty() {
        if (DIFFICULTY_EASY.equals(difficulty)) {
            return new int[][]{
                    {CST_BLEU, CST_BLEU, CST_ROUGE},
                    {CST_ROUGE, CST_ROUGE, CST_ROUGE},
                    {CST_ROUGE, CST_BLEU, CST_BLEU}
            };
        }
        if (DIFFICULTY_HARD.equals(difficulty)) {
            return new int[][]{
                    {CST_BLEU, CST_BLEU, CST_BLEU, CST_BLEU, CST_ROUGE, CST_ROUGE},
                    {CST_BLEU, CST_ROUGE, CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU},
                    {CST_ROUGE, CST_BLEU, CST_BLEU, CST_BLEU, CST_BLEU, CST_ROUGE},
                    {CST_BLEU, CST_BLEU, CST_ROUGE, CST_ROUGE, CST_BLEU, CST_BLEU},
                    {CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU, CST_ROUGE},
                    {CST_ROUGE, CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU}
            };
        }
        return new int[][]{
                {CST_ROUGE, CST_BLEU, CST_BLEU, CST_BLEU, CST_BLEU},
                {CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU, CST_BLEU},
                {CST_ROUGE, CST_ROUGE, CST_ROUGE, CST_ROUGE, CST_ROUGE},
                {CST_BLEU, CST_BLEU, CST_BLEU, CST_BLEU, CST_ROUGE},
                {CST_BLEU, CST_BLEU, CST_ROUGE, CST_BLEU, CST_BLEU}
        };
    }

    private int[][] copyGrid(int[][] source) {
        int[][] result = new int[source.length][source.length];
        for (int row = 0; row < source.length; row++) {
            for (int column = 0; column < source[row].length; column++) {
                result[row][column] = source[row][column];
            }
        }
        return result;
    }

    private void calculateAnchors() {
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
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                drawTile(canvas, carte[row][column], carteLeftAnchor + column * tileSize,
                        carteTopAnchor + row * tileSize, tileSize, false);
            }
        }
    }

    private void paintMiniCarte(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        canvas.drawText("Cible", miniLeftAnchor, Math.max(28, miniTopAnchor - 14), paint);
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                drawTile(canvas, minicarte[row][column], miniLeftAnchor + column * miniTileSize,
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
        canvas.drawText("Score final : " + score, left, 150 + imageHeight + 44, paint);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("-> FCT <-", "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("-> FCT <-", "surfaceChanged " + width + " - " + height);
        calculateAnchors();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("-> FCT <-", "surfaceDestroyed");
    }

    @Override
    public void run() {
        Canvas c = null;
        while (in) {
            try {
                Thread.sleep(40);

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
            } catch (Exception e) {
                Log.e("-> RUN <-", "PB DANS RUN");
            }
        }
    }

    private boolean win() {
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                if (carte[row][column] != minicarte[row][column]) {
                    return false;
                }
            }
        }
        return true;
    }

    private void nDraw(Canvas canvas) {
        canvas.drawRGB(32, 38, 44);
        paintMiniCarte(canvas);
        paintCarte(canvas);
        if (gameWon) {
            paintWin(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameWon) {
            return true;
        }

        xchange = ((int) event.getX() - carteLeftAnchor) / tileSize;
        ychange = ((int) event.getY() - carteTopAnchor) / tileSize;

        if (!isInsideGrid(xchange, ychange)) {
            return true;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                xtemp = xchange;
                ytemp = ychange;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (xtemp != xchange) {
                    if (xchange - xtemp > 0) {
                        rotateRowRight(ytemp);
                    } else {
                        rotateRowLeft(ytemp);
                    }
                    xtemp = xchange;
                    recordMove();
                }
                if (ytemp != ychange) {
                    if (ychange - ytemp > 0) {
                        rotateColumnDown(xtemp);
                    } else {
                        rotateColumnUp(xtemp);
                    }
                    ytemp = ychange;
                    recordMove();
                }
                break;
            case MotionEvent.ACTION_UP:
                checkForWin();
                break;
            default:
                break;
        }

        return true;
    }

    private void recordMove() {
        moves++;
        score = Math.max(0, score - movePenalty);
        checkForWin();
        notifyGameState();
    }

    private void checkForWin() {
        if (!gameWon && win()) {
            int fastFinishBonus = Math.max(0, (parMoves - moves + 1) * quickMoveBonus);
            int bonus = finishBonus + fastFinishBonus;
            score += bonus;
            finalElapsedSeconds = getElapsedSeconds();
            gameWon = true;
            notifyGameState();
        }
    }

    public int getElapsedSeconds() {
        if (gameWon) {
            return finalElapsedSeconds;
        }
        return (int) ((System.currentTimeMillis() - startTimeMillis) / 1000);
    }

    private void notifyGameState() {
        if (gameStateListener != null) {
            gameStateListener.onGameStateChanged(difficultyLabel, score, moves, getElapsedSeconds(), gameWon);
        }
    }

    private boolean isInsideGrid(int column, int row) {
        return column >= 0 && column < gridSize && row >= 0 && row < gridSize;
    }

    private void rotateRowRight(int row) {
        int last = carte[row][gridSize - 1];
        for (int column = gridSize - 1; column > 0; column--) {
            carte[row][column] = carte[row][column - 1];
        }
        carte[row][0] = last;
    }

    private void rotateRowLeft(int row) {
        int first = carte[row][0];
        for (int column = 0; column < gridSize - 1; column++) {
            carte[row][column] = carte[row][column + 1];
        }
        carte[row][gridSize - 1] = first;
    }

    private void rotateColumnDown(int column) {
        int last = carte[gridSize - 1][column];
        for (int row = gridSize - 1; row > 0; row--) {
            carte[row][column] = carte[row - 1][column];
        }
        carte[0][column] = last;
    }

    private void rotateColumnUp(int column) {
        int first = carte[0][column];
        for (int row = 0; row < gridSize - 1; row++) {
            carte[row][column] = carte[row + 1][column];
        }
        carte[gridSize - 1][column] = first;
    }
}
