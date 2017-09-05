package com.example.jun.poissonblendalpha2.pb.Func;

import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Created by Jun on 2016-11-10.
 */

public class LayoutScale {
    public static void ratioScaleFitWIdth(RelativeLayout frame, int sourceW, int sourceH) {
        float ratio = (float) sourceH / (float) sourceW;

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) frame.getLayoutParams();
        lp.width = frame.getWidth();
        lp.height = Math.round(lp.width * ratio);
        frame.setLayoutParams(lp);
    }
}
