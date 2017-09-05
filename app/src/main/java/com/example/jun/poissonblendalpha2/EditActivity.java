package com.example.jun.poissonblendalpha2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.jun.poissonblendalpha2.pb.Func.Rescaled;
import com.example.jun.poissonblendalpha2.pb.Info.AppInfo;
import com.example.jun.poissonblendalpha2.pb.View.MaskView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Jun on 2016-11-10.
 */

public class EditActivity extends Activity implements View.OnClickListener {
    private boolean Close = false, Loaded = false;
    private ImageButton RefreshBtn, BrushBtn, EraseBtn, OkBtn, BackBtn, NextBtn;
    private MaskView Maskview;
    private RelativeLayout LocateRL;
    private RelativeLayout.LayoutParams LocateLP;
    private ImageView AttachIV, BaseIV, MaskedIV;
    private float mRatio = 1.0f;

    /// Handler
    private final int HANDLER_BITMAP_COMPLETE = 6101;
    private final int HANDLER_FAIL = 6100;
    public Handler editHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_BITMAP_COMPLETE:
                    AttachIV.setImageBitmap(attachbmp);
                    Maskview.setImageBitmap(maskbmp);

                    RefreshBtn.setVisibility(View.VISIBLE);
                    BrushBtn.setVisibility(View.VISIBLE);
                    EraseBtn.setVisibility(View.VISIBLE);
                    OkBtn.setVisibility(View.INVISIBLE);
                    break;
                case HANDLER_FAIL:
                    ((ImageView) findViewById(R.id.attachIV)).setImageBitmap(attachbmp);
                    break;
            }
        }
    };

    private Bitmap attachbmp, maskbmp, basebmp, maskedbmp, tmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_edit);

        __init__();
    }

    private void __init__() {
        findViewById(R.id.loadBtn).setOnClickListener(this);
        RefreshBtn = (ImageButton) findViewById(R.id.newBtn);
        BrushBtn = (ImageButton) findViewById(R.id.drawBtn);
        EraseBtn = (ImageButton) findViewById(R.id.eraseBtn);
        OkBtn = (ImageButton) findViewById(R.id.okBtn);
        BackBtn = (ImageButton) findViewById(R.id.backBtn);
        NextBtn = (ImageButton) findViewById(R.id.nextBtn);
        findViewById(R.id.cancelBtn).setOnClickListener(this);
        Maskview = (MaskView) findViewById(R.id.attachMV);
        AttachIV = (ImageView) findViewById(R.id.attachIV);
        BaseIV = (ImageView) findViewById(R.id.attachIV2);
        MaskedIV = (ImageView) findViewById(R.id.attachIV3);
        LocateRL = (RelativeLayout) findViewById(R.id.attachRL);
        LocateLP = (RelativeLayout.LayoutParams) MaskedIV.getLayoutParams();

        RefreshBtn.setOnClickListener(this);
        BrushBtn.setOnClickListener(this);
        EraseBtn.setOnClickListener(this);
        OkBtn.setOnClickListener(this);
        BackBtn.setOnClickListener(this);
        NextBtn.setOnClickListener(this);

        RefreshBtn.setVisibility(View.GONE);
        BrushBtn.setVisibility(View.GONE);
        EraseBtn.setVisibility(View.GONE);
        OkBtn.setVisibility(View.GONE);
        LocateRL.setVisibility(View.INVISIBLE);

        EraseBtn.setBackgroundResource(R.drawable.selected);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case AppInfo.GALLERY_REQUEST:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                attachbmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                                attachbmp = Rescaled.ReSizeMax(attachbmp, 680);
                                maskbmp = Bitmap.createBitmap(attachbmp.getWidth(), attachbmp.getHeight(), Bitmap.Config.ARGB_8888);
                                editHandler.sendEmptyMessage(HANDLER_BITMAP_COMPLETE);
                                Loaded = true;
                            } catch (IOException e) {
                                attachbmp = null;
                                maskbmp = null;
                                editHandler.sendEmptyMessage(HANDLER_FAIL);
                                Loaded = false;
                                Toast.makeText(EditActivity.this, "불러오기 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).start();
                } else {
                    Toast.makeText(EditActivity.this, "이미지 불러오기 취소", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    float VerRate = 1;

    @Override
    public void onClick(View view) {
        Intent i;
        switch (view.getId()) {
            case R.id.loadBtn:
                i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, AppInfo.GALLERY_REQUEST);
                break;
            case R.id.drawBtn:
                Maskview.setBrush();
                NextBtn.setVisibility(View.VISIBLE);
                OkBtn.setVisibility(View.VISIBLE);
                BrushBtn.setBackgroundResource(R.drawable.selected);
                EraseBtn.setBackgroundResource(R.drawable.whitebox);
                break;
            case R.id.eraseBtn:
                Maskview.setEraser();
                BrushBtn.setBackgroundResource(R.drawable.whitebox);
                EraseBtn.setBackgroundResource(R.drawable.selected);
                break;
            case R.id.okBtn:
                if (Loaded) {
                    File Folder = new File(AppInfo.SdcardDir + "/" + AppInfo.AppFolder);
                    if (!Folder.exists())
                        Folder.mkdirs();
                    File AttBmp = new File(Folder.getPath(), "target.jpg");
                    File MaskBmp = new File(Folder.getPath(), "mask.jpg");
                    try {
                        attachbmp.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(AttBmp));
                        maskbmp.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(MaskBmp));
                        attachbmp.recycle();
                        maskbmp.recycle();
                        Intent intent = new Intent();
                        intent.putExtra("ROI", Maskview.getBound());
                        intent.putExtra("POS", new int[]{LocateLP.leftMargin, LocateLP.topMargin});
                        intent.putExtra("SCA", attachscale);
                        intent.putExtra("RAT", VerRate);
                        setResult(RESULT_OK, intent);
                        finish();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        setResult(RESULT_CANCELED);
                        Toast.makeText(EditActivity.this, "마스크 실패", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.newBtn:
                if (maskbmp != null)
                    maskbmp.recycle();
                maskbmp = Bitmap.createBitmap(attachbmp.getWidth(), attachbmp.getHeight(), Bitmap.Config.ARGB_8888);
                Maskview.setImageBitmap(maskbmp);
                break;
            case R.id.cancelBtn:
                if (attachbmp != null)
                    attachbmp.recycle();
                if (maskbmp != null)
                    maskbmp.recycle();
                setResult(RESULT_CANCELED);
                finish();
                break;
            case R.id.nextBtn:
                BitmapFactory.Options op = new BitmapFactory.Options();
                op.inMutable = true;
                op.inPreferredConfig = Bitmap.Config.ARGB_8888;
                basebmp = BitmapFactory.decodeFile(AppInfo.SdcardDir + "/" + AppInfo.AppFolder + "/base.jpg", op);
                maskedbmp = Rescaled.ComputeBmpNMsk(attachbmp, maskbmp);
                AttachIV.setVisibility(View.INVISIBLE);
                Maskview.setVisibility(View.INVISIBLE);
                LocateRL.setVisibility(View.VISIBLE);
                BaseIV.setImageBitmap(basebmp);

                float viewRate = (float) BaseIV.getWidth() / BaseIV.getHeight();
                float imgRate = (float) basebmp.getWidth() / basebmp.getHeight();

                int reSizeX = basebmp.getWidth();
                if (viewRate >= imgRate) {                                           // 이미지가 세로로 긴 것
                    VerRate = (float) BaseIV.getHeight() / basebmp.getHeight();
                    reSizeX = (int)(reSizeX * VerRate);
                } else {
                    VerRate = 1;
                    reSizeX = BaseIV.getWidth();
                }
                mRatio = (float) reSizeX / basebmp.getWidth();
                maskedbmp = Rescaled.ScalePerBitmap(maskedbmp, mRatio);
                tmp = maskedbmp.copy(Bitmap.Config.ARGB_8888, true);

                LocateLP.leftMargin = 0;
                LocateLP.topMargin = 0;
                LocateLP.width = maskedbmp.getWidth();
                LocateLP.height = maskedbmp.getHeight();
                MaskedIV.setLayoutParams(LocateLP);
                MaskedIV.setImageBitmap(maskedbmp);
                BackBtn.setVisibility(View.VISIBLE);
                NextBtn.setVisibility(View.INVISIBLE);
                break;
            case R.id.backBtn:
                AttachIV.setVisibility(View.VISIBLE);
                Maskview.setVisibility(View.VISIBLE);
                LocateRL.setVisibility(View.GONE);
                BackBtn.setVisibility(View.INVISIBLE);
                NextBtn.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!Close) {
            Close = true;
            Toast.makeText(EditActivity.this, "뒤로 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                        Close = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }

    // 드래그 모드인지 핀치줌 모드인지 구분
    final int NONE = 0;
    final int DRAG = 1;
    final int ZOOM = 2;
    int mode = NONE;

    // 드래그시 좌표 저장
    int posX1 = 0, posX2 = 0, posY1 = 0, posY2 = 0;
    float attachscale = 1.0f;

    // 핀치시 두좌표간의 거리 저장
    float oldDist = 1f;
    float newDist = 1f;

    public boolean onTouchEvent(MotionEvent event) {
        int act = event.getAction();
        String strMsg = "";

        switch (act & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:    //첫번째 손가락 터치(드래그 용도)
                posX1 = (int) event.getX();
                posY1 = (int) event.getY();

                mode = DRAG;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {  // 드래그 중
                    posX2 = (int) event.getX();
                    posY2 = (int) event.getY();

                    if (Math.abs(posX2 - posX1) > 4 || Math.abs(posY2 - posY1) > 4) {
                        LocateLP.leftMargin += posX2 - posX1;
                        LocateLP.topMargin += posY2 - posY1;
                        MaskedIV.setLayoutParams(LocateLP);
                        posX1 = posX2;
                        posY1 = posY2;
                    }
                } else if (mode == ZOOM) {    // 핀치 중
                    newDist = spacing(event);
                    MaskedIV.setPivotX(0);
                    MaskedIV.setPivotY(0);

                    boolean pass = true;
                    if (newDist - oldDist > 13) { // zoom in
                        pass = false;
                        attachscale *= 1.02;
                        oldDist = newDist;
                    } else if (oldDist - newDist > 13) { // zoom out
                        pass = false;
                        attachscale *= 1 / 1.02;
                        oldDist = newDist;
                    }
                    if(!pass) {
                        tmp.recycle();
                        tmp = Rescaled.ScalePerBitmap(maskedbmp, attachscale);
                        RelativeLayout.LayoutParams Rlp = (RelativeLayout.LayoutParams)MaskedIV.getLayoutParams();
                        Rlp.width = tmp.getWidth();
                        Rlp.height = tmp.getHeight();
                        MaskedIV.setLayoutParams(Rlp);
                        MaskedIV.setImageBitmap(tmp);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:    // 첫번째 손가락을 떼었을 경우
            case MotionEvent.ACTION_POINTER_UP:  // 두번째 손가락을 떼었을 경우
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //두번째 손가락 터치(손가락 2개를 인식하였기 때문에 핀치 줌으로 판별)
                mode = ZOOM;

                newDist = spacing(event);
                oldDist = spacing(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            default:
                break;
        }

        return super.onTouchEvent(event);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}
