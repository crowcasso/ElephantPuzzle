package edu.elon.cs.elephantpuzzle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.DisplayMetrics;

public class Piece {

    protected float x, y;
    private float width, height;
    private Bitmap bitmap;
    protected String name;
    protected boolean isSelected;
    protected boolean isPlaced;
    private float finalX, finalY;
    protected float rotation;
    protected float rotX, rotY;
    private Matrix matrix;
    protected boolean first;
    protected int victory = 0;
    private int screenWidth, screenHeight;

    private final float PLACEMENT = 30.0f;
    private final float ANGLE = 10.0f;

    private final float SCALE = .68f;

    public Piece(Context context, int id, String name, float finalX, float finalY) {
        this.name = name;
        this.isSelected = false;
        this.isPlaced = false;
        this.finalX = finalX;
        this.finalY = finalY;

        matrix = new Matrix();
        rotation = 0.0f;

        // get the screen size
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        bitmap = BitmapFactory.decodeResource(context.getResources(), id);

        width = bitmap.getWidth() * SCALE;
        height = bitmap.getHeight() * SCALE;

        // start in the right place
        x = finalX;
        y = finalY;
        first = true;
    }

    public void doDraw(Canvas canvas) {
        if (victory == 0) {
            matrix.reset();
            matrix.postScale(SCALE, SCALE);
            matrix.postTranslate(x - width/2, y - height/2);
            //matrix.postRotate(rotation, x, y);
            canvas.drawBitmap(bitmap, matrix, null);
            //System.out.println(name + " " + x+ "," + y);
        } else {
            matrix.reset();
            matrix.postScale(SCALE, SCALE);
            matrix.postTranslate(x - width/2, y - height/2);
            //matrix.postRotate(rotation, screenWidth/2, screenHeight/2);
            canvas.drawBitmap(bitmap, matrix, null);
        }
    }

    private final int EDGE_BUFFER = 25;

    public void doUpdate(float touchX, float touchY) {

        isPlaced = false;
        if (touchX >= finalX - PLACEMENT && touchX <= finalX + PLACEMENT) {
            if (touchY >= finalY - PLACEMENT && touchY <= finalY + PLACEMENT) {
                if (rotation >= 360-ANGLE || rotation <= ANGLE) {
                    x = finalX;
                    y = finalY;
                    rotation = 0;
                    isPlaced = true;
                    return;
                }
            }
        }


        if (touchX > EDGE_BUFFER && touchX < screenWidth - EDGE_BUFFER)
            x = touchX;
        if (touchY > EDGE_BUFFER && touchY < screenHeight - EDGE_BUFFER)
            y = touchY;

        //System.out.println(x + "," + y);
    }

    public boolean touchCollision(float touchX, float touchY) {

        if (touchX >= x - width/2 && touchX <= x + width/2) {
            if (touchY >= y - height/2 && touchY <= y + height/2) {
                return true;
            }
        }

        return false;
    }
}