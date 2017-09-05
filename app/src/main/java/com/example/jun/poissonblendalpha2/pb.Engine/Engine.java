package com.example.jun.poissonblendalpha2.pb.Engine;

import android.util.Log;

import com.example.jun.poissonblendalpha2.MainActivity;
import com.example.jun.poissonblendalpha2.pb.Circleprogress.CircleProgressView;
import com.example.jun.poissonblendalpha2.pb.Info.AppInfo;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Objdetect;

import java.util.ArrayList;

/**
 * Created by Jun on 2016-11-08.
 */

public class Engine {
    public static int LOOP_MAX = 1900;
    int NUM_NEIGHBOR = 4;
    private CircleProgressView Cpv;

    public Engine(CircleProgressView c) {
        Cpv = c;
    }

    public Mat quasi_poisson_solver_test3(Mat img_dstC1, Mat img_src, Mat img_mask, int channel, int offset[], Rect Roi) {
        int i, j, loop, neighbor, count_neighbors;
        boolean ok;
        double error, sum_f, sum_vpq, fp;
        int[][] naddr = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};

        Mat img_new = img_dstC1.clone();
        img_new.convertTo(img_new, CvType.CV_64FC1);
        Mat tmp_src = img_src.clone();
        tmp_src.convertTo(tmp_src, CvType.CV_64FC4);
        Mat tmp_msk = img_mask.clone();
        tmp_msk.convertTo(tmp_msk, CvType.CV_64FC4);

        int dstW = img_dstC1.cols();
        int srcW = tmp_src.cols(), srcCh = tmp_src.channels();
        int mskW = img_mask.cols(), mskCh = tmp_msk.channels();

        double[] arr_new = new double[img_dstC1.rows() * dstW];
        img_new.get(0, 0, arr_new);
        double[] arr_src = new double[img_src.height() * img_src.width() * srcCh];
        tmp_src.get(0, 0, arr_src);
        double[] arr_msk = new double[tmp_msk.rows() * tmp_msk.cols() * tmp_msk.channels()];
        tmp_msk.get(0, 0, arr_msk);

        tmp_src.release();
        tmp_msk.release();

        int DstR = img_dstC1.rows();
        int DstC = img_dstC1.cols();
        int MskR = (offset[0] + Roi.y + Roi.height < DstR) ? (int)(Roi.y + Roi.height) : DstR - offset[0];
        int MskC = (offset[1] + Roi.x + Roi.width < DstC) ? (int)(Roi.x + Roi.width) : DstC - offset[1];
        int StatR = (offset[0] + Roi.y > 0) ? Roi.y : Math.abs(offset[0]);
        int StatC = (offset[1] + Roi.x > 0) ? Roi.x : Math.abs(offset[1]);

        for (loop = 0; loop < LOOP_MAX; loop++) {
            AppInfo.CompleteCalc += 1f;
            ok = true;
            for (i = StatR; i < MskR; i++) {
                for (j = StatC; j < MskC; j++) {
                    if (arr_msk[i * mskW * mskCh + j * mskCh] > 0.0) {
                        sum_f = 0.0;
                        sum_vpq = 0.0;
                        count_neighbors = 0;
                        for (neighbor = 0; neighbor < NUM_NEIGHBOR; neighbor++) {
                            if (i + offset[0] + naddr[neighbor][0] >= 0
                                    && j + offset[1] + naddr[neighbor][1] >= 0
                                    && i + offset[0] + naddr[neighbor][0] < DstR
                                    && j + offset[1] + naddr[neighbor][1] < DstC) {
                                sum_f += arr_new[((i + offset[0] + naddr[neighbor][0]) * dstW) + (j + offset[1] + naddr[neighbor][1])];
                                try {
                                    sum_vpq += (arr_src[i * srcW * srcCh + j * srcCh + channel] - arr_src[(i + naddr[neighbor][0]) * srcW * srcCh + (j + naddr[neighbor][1]) * srcCh + channel]);
                                }catch (ArrayIndexOutOfBoundsException e){
//                                    Log.e("         image", "w : " + img_src.width() + ", h : " + img_src.height());
//                                    Log.e("         error", srcW + ", " + srcCh);
//                                    Log.e("         index", "j : " + j + ", i : " + i);
//                                    Log.e("         index1", "" + (i * srcW * srcCh + j * srcCh + channel));
//                                    Log.e("         index2", "" + ((i + naddr[neighbor][0]) * srcW * srcCh + (j + naddr[neighbor][1]) * srcCh + channel));
                                }
                                count_neighbors++;
                            }
                        }
                        fp = (sum_f + sum_vpq) / count_neighbors;
                        error = Math.abs(fp - arr_new[(i + offset[0]) * dstW + (j + offset[1])]);

                        if (ok && error > 0) {
                            ok = false;
                        }
                        arr_new[(i + offset[0]) * dstW + (j + offset[1])] = fp;
                    }
                }
            }
            if (ok) {
                break;
            }
        }
        img_new.put(0, 0, arr_new);
        img_new.convertTo(img_new, CvType.CV_8UC1);

        return img_new;
    }
}
