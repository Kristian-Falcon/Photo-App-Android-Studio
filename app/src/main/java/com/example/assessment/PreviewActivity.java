package com.example.assessment;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;

public class PreviewActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView preview;
    Bitmap previewBM;
    String filePath;
    File file;
    ImageButton delete;
    AlertDialog.Builder deleteWarning;
    //Source for alert dialog: https://developer.android.com/guide/topics/ui/dialogs


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //These two lines hides the notification bar at the top of the screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_preview);

        preview = (ImageView)findViewById(R.id.iv_preview);
        filePath = getIntent().getStringExtra("Picture");
        file = new File(filePath);
        previewBM = BitmapFactory.decodeFile(file.getAbsolutePath());
        preview.setImageBitmap(previewBM);

        delete = (ImageButton) findViewById(R.id.btn_delete);
        delete.setOnClickListener(this);

        deleteWarning = new AlertDialog.Builder(PreviewActivity.this);
        deleteWarning.setTitle("Delete Image");
        deleteWarning.setMessage("Are you sure you want to delete this image?");
        deleteWarning.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deletePhoto();
            }
        });
        deleteWarning.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_delete:
                deleteWarning.show();
                break;
        }
    }

    void deletePhoto(){
        file.delete();
        finish();
    }
}
