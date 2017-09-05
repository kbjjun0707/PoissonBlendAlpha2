package com.example.jun.poissonblendalpha2.pb.Adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.example.jun.poissonblendalpha2.R;

import java.util.ArrayList;

/**
 * Created by Jun on 2016-11-08.
 */

public class GalleryAdapter extends BaseAdapter {
    ArrayList<String> mList;
    Activity mAct;
    LayoutInflater inflater;

    public GalleryAdapter(Activity act, ArrayList<String> list){
        mAct = act;
        mList = list;
        inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Bitmap getItem(int i) {
        return BitmapFactory.decodeFile(mList.get(i));
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null)
            view = inflater.inflate(R.layout.gallery_adapter, viewGroup, false);

        ImageView iv = (ImageView)view.findViewById(R.id.galleryIV);
        iv.setImageBitmap(getItem(i));

        return view;
    }
}
