package com.naukri.imagecropper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.naukri.imagecropper.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by akash.singla on 11/9/2015.
 */
public class Croper extends View {

    private Point pointPosition = null;
    private Point pointChange = null;
    private Point pointStart = null;
    private ImageCordinate imageCordinate = null;
    private EdgeSelection edgeSelection = null;

    private int selectedEdge = 0;

    private Bitmap bitmap = null;
    private int strokeWidth = 3;
    private String readLocation = null;
    private String writeLocation = null;

    private static int MARGIN = 20;
    private static float MIN_WIDTH = 100;
    private static float MIN_HEIGHT = 100;

    public Croper(Context context) {
        super(context);
    }

    public Croper(Context context, AttributeSet attrs) {
        super(context, attrs);
        imageCordinate = new ImageCordinate();
        pointPosition = new Point();
        pointStart = new Point();
        pointChange = new Point();
        edgeSelection = new EdgeSelection();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(bitmap != null) {
            // draw image.
            drawImage(canvas);
            // Un-Selected portion.
            drawShadowEffect(canvas);
            drawSelection(canvas);
        }
    }

    /**
     * Draw image.
     *
     * @param canvas
     */
    private void drawImage(Canvas canvas) {
        canvas.drawBitmap(bitmap, imageCordinate.startX, imageCordinate.startY, new Paint());
    }

    /**
     * Draw Shadow effect
     *
     * @param canvas
     */
    private void drawShadowEffect(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAlpha(90);
        canvas.drawRect(imageCordinate.startX, pointChange.bottom, imageCordinate.endX, imageCordinate.endY, paint);
        canvas.drawRect(imageCordinate.startX, imageCordinate.startY, imageCordinate.endX, pointChange.top, paint);
        canvas.drawRect(imageCordinate.startX, pointChange.top, pointChange.left, pointChange.bottom, paint);
        canvas.drawRect(pointChange.right, pointChange.top, imageCordinate.endX, pointChange.bottom, paint);
    }

    /**
     * Draw Image Selection
     *
     * @param canvas
     */
    private void drawSelection(Canvas canvas) {
        Paint paint = new Paint();
        paint.setAlpha(0);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        Paint paintRectangle = new Paint();
        paintRectangle.setColor(Color.TRANSPARENT);
        paintRectangle.setStrokeWidth(0);
        // Draw selected portion of image.
        canvas.drawRect(pointChange.left, pointChange.top, pointChange.right, pointChange.bottom, paint);
        canvas.drawRect(pointChange.left + strokeWidth, pointChange.top + strokeWidth,
                pointChange.right - strokeWidth, pointChange.bottom - strokeWidth, paintRectangle);
    }

    /**
     * Get Croped bitmap image.
     *
     * @return Bitmap
     */
    public void getImage() throws IOException {
        int width = (int) (pointPosition.right - pointPosition.left);
        int height = (int) (pointPosition.bottom - pointPosition.top);
        Bitmap imageBitmap = Bitmap.createBitmap(bitmap, (int) (pointPosition.left - imageCordinate.startX),
                (int) (pointPosition.top - imageCordinate.startY), width, height);

        writeImage(imageBitmap);
    }

    /**
     * Write image in writelocation path.
     *
     * @throws IOException
     */
    private void writeImage(Bitmap imageBitmap) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(new File(writeLocation));
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        int action = event.getAction();
        float getX = event.getX();
        float getY = event.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            if (!isTouchOnRectangle(getX, getY))
                return false;
            handleTouchActionDown(getX, getY);
        } else if (action == MotionEvent.ACTION_MOVE) {
            handleTouchActionMove(getX, getY);
        } else if (action == MotionEvent.ACTION_UP) {
            handleTouchActionUp();
        }
        return true;
    }

    private void handleTouchActionMove(float getX, float getY) {
        float dx;
        float dy;
        // Get change in co-ordinate.
        dx = (getX - pointStart.x);
        dy = (getY - pointStart.y);

        // For Move
        if (selectedEdge == Edge.NO_EDGE && (isLeftRightScreen(dx) || isTopBottomScreen(dy))) {
            handleMove(dx, dy);
        }
        // For Resizing.
        else {
            // check rectangle minimum size.
            if (checkMin(dx, dy, selectedEdge)) {
                // Left Bottom Corner.
                handleLeftBottom(getSizeSquare(dx, dy), dy);
                // Right Top Corner.
                handleRightTop(getSizeSquare(dx, dy), dy, (int) imageCordinate.endX);
                // Left Boundary.
                handleLeft(dx);
                //  Right edge resizing.
                handleRight(dx);
                // Top Boundary resizing.
                handleTop(dy);
                // Bottom Boundary resizing.
                handleBottom(dy);
            }
        }
        invalidate();
    }

    /**
     * Handle bottom edge for re-sizing.
     *
     * @param dy
     */
    private void handleBottom(float dy) {
        if (selectedEdge == Edge.BOTTOM && (((pointPosition.right + dy) <= imageCordinate.endX) &&
                ((pointPosition.bottom + dy) <= imageCordinate.endY))) {
            pointChange.right = (pointPosition.right + dy);
            pointChange.bottom = (pointPosition.bottom + dy);
            edgeSelection.bottom = true;
        } else if (selectedEdge == Edge.BOTTOM && edgeSelection.bottom) {
            // rectangle touch the right screen edge, then stop resizing.
            if ((pointPosition.right + dy) > imageCordinate.endX) {
                float diff = (imageCordinate.endX - pointPosition.right);
                pointChange.right = pointPosition.right + diff;
                pointChange.bottom = pointPosition.bottom + diff;
                edgeSelection.bottom = false;
            }
            // rectangle touch the bottom screen edge, then stop resizing.
            else if (((pointPosition.bottom + dy) > imageCordinate.endY)) {
                float diff = (imageCordinate.endY - pointPosition.bottom);
                pointChange.right = pointPosition.right + diff;
                pointChange.bottom = pointPosition.bottom + diff;
                edgeSelection.bottom = false;
            }

        }
    }

    /**
     * Handle top for resizing.
     *
     * @param dy
     */
    private void handleTop(float dy) {
        if (selectedEdge == Edge.Top && (((pointPosition.top + dy) >= imageCordinate.startY) &&
                ((pointPosition.left + dy) >= imageCordinate.startX))) {
            pointChange.top = (pointPosition.top + dy);
            pointChange.left = (pointPosition.left + dy);
            edgeSelection.top = true;
        } else if (selectedEdge == Edge.Top && edgeSelection.top) {
            // rectangle touch the top screen edge, then stop resizing.
            if (((pointPosition.top + dy) < imageCordinate.startY)) {
                float diff = pointPosition.top - imageCordinate.startY;
                pointChange.top = imageCordinate.startY;
                pointChange.left = pointPosition.left - diff;
                edgeSelection.top = false;
            }
            // rectangle touch the left screen edge, then stop resizing.
            if ((pointPosition.left + dy) < imageCordinate.startX) {
                float diff = pointPosition.left - imageCordinate.startX;
                pointChange.top = pointPosition.top - diff;
                pointChange.left = imageCordinate.startX;
                edgeSelection.top = false;
            }

        }
    }

    /**
     * Hanlde right edge for resizing.
     *
     * @param dx
     */
    private void handleRight(float dx) {
        if (selectedEdge == Edge.RIGHT && (((pointPosition.right + dx) <= imageCordinate.endX) &&
                (((pointPosition.bottom + dx)) <= imageCordinate.endY))) {
            pointChange.right = (pointPosition.right + dx);
            pointChange.bottom = (pointPosition.bottom + dx);
            edgeSelection.right = true;
        } else if (selectedEdge == Edge.RIGHT && edgeSelection.right) {
            // rectangle touch the right screen edge, then stop resizing.
            if (((pointPosition.right + dx) > imageCordinate.endX)) {
                float diff = imageCordinate.endX - pointPosition.right;
                pointChange.right = imageCordinate.endX;
                pointChange.bottom = pointPosition.bottom + diff;
                edgeSelection.right = false;
            }
            // rectangle touch the bottom screen edge, then stop resizing.
            if ((((pointPosition.bottom + dx)) > imageCordinate.endY)) {
                float diff = imageCordinate.endY - pointPosition.bottom;
                pointChange.bottom = imageCordinate.endY;
                pointChange.right = pointPosition.right + diff;
                edgeSelection.right = false;
            }
        }
    }

    /**
     * Handle left edge for resizing.
     *
     * @param dx
     */
    private void handleLeft(float dx) {
        if (selectedEdge == Edge.LEFT && (((pointPosition.left + dx) >= imageCordinate.startX) &&
                ((pointPosition.top + dx) >= imageCordinate.startY))) {
            pointChange.left = (pointPosition.left + dx);
            pointChange.top = (pointPosition.top + dx);
            edgeSelection.left = true;
        } else if (selectedEdge == Edge.LEFT && edgeSelection.left) {
            // rectangle touch the Left screen edge, then stop resizing. //
            if (((pointPosition.left + dx) <= imageCordinate.startX)) {
                float diff = pointPosition.left - imageCordinate.startX;
                pointChange.left = imageCordinate.startX;
                pointChange.top = pointPosition.top - diff;
                edgeSelection.left = false;
            }
            // rectangle touch the top screen edge, then stop resizing.
            if (((pointPosition.top + dx) < imageCordinate.startY)) {
                float diff = pointPosition.top - imageCordinate.startY;
                pointChange.top = imageCordinate.startY;
                pointChange.left = pointPosition.left - diff;
                edgeSelection.left = false;
            }
        }
    }

    /**
     * Handle top right corner for resizing.
     *
     * @param sizeSquare
     * @param dy
     * @param i
     */
    private void handleRightTop(float sizeSquare, float dy, int i) {
        if (selectedEdge == Edge.RIGHT_TOP && (((pointPosition.top + dy) >= imageCordinate.startY)
                && ((pointPosition.right - dy) <= i))) {
            float dz = sizeSquare;
            pointChange.top = (pointPosition.top + dy);
            pointChange.right = (pointPosition.right - dy);
            edgeSelection.rightTop = true;
        } else if (selectedEdge == Edge.RIGHT_TOP && edgeSelection.rightTop) {
            // rectangle touch the top screen edge, then stop resizing.
            if (((pointPosition.top + dy) < imageCordinate.startY)) {
                float diff = pointPosition.top - imageCordinate.startY;
                pointChange.top = imageCordinate.startY;
                pointChange.right = pointPosition.right + diff;
                edgeSelection.rightTop = false;
            }
            // rectangle touch the right screen edge, then stop resizing.
            if (((pointPosition.right - dy) > i)) {
                float diff = imageCordinate.endX - pointPosition.right;
                pointChange.right = i;
                pointChange.top = pointPosition.top - diff;
                edgeSelection.rightTop = false;
            }
        }
    }

    /**
     * Handle left bottom corner for resizing.
     *
     * @param sizeSquare
     * @param dy
     */
    private void handleLeftBottom(float sizeSquare, float dy) {
        if (selectedEdge == Edge.LEFT_BOTTOM && ((((pointPosition.left - dy)) >= imageCordinate.startX) &&
                (((pointPosition.bottom + dy)) <= imageCordinate.endY))) {
            float dz = sizeSquare;
            pointChange.left = (pointPosition.left - dy);
            pointChange.bottom = (pointPosition.bottom + dy);
            edgeSelection.leftBottom = true;

        } else if (selectedEdge == Edge.LEFT_BOTTOM && edgeSelection.leftBottom) {
            // rectangle touch the left screen edge, then stop resizing.
            if ((((pointPosition.left - dy)) < imageCordinate.startX)) {
                float diff = pointPosition.left - imageCordinate.startX;
                pointChange.left = imageCordinate.startX;
                pointChange.bottom = (pointPosition.bottom + diff);
                edgeSelection.leftBottom = false;
            }
            // rectangle touch the bottom screen edge, then stop resizing.
            if ((((pointPosition.bottom + dy)) > imageCordinate.endY)) {

                float diff = imageCordinate.endY - pointPosition.bottom;
                pointChange.bottom = imageCordinate.endY;
                pointChange.left = pointPosition.left - diff;
                edgeSelection.leftBottom = false;
            }
        }
    }

    /**
     * Handle move
     *
     * @param dx
     * @param dy
     */
    private void handleMove(float dx, float dy) {
        if (isLeftRightScreen(dx)) {
            pointChange.left = (pointPosition.left + dx);
            pointChange.right = (pointPosition.right + dx);
        }
        if (isTopBottomScreen(dy)) {
            pointChange.top = (pointPosition.top + dy);
            pointChange.bottom = (pointPosition.bottom + dy);
        }
    }

    /**
     * Handle touch UpAction: when user take finger from screen.
     */
    private void handleTouchActionUp() {
        selectedEdge = 0;
        // set position variable.
        pointPosition.left = pointChange.left;
        pointPosition.right = pointChange.right;
        pointPosition.top = pointChange.top;
        pointPosition.bottom = pointChange.bottom;
        // set size of rectangle
        pointPosition.width = pointPosition.right - pointPosition.left;
        pointPosition.height = pointPosition.bottom - pointPosition.top;
    }

    /**
     * Handle Touch DownAction : when user start touch the screen.
     *
     * @param getX
     * @param getY
     */
    private void handleTouchActionDown(float getX, float getY) {
        // Store user starting touch co-ordinate.
        pointStart.x = getX;
        pointStart.y = getY;
        // If User select on any edge/ boundary.
        if ((((pointChange.bottom - MARGIN) < getY) && ((pointChange.bottom + MARGIN) > getY)) &&
                (((pointChange.left - MARGIN) < getX) && ((pointChange.left + MARGIN) > getX))) {
            selectedEdge = Edge.LEFT_BOTTOM;
        } else if ((((pointChange.top - MARGIN) < getY) && ((pointChange.top + MARGIN) > getY)) &&
                (((pointChange.right - MARGIN) < getX) && ((pointChange.right + MARGIN) > getX))) {
            selectedEdge = Edge.RIGHT_TOP;
        } else if (((pointChange.left + MARGIN) > getX && (pointChange.left - MARGIN) < getX)) {
            selectedEdge = Edge.LEFT;
        } else if (((pointChange.right - MARGIN) < getX && (pointChange.right + MARGIN) > getX)) {
            selectedEdge = Edge.RIGHT;
        } else if (((pointChange.top - MARGIN) < getY && (pointChange.top + MARGIN) > getY)) {
            selectedEdge = Edge.Top;
        } else if (((pointChange.bottom - MARGIN) < getY && (pointChange.bottom + MARGIN) > getY)) {
            selectedEdge = Edge.BOTTOM;
        }
    }

    /**
     * Check for minimum rectangel size.
     *
     * @param dx
     * @param dy
     * @param edge
     * @return
     */
    private boolean checkMin(float dx, float dy, int edge) {

        switch (edge) {
            case Edge.Top:
                if (handleMinLeftTop(dx, dy))
                    return true;
                break;

            case Edge.LEFT:
                if (handleMinLeftTop(dx, dy)) return true;
                break;

            case Edge.RIGHT:
                if (handleMinRightBottom(dx, dy)) return true;
                break;

            case Edge.BOTTOM:
                if (handleMinRightBottom(dx, dy)) return true;
                break;

            case Edge.LEFT_BOTTOM:
                if (handleMinLeftBottom(dx, dy)) return true;
                break;

            case Edge.RIGHT_TOP:

                if (handleMinRightTop(dx, dy)) return true;
                break;
        }

        return false;
    }

    /**
     * check minimum rectangle size when user touch on top or left edge.
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean handleMinRightTop(float dx, float dy) {
        if ((pointPosition.right - pointPosition.left + dx) >= MIN_WIDTH &&
                ((pointPosition.bottom - pointPosition.top - dy) >= MIN_HEIGHT))
            return true;
        else {
            pointChange.right = (pointPosition.left + MIN_WIDTH);
            pointChange.top = (pointPosition.bottom - MIN_WIDTH);
        }
        return false;
    }

    /**
     * check minimum rectangle size when user touch on left and bottom corner.
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean handleMinLeftBottom(float dx, float dy) {
        if ((pointPosition.right - pointPosition.left - dx) >= MIN_WIDTH &&
                ((pointPosition.bottom - pointPosition.top + dy) >= MIN_HEIGHT))
            return true;
        else {
            pointChange.left = (pointPosition.right - MIN_WIDTH);
            pointChange.bottom = (pointPosition.top + MIN_WIDTH);
        }
        return false;
    }

    /**
     * check minimum rectangle size when user touch on right or bottom edge.
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean handleMinRightBottom(float dx, float dy) {

        if (((pointPosition.right + dx) - (pointPosition.left)) >= MIN_WIDTH &&
                (((pointPosition.bottom + dy) - pointPosition.top) >= MIN_HEIGHT)) {
            return true;
        } else {
            pointChange.right = (pointPosition.left + MIN_WIDTH);
            pointChange.bottom = (pointPosition.top + MIN_WIDTH);
        }

        return false;
    }

    /**
     * check minimum rectangle size when user touch on left and top corner.
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean handleMinLeftTop(float dx, float dy) {
        if ((pointPosition.right - pointPosition.left - dx) >= MIN_WIDTH && ((pointPosition.bottom - pointPosition.top - dy) >= MIN_HEIGHT))
            return true;
        else {
            pointChange.left = (pointPosition.right - MIN_WIDTH);
            pointChange.top = (pointPosition.bottom - MIN_WIDTH);
        }
        return false;
    }

    /**
     * Get Square size of rectangle.
     *
     * @param X
     * @param Y
     * @return
     */
    private float getSizeSquare(float X, float Y) {
        float squareX = (X * X);
        float squareY = (Y * Y);

        float Z = (float) Math.sqrt((squareX + squareY));

        return Z;
    }

    /**
     * @param diffX
     * @return
     */
    private boolean isLeftRightScreen(float diffX) {
        boolean inScreen = true;
        float leftEdge = pointPosition.left + diffX;
        float rightEdge = (pointPosition.right + diffX);
        if (leftEdge < imageCordinate.startX) {
            inScreen = false;
            pointChange.left = imageCordinate.startX;
            pointChange.right = (imageCordinate.startX + pointPosition.width);
            requestLayout();
        }
        if (rightEdge > imageCordinate.endX) {
            inScreen = false;
            pointChange.left = imageCordinate.endX - pointPosition.width;
            pointChange.right = imageCordinate.endX;
            requestLayout();
        }

        return inScreen;
    }

    /**
     * @param diffY
     * @return
     */
    private boolean isTopBottomScreen(float diffY) {
        boolean inScreen = true;

        float topEdge = pointPosition.top + diffY;
        float bottomEdge = (pointPosition.bottom + diffY);

        if (topEdge < imageCordinate.startY) {
            inScreen = false;
            pointChange.top = imageCordinate.startY;
            pointChange.bottom = (imageCordinate.startY + pointPosition.height);
            requestLayout();
        }
        if (bottomEdge > imageCordinate.endY) {
            inScreen = false;
            pointChange.bottom = imageCordinate.endY;
            pointChange.top = imageCordinate.endY - pointPosition.height;
            requestLayout();
        }

        return inScreen;
    }

    /**
     * Check user touch in rectangle area or not.
     *
     * @param x
     * @param y
     * @return
     */
    private boolean isTouchOnRectangle(float x, float y) {
        boolean isTouchOnRectangle = false;

        if (((pointChange.left - MARGIN) <= x)
                && ((pointChange.right + MARGIN) >= x)
                && ((pointChange.top - MARGIN) <= y)
                && ((pointChange.bottom + MARGIN) >= y)
                ) {
            isTouchOnRectangle = true;
        }
        return isTouchOnRectangle;
    }

    /**
     * Get file read and write location.
     *
     * @param readLocation
     * @param writeLocation
     */
    public void setLocation(String readLocation, String writeLocation) {
        this.readLocation = readLocation;
        this.writeLocation = writeLocation;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

//        try {
        setRectangleSize(w, h);
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//            Toast.makeText(getContext(), "Image not found at specified loation", Toast.LENGTH_SHORT).show();
//        }
    }

    /**
     * Set rectangle size.
     */
    private void setRectangleSize(int width, int height)
    {
        try {
            bitmap = FileUtil.decodeSampledBitmapFromResource(readLocation, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bitmap == null)
        {
            Activity cropperActivity = (Activity) getContext();
            cropperActivity.setResult(Activity.RESULT_CANCELED);
            cropperActivity.finish();
        }
        else {
            imageCordinate.width = bitmap.getWidth();
            imageCordinate.height = bitmap.getHeight();

            boolean isSizeGreater = false;

            // check image size is greated than screen size or not.
            if (imageCordinate.width > getWidth() || imageCordinate.height > getHeight()) {
                isSizeGreater = true;
            }

            // if image size is greated than screen size, then reduce the image size according the aspect ratio of image.
            if (isSizeGreater) {
                // ratioY = screenHeight / Image Height.
                float ratioY = (getHeight() / imageCordinate.height);

                // ratioX = screenWidth / Image Width;
                float ratioX = (getWidth() / imageCordinate.width);

                // get min ration, so image can fit in screen.
                float aspectRatio = Math.min(ratioX, ratioY);

                // calculate new image size.
                imageCordinate.width = (aspectRatio * imageCordinate.width);
                imageCordinate.height = (aspectRatio * imageCordinate.height);

                // resize image bitmap according to new size.
                bitmap = Bitmap.createScaledBitmap(bitmap, (int) imageCordinate.width, (int) imageCordinate.height, false);
            }

            // get Image starting X,Y co-ordinate.
            imageCordinate.startX = ((getWidth() - imageCordinate.width) / 2);
            imageCordinate.startY = ((getHeight() - imageCordinate.height) / 2);
            imageCordinate.endX = (imageCordinate.startX + imageCordinate.width);
            imageCordinate.endY = (imageCordinate.startY + imageCordinate.height);

            float size = imageCordinate.height;
            if (imageCordinate.height > imageCordinate.width) {
                size = imageCordinate.width;
            }

            int sideSpace = (int) (size * 0.10f);
            size = (size - (2 * sideSpace));

            MIN_HEIGHT = size / 5;
            MIN_WIDTH = size / 5;
            MARGIN = (int) (size / 20);

            // set changable values.
            pointChange.left = imageCordinate.startX + sideSpace;
            pointChange.top = imageCordinate.startY + sideSpace;
            pointChange.right = pointChange.left + size;
            pointChange.bottom = pointChange.top + size;

            // set position variables
            pointPosition.left = pointChange.left;
            pointPosition.right = pointChange.right;
            pointPosition.bottom = pointChange.bottom;
            pointPosition.top = pointChange.top;

            // set height and width of rectangle.
            pointPosition.width = size;
            pointPosition.height = size;
        }
    }
}
