package com.example.shad.projetosnomade;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import static android.support.v4.view.MotionEventCompat.AXIS_X;
import static android.support.v4.view.MotionEventCompat.AXIS_Y;
import static android.support.v4.view.MotionEventCompat.getActionMasked;

/**
 * Created by shad on 18/12/15.
 */
public class IntelligenceWorkoutView extends SurfaceView implements SurfaceHolder.Callback,Runnable {

    // Declaration des images
    private Bitmap bleu;
    private Bitmap rouge;
    private Bitmap minibleu;
    private Bitmap minirouge;
    private Bitmap 		win;

    // Declaration des objets Ressources et Context permettant d'acc�der aux ressources de notre application et de les charger
    private Resources mRes;
    private Context mContext;

    // tableau modelisant la carte du jeu
    int[][] carte;
    int[][] minicarte;

    // ancres pour pouvoir centrer la carte du jeu
    int carteTopAnchor;                   // coordonn�es en Y du point d'ancrage de notre carte
    int carteLeftAnchor;                  // coordonn�es en X du point d'ancrage de notre carte

    // taille de la minicarte
    static final int carteWidthminicarte = 5;
    static final int carteHeightminicarte = 5;
    static final int carteTileSizeminicarte = 50;


    // ancres pour pouvoir centrer la minicarte du jeu
    int carteTopAnchormincarte;                   // coordonn�es en Y du point d'ancrage de notre carte
    int carteLeftAnchorminicarte;                  // coordonn�es en X du point d'ancrage de notre carte

    // taille de la carte
    static final int carteWidth = 5;
    static final int carteHeight = 5;
    static final int carteTileSize = 200;
    // constante modelisant les differentes types de cases
    static final int CST_bleu = 0;
    static final int CST_rouge = 1;

    //variable X et Y pour bouger les case
    int xchange, ychange, x, y, xtemp, ytemp;

    //variable score
    private int score;
    private int niv=0;
//tableau de reference du jeux
    int[][] niv1 = {
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},
            {CST_rouge, CST_rouge, CST_rouge, CST_rouge, CST_rouge},
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},
            {CST_bleu, CST_bleu, CST_bleu, CST_rouge, CST_bleu},

    };
    int[][] mininiv1 = {
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},
            {CST_rouge, CST_rouge, CST_rouge, CST_rouge, CST_rouge},
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},

    };
    int[][] mininiv2 = {
            {CST_rouge, CST_bleu, CST_bleu, CST_bleu, CST_rouge},
            {CST_bleu, CST_rouge, CST_bleu, CST_rouge, CST_bleu},
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},
            {CST_bleu, CST_rouge, CST_bleu, CST_rouge, CST_bleu},
            {CST_rouge, CST_bleu, CST_bleu, CST_bleu, CST_rouge},

    };
    int[][] niv2 = {
            {CST_bleu, CST_rouge, CST_bleu, CST_bleu, CST_rouge},
            {CST_bleu, CST_rouge, CST_bleu, CST_rouge, CST_bleu},
            {CST_bleu, CST_bleu, CST_rouge, CST_bleu, CST_bleu},
            {CST_rouge, CST_bleu, CST_bleu, CST_rouge, CST_bleu},
            {CST_rouge, CST_bleu, CST_rouge, CST_bleu, CST_bleu},

    };



    // thread utiliser pour animer les zones de depot des diamants
    private boolean in = true;
    private Thread cv_thread;

    SurfaceHolder holder;

    Paint paint;


    public IntelligenceWorkoutView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // permet d'ecouter les surfaceChanged, surfaceCreated, surfaceDestroyed
        holder = getHolder();
        holder.addCallback(this);

        // chargement des images
        mContext = context;
        mRes = mContext.getResources();
        bleu = BitmapFactory.decodeResource(mRes, R.mipmap.blue);
        rouge = BitmapFactory.decodeResource(mRes, R.mipmap.red);
        minibleu = BitmapFactory.decodeResource(mRes, R.mipmap.miniblue);
        minirouge = BitmapFactory.decodeResource(mRes, R.mipmap.minired);
        win 		= BitmapFactory.decodeResource(mRes, R.mipmap.win);

        initparameters();
        // creation du thread
        cv_thread = new Thread(this);
        // prise de focus pour gestion des touches
        setFocusable(true);

    }

    // chargement du niveau a partir du tableau de reference du niveau
    private void loadlevel() {

            for (int i = 0; i < carteHeight; i++) {
                for (int j = 0; j < carteWidth; j++) {
                    carte[j][i] = niv1[j][i];
                }
            }
            for (int i = 0; i < carteHeightminicarte; i++) {
                for (int j = 0; j < carteWidthminicarte; j++) {
                    minicarte[j][i] = mininiv1[j][i];
                }
            }
        }

    private void loadlevel2() {

            for (int i = 0; i < carteHeight; i++) {
                for (int j = 0; j < carteWidth; j++) {
                    carte[j][i] = niv2[j][i];
                }
            }
            for (int i = 0; i < carteHeightminicarte; i++) {
                for (int j = 0; j < carteWidthminicarte; j++) {
                    minicarte[j][i] = mininiv2[j][i];
                }
            }
    }
    // initialisation du jeu
    public void initparameters() {
        paint = new Paint();
        paint.setColor(0xff0000);

        paint.setDither(true);
        paint.setColor(0xFFFFFF00);
        paint.setStyle(Paint.Style.STROKE);

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(3);
        paint.setTextAlign(Paint.Align.LEFT);
        carte = new int[carteHeight][carteWidth];
        minicarte = new int[carteHeightminicarte][carteWidthminicarte];
        if(niv==1){
            score=0;
            loadlevel2();
        }else {
            loadlevel();
        }
        carteTopAnchor = (getHeight() - carteHeight * carteTileSize);
        carteLeftAnchor = (getWidth() - carteWidth * carteTileSize) / 2;
        carteTopAnchormincarte = (getHeight() - carteHeightminicarte * carteTileSizeminicarte) - 1350;
        carteLeftAnchorminicarte = (getWidth() - carteWidthminicarte * carteTileSizeminicarte) / 2;

        // creation du thread
        if ((cv_thread != null) && (!cv_thread.isAlive())) {
            cv_thread.start();
            Log.e("-FCT-", "cv_thread.start()");
        }
    }


    // dessin de la carte du jeu
    private void paintcarte(Canvas canvas) {
        for (int i = 0; i < carteHeight; i++) {

            for (int j = 0; j < carteWidth; j++) {

                switch (carte[i][j]) {

                    case CST_bleu:

                        canvas.drawBitmap(bleu, carteLeftAnchor + j * carteTileSize, carteTopAnchor + i * carteTileSize, null);
                        break;

                    case CST_rouge:
                        canvas.drawBitmap(rouge, carteLeftAnchor + j * carteTileSize, carteTopAnchor + i * carteTileSize, null);
                        break;
                }
            }
        }
    }

    // dessin de la carte du jeu
    private void paintminicarte(Canvas canvas) {
        for (int i = 0; i < carteHeightminicarte; i++) {
            for (int j = 0; j < carteWidthminicarte; j++) {
                switch (minicarte[i][j]) {

                    case CST_bleu:

                        canvas.drawBitmap(minibleu, carteLeftAnchorminicarte + j * carteTileSizeminicarte, carteTopAnchormincarte + i * carteTileSizeminicarte, null);
                        break;


                    case CST_rouge:
                        canvas.drawBitmap(minirouge, carteLeftAnchorminicarte + j * carteTileSizeminicarte, carteTopAnchormincarte + i * carteTileSizeminicarte, null);
                        break;
                }
            }
        }
    }

    //dessin le score
    private void paintscore(Canvas canvas) {
        Paint paintscore = new Paint();

        paintscore.setColor(Color.RED);
        paintscore.setTextSize(70);
        String drawString = "Move : " + score;
        canvas.drawText(drawString, 50, 250, paintscore);

    }
    // dessin du gagne si gagne
    private void paintwin(Canvas canvas) {
        canvas.drawBitmap(win, 190, 300, null);
    }

        @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("-> FCT <-", "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("-> FCT <-", "surfaceChanged " + width + " - " + height);
        initparameters();
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
                cv_thread.sleep(40);

                try {
                    c = holder.lockCanvas(null);
                    nDraw(c);
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
    private boolean win(){//fonction gagnée
        int compteur=0;//comteur de case
        for(int i=0;i<carteHeight;i++){
            for (int j=0;j<carteWidth;j++){

                if(carte[i][j]==minicarte[i][j]) {

                    compteur++;
                   // Log.i("hello", " compteur=" + compteur);

                }
                if(compteur==25){
                   // niv=1;
                    return true;
                }

            }
        }
        return false;
    }

    private void nDraw(Canvas canvas) {//fonction qui gere le jeux
        canvas.drawRGB(44, 44, 44);
        if(win()){
            paintwin(canvas);
            paintscore(canvas);

            if(niv==1) {
                initparameters();
                paintcarte(canvas);
                paintminicarte(canvas);
                paintscore(canvas);
            }

        }else {
            paintcarte(canvas);
            paintminicarte(canvas);
            paintscore(canvas);
        }
    }





    // fonction permettant de recuperer les evenements tactiles
    public boolean onTouchEvent(MotionEvent event) {
        Log.i("-> FCT <-", "onTouchEvent: x = " + event.getX() + " y =" + event.getY());
        x = (int) event.getX();
        y = (int) event.getY();

        xchange =  ((x) / carteTileSize);
        ychange =  ((y - carteTopAnchor) / carteTileSize);


        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN://on recupere les x et y au moment du clik

                xtemp = xchange;
                ytemp = ychange;

                Log.i("hello", "xchange=" + xchange + "ychange=" + ychange);

                return true;
            case MotionEvent.ACTION_MOVE:


                if (xtemp != xchange) {// on compare la valeur de x pendent le movement

                    if (xchange - xtemp > 0) {// le mouvement est vers la droite
                        xtemp = xchange;


                        Log.i("hello", "move  right" + xtemp + " ," + ytemp);
                        int ligne[] = new int[5];//vecteur permettant de recuéré une ligne
                        for (int i = 0; i < carteHeight; i++) {
                            ligne[i] = carte[ytemp][i];

                        }

                        for (int i = 1; i < carteHeight; i++) {
                            carte[ytemp][i] = ligne[i - 1];// on recopie le vecteur dans la matrice avec un decalage vers la droit

                        }
                        carte[ytemp][0] = ligne[4];//la denier case remplace la 1er

                    } else if (xchange - xtemp < 0) {
                        xtemp = xchange;


                        Log.i("hello", "move   lefte" + xtemp + " ," + ytemp);
                        int ligne[] = new int[5];
                        for (int i = 0; i < carteHeight; i++) {
                            ligne[i] = carte[ytemp][i];
                        }

                        for (int i = 0; i < (carteHeight - 1); i++) {
                            carte[ytemp][i] = ligne[i + 1];//on recopie le vecteur dans la matrice avec un decalage vers la gauche

                        }
                        carte[ytemp][4] = ligne[0];
                    }

                }
                if (ytemp != ychange) {//  on compare la valeur de y pendent le movement

                    if (ychange - ytemp > 0) {// le mouvement est vers le bas
                        ytemp = ychange;


                        Log.i("hello", "move  right" + xtemp + " ," + ytemp);
                        int colone[] = new int[5];//vecteur permettant de recuéré une colone
                        for (int i = 0; i < carteWidth; i++) {
                            colone[i] = carte[i][xtemp];
                        }

                        for (int i = 1; i < carteWidth; i++) {
                            carte[i][xtemp] = colone[i - 1];//on recopie le vecteur dans la matrice avec un decalage vers le haut
                        }
                        carte[0][xtemp] = colone[4];
                    } else if (ychange - ytemp < 0) {// le mouvement est vers le haut
                        ytemp = ychange;// on demarre de la position clique


                        Log.i("hello", "move   lefte" + xtemp + " ," + ytemp);
                        int colone[] = new int[5];
                        for (int i = 0; i < carteWidth; i++) {
                            colone[i] = carte[i][xtemp];
                        }

                        for (int i = 0; i < carteWidth - 1; i++) {
                            carte[i][xtemp] = colone[i + 1];//on recopie le vecteur dans la matrice avec un decalage vers le bas
                        }
                        carte[4][xtemp] = colone[0];
                    }
                }


                break;
            case MotionEvent.ACTION_UP:
                score++;
                break;

            default:
                break;

        }
        if(win()) {
            try {
                sleep();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            niv = 1;
        }

        return super.onTouchEvent(event);
    }
    private void sleep() throws InterruptedException {       Thread.sleep(1000);}

}

