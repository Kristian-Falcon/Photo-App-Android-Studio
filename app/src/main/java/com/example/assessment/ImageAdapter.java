package com.example.assessment;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;

public class ImageAdapter extends BaseAdapter {
    private Context context;
    private Bitmap[] thumbs;

    public ImageAdapter(Context c, Bitmap[] b){
        context = c;
        thumbs = b;
    }

    @Override
    public int getCount() {
        return thumbs.length;
    }

    @Override
    public Object getItem(int position) {
        // This is where we normally return the data object.
        // since we don't use objects to store images, return drawable id (int)
        Log.w("ADAPTER", "Returning ID at position: " + position);
        return thumbs[position];
    }

    @Override
    public long getItemId(int position) {
        // not used in this example
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // If view is not recycled, (re)initialise it
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(285, 285));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(4, 4, 4, 4);
        } else {
            // otherwise, use the original
            imageView = (ImageView) convertView;
        }
        imageView.setImageBitmap(this.thumbs[position]);
        return imageView;
    }
}
