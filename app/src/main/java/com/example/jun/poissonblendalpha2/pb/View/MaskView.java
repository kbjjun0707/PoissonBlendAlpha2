package com.example.jun.poissonblendalpha2.pb.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.example.jun.poissonblendalpha2.pb.Func.Rescaled;

import org.opencv.core.Rect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Jun on 2016-11-10.
 */

public class MaskView extends ImageView {
    private Paint mPaint, mStrokePaint, mBitmapPaint, mBrushstrokePaint, mBruchPaint, mEraserPaint;
    private Bitmap maskBitmap, subMaskBitmap;
    private Canvas mCanvas, mSubCanvas;
    private Path mPath, mSubPath;
    private float mX, mY, startX, startY, ratio;
    boolean setBitmap = false, DrawMode = false;
    private static final float TOUCH_TOLERANCE = 4;
    double sX, lX, sY, lY;
    float verRate = 1;

    public MaskView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPath = new Path();
        mSubPath = new Path();

        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mBruchPaint = new Paint();
        mBruchPaint.setAntiAlias(false);
        mBruchPaint.setDither(true);
        mBruchPaint.setColor(Color.argb(60, 255, 0, 0));
        mBruchPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mBrushstrokePaint = new Paint();
        mBrushstrokePaint.setAntiAlias(false);
        mBrushstrokePaint.setDither(true);
        mBrushstrokePaint.setColor(Color.argb(60, 255, 0, 0));
        mBrushstrokePaint.setStyle(Paint.Style.STROKE);
        mBrushstrokePaint.setStrokeJoin(Paint.Join.ROUND);
        mBrushstrokePaint.setStrokeCap(Paint.Cap.ROUND);
        mBrushstrokePaint.setStrokeWidth(3);

        mEraserPaint = new Paint();
        mEraserPaint.setAlpha(0);
        mEraserPaint.setAntiAlias(false);
        mEraserPaint.setDither(true);
        mEraserPaint.setColor(Color.argb(0, 0, 0, 0));
        mEraserPaint.setStyle(Paint.Style.STROKE);
        mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mEraserPaint.setStrokeWidth(24);

        mPaint = mEraserPaint;
        mStrokePaint = mEraserPaint;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        this.maskBitmap = bm;

        float viewRate = (float) getWidth() / getHeight(), imgRate = (float) bm.getWidth() / bm.getHeight();
        int reSizeX = bm.getWidth(), reSizeY = bm.getHeight();
        if (viewRate >= imgRate) {  // 이미지가 세로로 긴 것
            reSizeY = getHeight();
            reSizeX = Math.round(reSizeX * (float) getHeight() / bm.getHeight());
            verRate = (float)bm.getHeight() / getHeight();
        } else {
            reSizeY = Math.round(reSizeY * (float) getWidth() / bm.getWidth());
            reSizeX = getWidth();
            verRate = 1;
        }
        subMaskBitmap = Bitmap.createScaledBitmap(bm, reSizeX, reSizeY, true);
        ratio = (float) maskBitmap.getWidth() / subMaskBitmap.getWidth();

        super.setImageBitmap(subMaskBitmap);

        sX = bm.getWidth();
        lX = 0;
        sY = bm.getHeight();
        lY = 0;

        mCanvas = new Canvas(maskBitmap);
        mSubCanvas = new Canvas(subMaskBitmap);
        setBitmap = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (setBitmap) {
            canvas.drawBitmap(subMaskBitmap, 0, 0, mBitmapPaint);
        }
    }

    public void setBrush() {
        mStrokePaint = mBrushstrokePaint;
        mPaint = mBruchPaint;
        DrawMode = true;
    }

    public void setEraser() {
        mPaint = mEraserPaint;
        mStrokePaint = mEraserPaint;
        DrawMode = false;
    }

    public double[] getBound() {
        return new double[]{sX, sY, lX - sX, lY - sY};
    }

    public float getVerRatio(){return verRate;}

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mSubPath.reset();
        mSubPath.moveTo(x * ratio, y * ratio);
        startX = x;
        startY = y;
        mX = x;
        mY = y;
        float tx = (x + mX) * ratio / 2, ty = (y + mY) * ratio / 2;
        if (mStrokePaint != mEraserPaint) {
            if (sX > tx) sX = tx;
            if (lX < tx) lX = tx;
            if (sY > ty) sY = ty;
            if (lY < ty) lY = ty;
        }
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);


        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.lineTo((x + mX) / 2, (y + mY) / 2);
            float tx = (x + mX) * ratio / 2, ty = (y + mY) * ratio / 2;
            mSubPath.lineTo(tx, ty);
            mX = x;
            mY = y;
            if (setBitmap) {
                mSubCanvas.drawPath(mPath, mStrokePaint);
                mCanvas.drawPath(mSubPath, mStrokePaint);
                if (mStrokePaint != mEraserPaint) {
                    if (sX > tx) sX = tx;
                    if (lX < tx) lX = tx;
                    if (sY > ty) sY = ty;
                    if (lY < ty) lY = ty;
                }
            }
        }
    }

    private void touch_up() {
        if (DrawMode)
            mPath.lineTo(startX, startY);
        if (setBitmap) {
            mSubCanvas.drawPath(mPath, mPaint);
            mCanvas.drawPath(mSubPath, mPaint);
        }
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }

        return true;
    }
}
