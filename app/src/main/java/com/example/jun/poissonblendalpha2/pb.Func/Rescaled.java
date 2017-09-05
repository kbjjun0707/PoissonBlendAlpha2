package com.example.jun.poissonblendalpha2.pb.Func;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Jun on 2016-11-08.
 */

public class Rescaled {
    public static Bitmap FitLayerWidth(Bitmap source, int LayerWidth) {
        float rate = (float) LayerWidth / source.getWidth();
        return Bitmap.createScaledBitmap(source, Math.round(source.getWidth() * rate), Math.round(source.getHeight() * rate), true);
    }

    public static Bitmap ScalePerBitmap(Bitmap source, float per) {
        return Bitmap.createScaledBitmap(source, Math.round(source.getWidth() * per), Math.round(source.getHeight() * per), true);
    }

    public static Bitmap ReSizeMax(Bitmap source, int max) {
        float ratio = (float) source.getWidth() / (float) source.getHeight();
        int tmp = (source.getWidth() > source.getHeight()) ? source.getWidth() : source.getHeight();
        if (tmp < max) max = tmp;
        int val = (int)(max * ((float) source.getHeight() / (float) source.getWidth()));
        if (ratio >= 1)
            return Bitmap.createScaledBitmap(source, max, val, true);
        else
            return Bitmap.createScaledBitmap(source, (int)(max * ratio), max, true);
    }

    public static Bitmap ComputeBmpNMsk(Bitmap source, Bitmap mask) {
        if (source == null || mask == null)
            return null;

        int srcW = source.getWidth(), mskW = mask.getWidth();
        int srcH = source.getHeight(), mskH = mask.getHeight();
        if (srcW != mskW || srcH != mskH)
            return null;

        Bitmap res = source.copy(Bitmap.Config.ARGB_8888, true);

        res.setHasAlpha(true);
        int rval = (244 << 16), minrval = (200 << 16);
        for (int r = 0; r < mskH; r++) {
            for (int c = 0; c < mskW; c++) {
                int val = (rval & mask.getPixel(c, r));
                if (((rval < val) || (minrval > val))) {
                    res.setPixel(c, r, Color.TRANSPARENT);
                    mask.setPixel(c, r, Color.BLACK);
                } else {
                        mask.setPixel(c, r, Color.RED);
                }
            }
        }

        return res;
    }

}
