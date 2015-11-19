package edu.elon.cs.elephantpuzzle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private Context context;
    private SurfaceHolder surfaceHolder;
    private GameViewThread thread;
    private int screenWidth, screenHeight;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        // get the screen size
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        thread = new GameViewThread(context);
    }

    @SuppressLint("ClickableViewAccessibility") @Override
    public boolean onTouchEvent(MotionEvent event) {
        thread.onTouchEvent(event);
        return true;
    }

    // SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (thread.getState() == Thread.State.TERMINATED) {
            thread = new GameViewThread(context);
        }

        thread.setIsRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setIsRunning(false);

        System.out.println("surfaceDestroyed");

        while (retry) {
            try {
                thread.join();
                retry = false;
            }
            catch (InterruptedException e) { }
        }
    }

    // Game Loop Thread
    private class GameViewThread extends Thread {

        private boolean isRunning;
        private long lastTime;

        private int frames;
        private long nextUpdate;

        private Bitmap background;
        private Piece [] puzzle;
        private int currentPiece = -1;
        private Rect backRect;
        private boolean isWinner;

        private float xDiff = 0;
        private float yDiff = 0;
        private float initDegree = 0;
        private float startRot = 0;

        private final int DRAG = 0;
        private final int ROTATE = 1;
        private int mode = DRAG;

        public GameViewThread(Context context) {
            isRunning = false;
            frames = 0;

            int startX = 325;
            int startY = 150;
            background = BitmapFactory.decodeResource(context.getResources(), R.drawable.board_gray);
            backRect = new Rect(startX, startY, startX + (int)(screenWidth * .725f), startY + (int)(screenHeight * .795f));

            puzzle = new Piece[8];
            puzzle[0] = new Piece(context, R.drawable.head, "head", 694.1075f, 621.4866f);
            puzzle[1] = new Piece(context, R.drawable.trunk, "trunk", 543.05457f, 1030.032f);
            puzzle[2] = new Piece(context, R.drawable.back_foot, "back_foot", 1930, 1181);
            puzzle[3] = new Piece(context, R.drawable.back_head, "back_head", 1199.4921f, 449.7558f);
            puzzle[4] = new Piece(context, R.drawable.front_body, "front_body", 1184, 870);
            puzzle[5] = new Piece(context, R.drawable.front_foot, "front_foot", 1043, 1167.5752f);
            puzzle[6] = new Piece(context, R.drawable.tail, "tail", 1775, 876);
            puzzle[7] = new Piece(context, R.drawable.top_tail, "top_tail", 1743.2882f, 451.24908f);

            isWinner = false;
        }

        public void setIsRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        private void onTouchEvent(MotionEvent event) {

            // get pointer index from event object
            int pointerIndex = event.getActionIndex();
            // get pointer ID
            int pointerID = event.getPointerId(pointerIndex);

            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // find the topmost piece that is being touched
                    for (int i = puzzle.length - 1; i >= 0; i--) {
                        if (puzzle[i].touchCollision(event.getX(), event.getY())) {
                            xDiff = event.getX() - puzzle[i].x;
                            yDiff = event.getY() - puzzle[i].y;
                            startRot = puzzle[i].rotation;
                            currentPiece = i;
                            break;
                        }
                    }

                    if (currentPiece != -1) {
                        // shuffle the pieces down, put touched piece on top
                        puzzle[currentPiece].isSelected = true;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = ROTATE;
                    double delta_x = (event.getX(0) - event.getX(1));
                    double delta_y = (event.getY(0) - event.getY(1));
                    double radians = Math.atan2(delta_y, delta_x);
                    initDegree = (float) Math.toDegrees(radians);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (currentPiece != -1 && event.getPointerCount() == 1 && mode == DRAG) {
                        puzzle[currentPiece].doUpdate(event.getX() - xDiff, event.getY() - yDiff);
                    }
                    if (event.getPointerCount() > 1 && currentPiece != -1) {
                        double rads = Math.atan2(event.getY(0) - event.getY(1), event.getX(0) - event.getX(1));
                        double diff = Math.toDegrees(rads) - initDegree;
                        puzzle[currentPiece].rotX = event.getX(0) + event.getX(1) / 2;
                        puzzle[currentPiece].rotY = event.getY(0) + event.getY(1) / 2;
                        puzzle[currentPiece].rotation = (float) Math.abs(startRot + diff);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mode = DRAG;
                    if (currentPiece != -1) {
                        puzzle[currentPiece].isSelected = false;
                        puzzle[currentPiece].first = false;
                        currentPiece = -1;
                    }
                    break;
            } // end of switch
        }

        private void doDraw(Canvas canvas) {
            // draw the background
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(background, null, backRect, null);

            // shuffle the order toward last touched
            if (currentPiece != -1 && currentPiece != puzzle.length - 1) {
                Piece tmp = puzzle[currentPiece];
                for (int i = currentPiece; i < puzzle.length - 1; i++) {
                    puzzle[i] = puzzle[i+1];
                }
                puzzle[puzzle.length - 1] = tmp;
                currentPiece = puzzle.length - 1;
            }

            isWinner = puzzle[0].isPlaced;
            for (int i = 1; i < puzzle.length; i++) {
                isWinner = isWinner && puzzle[i].isPlaced;
            }
            if (isWinner) {
                // reset
                for (int i = 0; i < puzzle.length; i++) {
                    //puzzle[i].victory = 1;
                    puzzle[i].isPlaced = false;
                }
            }



            // draw the pieces
            for (int i = 0; i < puzzle.length; i++) {
                if (i != currentPiece) {
                    puzzle[i].doDraw(canvas);
                }
            }
            if (currentPiece != -1) {
                puzzle[currentPiece].doDraw(canvas);
            }
        }

        private void doUpdate(double elapsed) {

        }

        private void updateFPS(long now) {
            float fps = 0.0f;
            ++frames;
            float overtime = now - nextUpdate;
            if (overtime > 0) {
                fps = frames / (1 + overtime/1000.0f);
                frames = 0;
                nextUpdate = System.currentTimeMillis() + 1000;
            }
        }

        @Override
        public void run() {

            lastTime = System.currentTimeMillis() + 100;

            while (isRunning) {
                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    if (canvas == null) {
                        isRunning = false;
                        continue;
                    }

                    synchronized(surfaceHolder) {
                        long now = System.currentTimeMillis();
                        double elapsed = (now - lastTime) / 1000.0;
                        lastTime = now;
                        updateFPS(now);
                        doUpdate(elapsed);
                        doDraw(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}