package com.example.assessment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.io.File;

public class PhotoActivity extends AppCompatActivity /*implements View.OnClickListener*/ {
    File pictureFolder;
    Bitmap[] BMList;

    GridView gv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle("Photos");

        gv = (GridView)findViewById(R.id.gr);

        setGridView();
    }

    @Override
    protected void onPause() {
        gv.setAdapter(null);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setGridView();
    }

    private void setGridView(){
        pictureFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Photo App");
        final File[] files = pictureFolder.listFiles();
        BMList = new Bitmap[files.length];

        for (int i = 0; i < files.length; i++)
        {
            BMList[i] = BitmapFactory.decodeFile(files[i].getAbsolutePath());
        }

        gv.setAdapter(new ImageAdapter(this, this.BMList));

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                String previewPath = files[position].toString();
                Intent goToPreview = new Intent(PhotoActivity.this, PreviewActivity.class);
                goToPreview.putExtra("Picture", previewPath);
                startActivity(goToPreview);
            }
        });
    }
}
