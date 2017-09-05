package com.example.jun.poissonblendalpha2.pb.Info;

import android.os.Environment;

/**
 * Created by Jun on 2016-11-08.
 */

public class AppInfo {
    static {
        System.loadLibrary("opencv_java3");
    }

    // App proccess info
    public static float CompleteCalc;
    public static float MaxCalc;
    public static String SdcardDir = Environment.getExternalStorageDirectory().toString();
    public static String AppFolder = "Blender";

    // AppFlowState
    public static final int NONE = 1, BASE = (1 << 1), ATTACHED = (1 << 2), DURING = (1 << 3), DONE = (1 << 4);

    // Activity Request Code
    public final static int GALLERY_REQUEST = 5152, ATTACH_REQUEST = 5153, MASKEDIT_REQUEST = 5154;
}
