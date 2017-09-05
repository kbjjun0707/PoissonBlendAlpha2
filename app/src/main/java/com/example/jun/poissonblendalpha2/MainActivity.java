package com.example.jun.poissonblendalpha2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.jun.poissonblendalpha2.pb.Adapter.GalleryAdapter;
import com.example.jun.poissonblendalpha2.pb.Circleprogress.CircleProgressView;
import com.example.jun.poissonblendalpha2.pb.Circleprogress.TextMode;
import com.example.jun.poissonblendalpha2.pb.Circleprogress.UnitPosition;
import com.example.jun.poissonblendalpha2.pb.Engine.Engine;
import com.example.jun.poissonblendalpha2.pb.Func.LayoutScale;
import com.example.jun.poissonblendalpha2.pb.Func.Rescaled;
import com.example.jun.poissonblendalpha2.pb.Info.AppInfo;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.example.jun.poissonblendalpha2.pb.Func.GetImage.getAll_ImagesPath;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> ImgList;
    AppBarLayout Abl;
    CircleProgressView Cpv;
    Button ActBtn;
    ImageView LogoIV, BaseIV, AttachIV;
    float MainRatio;
    boolean r1 = false, r2 = false, r3 = false, readPermission;
    Engine engine;

    // Layout
    RelativeLayout ImageFrame;
    RelativeLayout.LayoutParams AttachLayoutParams;
    NestedScrollView mNestedSV;

    // Bitmap
    Bitmap BaseBmp, SourceBmp, MaskBmp, MaskedBmp, ResBmp;

    // CV Mat
    Mat BaseMat, SrcMat, MskMat;
    Mat Rr, Gr, Br, Ar, ResultMat;
    Rect Roi;

    Thread Rt, Gt, Bt;

    // LIstener
    Gallery.OnItemSelectedListener galleryItemSelectedListener = new Gallery.OnItemSelectedListener() {
        int pre = 0;
        View v = null;

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (pre != i) {
                v.setScaleX(1);
                v.setScaleY(1);
            }
            view.setScaleX(1.3f);
            view.setScaleY(1.3f);
            pre = i;
            v = view;
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };
    Gallery.OnItemClickListener galleryItemClickListener = new Gallery.OnItemClickListener() {
        int selected = 0;

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            selected = adapterView.getSelectedItemPosition();
            if (selected == i) {
                BaseBmp = Rescaled.ReSizeMax((Bitmap) adapterView.getItemAtPosition(i), 680);       // 선택 이미지 리스케일
                LogoIV.setVisibility(View.GONE);
                Abl.setExpanded(false);                 // 앱바 숨기기

                LayoutScale.ratioScaleFitWIdth(ImageFrame, BaseBmp.getWidth(), BaseBmp.getHeight());    // 이미지 비율에 따라 프레임 크기 조절
                MainRatio = (float) BaseBmp.getWidth() / (float) ImageFrame.getWidth();
                BaseIV.setImageBitmap(BaseBmp);             // 리스케일 이미지 삽입

                ActBtn.setVisibility(View.VISIBLE);
                ActBtn.setText("Target");
                ActBtn.setOnClickListener(baseBtnClickListener);

                try {
                    File Folder = new File(AppInfo.SdcardDir + "/" + AppInfo.AppFolder);
                    if (!Folder.exists())
                        Folder.mkdirs();
                    File BBmp = new File(Folder.getPath(), "base.jpg");
                    BaseBmp.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(BBmp));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    Button.OnClickListener baseBtnClickListener = new Button.OnClickListener() {
        Intent intent;

        @Override
        public void onClick(View view) {
            intent = new Intent(MainActivity.this, EditActivity.class);
            startActivityForResult(intent, AppInfo.ATTACH_REQUEST);
        }
    };
    Button.OnClickListener doneBtnClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View view) {
            File Folder = new File(AppInfo.SdcardDir + "/" + AppInfo.AppFolder);
            File ResFile = new File(Folder.getPath(), "res.jpg");
            try {
                ResBmp.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(ResFile));
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE));
                Toast.makeText(MainActivity.this, "\"" + ResFile.getAbsolutePath().toString() + "\"" + " 저장", Toast.LENGTH_SHORT).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    };
    Button.OnClickListener attachedBtnClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View view) {
            Toast.makeText(MainActivity.this, "시작", Toast.LENGTH_SHORT).show();
            AppInfo.CompleteCalc = 0;

            Cpv.setVisibility(CircleProgressView.VISIBLE);

            BaseMat = new Mat(BaseBmp.getHeight(), BaseBmp.getWidth(), CvType.CV_8UC4);
            SrcMat = new Mat(SourceBmp.getHeight(), SourceBmp.getWidth(), CvType.CV_8UC4);
            MskMat = new Mat(MaskBmp.getHeight(), MaskBmp.getWidth(), CvType.CV_8UC4);
            ResultMat = new Mat(BaseBmp.getHeight(), BaseBmp.getWidth(), CvType.CV_8UC4);

            Utils.bitmapToMat(BaseBmp, BaseMat);
            Utils.bitmapToMat(SourceBmp, SrcMat);
            Utils.bitmapToMat(MaskBmp, MskMat);

            final int[] offset = {Math.round(AttachLayoutParams.topMargin * MainRatio), Math.round(AttachLayoutParams.leftMargin * MainRatio)};

            final ArrayList<Mat> Channels = new ArrayList<Mat>();
            Core.split(BaseMat, Channels);
            Ar = Channels.get(3).clone();

            Rt = new Thread(new Runnable() {
                @Override
                public void run() {
                    Rr = engine.quasi_poisson_solver_test3(Channels.get(0), SrcMat, MskMat, 0, offset, Roi);
                    r1 = true;
                    MainHandler.sendEmptyMessage(AppInfo.DURING);
                    Channels.get(0).release();
                }
            });
            Gt = new Thread(new Runnable() {
                @Override
                public void run() {
                    Gr = engine.quasi_poisson_solver_test3(Channels.get(1), SrcMat, MskMat, 1, offset, Roi);
                    r2 = true;
                    MainHandler.sendEmptyMessage(AppInfo.DURING);
                    Channels.get(1).release();
                }
            });
            Bt = new Thread(new Runnable() {
                @Override
                public void run() {
                    Br = engine.quasi_poisson_solver_test3(Channels.get(2), SrcMat, MskMat, 2, offset, Roi);
                    r3 = true;
                    MainHandler.sendEmptyMessage(AppInfo.DURING);
                    Channels.get(2).release();
                }
            });
            Rt.start();
            Gt.start();
            Bt.start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!(r1 && r2 && r3)){
                        try {
                            Cpv.setValueAnimated(AppInfo.CompleteCalc, 1000);
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    ArrayList<Mat> RChannels = new ArrayList<Mat>();

                    RChannels.add(Rr);
                    RChannels.add(Gr);
                    RChannels.add(Br);
                    RChannels.add(Ar);
                    try {
                        Core.merge(RChannels, ResultMat);
                        ResBmp = Bitmap.createBitmap(BaseBmp.getWidth(), BaseBmp.getHeight(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(ResultMat, ResBmp);
                        MainHandler.sendEmptyMessage(AppInfo.DONE);
                        Rr.release(); Rr = null;
                        Gr.release(); Gr = null;
                        Br.release(); Br = null;
                        Ar.release(); Ar = null;
                        ResultMat.release();
                        BaseMat.release();
                        ResultMat = null;
                        BaseMat = null;
                    }
                    catch (Exception e){

                    }
                    Cpv.setValue(0);
                    AppInfo.CompleteCalc = 0;
                }
            }).start();
            if (Channels.size() > 3)
                Channels.get(3).release();
        }
    };

    // Handler
    Handler MainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AppInfo.NONE:
                    LogoIV.setVisibility(View.VISIBLE);
                    BaseIV.setImageBitmap(null);
                    AttachIV.setVisibility(View.GONE);
                    ActBtn.setVisibility(View.GONE);
                    r1 = false;
                    r2 = false;
                    r3 = false;
                    break;
                case AppInfo.BASE:
                    break;
                case AppInfo.ATTACHED:
                    break;
                case AppInfo.DURING:
                    break;
                case AppInfo.DONE:
                    BaseIV.setImageBitmap(ResBmp);
                    AttachIV.setVisibility(View.GONE);
                    Cpv.setVisibility(View.GONE);
                    ActBtn.setText("저장");
                    ActBtn.setOnClickListener(doneBtnClickListener);
                    mNestedSV.setNestedScrollingEnabled(true);
                    r1 = false;
                    r2 = false;
                    r3 = false;
                    BaseBmp.recycle();
                    SourceBmp.recycle();
                    MaskBmp.recycle();
                    MaskedBmp.recycle();
                    BaseBmp = null;
                    SourceBmp = null;
                    MaskBmp = null;
                    MaskedBmp = null;
                    SrcMat.release();
                    MskMat.release();
                    SrcMat = null;
                    MskMat = null;
                    Toast.makeText(MainActivity.this, "완료", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            readPermission = false;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }

        __init__();
    }

    private void __init__() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setLogo(R.drawable.logo);
        setSupportActionBar(toolbar);

        // 아이디
        Abl = (AppBarLayout) findViewById(R.id.app_bar);            // AppBarLayout
        ImageFrame = (RelativeLayout) findViewById(R.id.containerimgframe);
        LogoIV = (ImageView) findViewById(R.id.mainlogoIV);
        ActBtn = (Button) findViewById(R.id.actbutton);
        Cpv = (CircleProgressView) findViewById(R.id.mainCPV);
        BaseIV = (ImageView) findViewById(R.id.mainBaseIV);
        AttachIV = (ImageView) findViewById(R.id.mainAttachIV);
        AttachLayoutParams = (RelativeLayout.LayoutParams) AttachIV.getLayoutParams();

        if(readPermission) {
            // 갤러리의 이미지 패스 가져오기
            ImgList = getAll_ImagesPath(MainActivity.this);
            Gallery g = (Gallery) findViewById(R.id.mainGallery);
            g.setAdapter(new GalleryAdapter(MainActivity.this, ImgList));
            g.setOnItemSelectedListener(galleryItemSelectedListener);
            g.setOnItemClickListener(galleryItemClickListener);
            g.setSpacing(10);

            ActBtn.setVisibility(View.GONE);
        }

        mNestedSV = (NestedScrollView) findViewById(R.id.nestedscrollview);

        AppInfo.MaxCalc = Engine.LOOP_MAX * 3;
        Cpv.setAutoTextSize(true);
        Cpv.setUnitVisible(true);
        Cpv.setUnit("%");
        Cpv.setTextMode(TextMode.PERCENT);
        Cpv.setBlockCount(10);
        Cpv.setUnitPosition(UnitPosition.RIGHT_BOTTOM);
        Cpv.setMaxValue(AppInfo.MaxCalc);
        Cpv.setBackgroundColor(Color.WHITE);
        Cpv.setValue(0);

        engine = new Engine(Cpv);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case AppInfo.GALLERY_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    try {
                        BaseBmp = Rescaled.ReSizeMax(MediaStore.Images.Media.getBitmap(getContentResolver(), uri), 680);       // 선택 이미지 리스케일
                        LogoIV.setVisibility(View.GONE);
                        Abl.setExpanded(false);                 // 앱바 숨기기

                        LayoutScale.ratioScaleFitWIdth(ImageFrame, BaseBmp.getWidth(), BaseBmp.getHeight());    // 이미지 비율에 따라 프레임 크기 조절
                        MainRatio = (float) BaseBmp.getWidth() / (float) ImageFrame.getWidth();
                        BaseIV.setImageBitmap(BaseBmp);             // 리스케일 이미지 삽입

                        ActBtn.setVisibility(View.VISIBLE);
                        ActBtn.setText("Target");
                        ActBtn.setOnClickListener(baseBtnClickListener);

                        File Folder = new File(AppInfo.SdcardDir + "/" + AppInfo.AppFolder);
                        if (!Folder.exists())
                            Folder.mkdirs();
                        File BBmp = new File(Folder.getPath(), "base.jpg");
                        BaseBmp.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(BBmp));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case AppInfo.ATTACH_REQUEST:
                if (resultCode == RESULT_OK) {
                    BitmapFactory.Options op = new BitmapFactory.Options();
                    op.inMutable = true;
                    op.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    SourceBmp = BitmapFactory.decodeFile(AppInfo.SdcardDir + "/" + AppInfo.AppFolder + "/target.jpg", op);
                    MaskBmp = BitmapFactory.decodeFile(AppInfo.SdcardDir + "/" + AppInfo.AppFolder + "/mask.jpg", op);
                    MaskedBmp = Rescaled.ComputeBmpNMsk(SourceBmp, MaskBmp);
                    if (MaskBmp == null) {
                        Toast.makeText(MainActivity.this, "실패 ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Roi = new Rect(data.getExtras().getDoubleArray("ROI"));
                    int[] Pos = data.getExtras().getIntArray("POS");
                    float Sca = data.getExtras().getFloat("SCA");
//                    float Rat = data.getExtras().getFloat("RAT");

                    MaskedBmp = Rescaled.ScalePerBitmap(MaskedBmp, Sca / MainRatio);
                    SourceBmp = Rescaled.ScalePerBitmap(SourceBmp, Sca);
                    MaskBmp = Rescaled.ScalePerBitmap(MaskBmp, Sca);
                    Roi.x *= Sca;
                    if (Roi.x < 0) Roi.x = 0;
                    Roi.y *= Sca;
                    if (Roi.y < 0) Roi.y = 0;
                    Roi.width *= Sca;
                    if (Roi.width > MaskBmp.getWidth()) Roi.width = MaskedBmp.getWidth();
                    Roi.height *= Sca;
                    if (Roi.height > MaskBmp.getHeight()) Roi.height = MaskedBmp.getHeight();

                    AttachIV.setVisibility(View.VISIBLE);
                    AttachLayoutParams.leftMargin = Pos[0];
                    AttachLayoutParams.topMargin = Pos[1];
                    AttachLayoutParams.width = MaskedBmp.getWidth();
                    AttachLayoutParams.height = MaskedBmp.getHeight();
                    AttachIV.setLayoutParams(AttachLayoutParams);

                    AttachIV.setImageBitmap(MaskedBmp);

                    ActBtn.setVisibility(View.VISIBLE);
                    ActBtn.setText("Blend");
                    ActBtn.setOnClickListener(attachedBtnClickListener);

                    Abl.setExpanded(false);
                    mNestedSV.setNestedScrollingEnabled(false);

                    MainHandler.sendEmptyMessage(AppInfo.ATTACHED);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbarmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newtaskitem:
                LogoIV.setVisibility(View.VISIBLE);
                LayoutScale.ratioScaleFitWIdth(ImageFrame, LogoIV.getWidth(), LogoIV.getHeight());
                BaseIV.setImageBitmap(null);
                AttachIV.setVisibility(View.GONE);
                ActBtn.setVisibility(View.GONE);
                mNestedSV.setNestedScrollingEnabled(true);
                r1 = false;
                r2 = false;
                r3 = false;
                deletetmp();
                break;
            case R.id.callgalleryitem:
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, AppInfo.GALLERY_REQUEST);
                break;
            case R.id.resetitem:
                AttachIV.setVisibility(View.GONE);
                ActBtn.setVisibility(View.VISIBLE);
                ActBtn.setText("Target");
                ActBtn.setOnClickListener(baseBtnClickListener);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        deletetmp();

        super.onDestroy();
    }

    private void deletetmp() {
        File Folder = new File(AppInfo.SdcardDir + "/" + AppInfo.AppFolder);
        if (Folder.exists()) {
            File AttBmp = new File(Folder.getPath(), "target.jpg");
            File MaskBmp = new File(Folder.getPath(), "mask.jpg");
            File BasBmp = new File(Folder.getPath(), "base.jpg");
            if (BasBmp.exists())
                BasBmp.delete();
            if (AttBmp.exists())
                AttBmp.delete();
            if (MaskBmp.exists())
                MaskBmp.delete();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    readPermission = true;
                break;
        }
    }

}
