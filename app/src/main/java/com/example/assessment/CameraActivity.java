//This activity is the actual main activity where the camera can take pictures as well as access the gallery
//Main package summery for reference: https://developer.android.com/reference/android/hardware/camera2/package-summary.html
//Also a useful source: https://www.programcreek.com/java-api-examples/?api=android.hardware.camera2.CameraCaptureSession
package com.example.assessment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;  //Permission Request codes
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;

    boolean frontFacing;  //Whether or not it is the front-facing camera
    int mTotalRotation;  //Used to get rotation of the phone

    ImageButton b_photos;    //Used to open photo gallery
    ImageButton b_capture;   //Used to capture an image
    ImageButton b_flip;      //Used to toggle forward-facing camera

    File ImageFolder;  //Folder to hold the images
    static String ImageFilePath;  //The complete path to the file

    HandlerThread mHandlerThread;  //Handlers for optimization
    Handler mHandler;
    String mCameraID;  //String needed for CameraManager
    Size mPreviewSize;  //Sizes determine dimensions of the preview and images
    Size mImageSize;
    ImageReader mImageReader; //Allows access to image data created by the texture view
    //Source: https://developer.android.com/reference/android/media/ImageReader
    final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            mHandler.post(new ImageSaver(reader.acquireLatestImage()));
        }
    };

    TextureView mTextureView;   //Texture view used to display images
    //Texture views have listeners as well to see if a TV is available
    TextureView.SurfaceTextureListener mSurTexLis = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setUpCamera(width, height);
            fixImage(width, height);
            connectCamera();
        }
        //The rest of the methods are not needed but without them the SurfaceTextureListener will claim a syntax error
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    CameraCaptureSession mPreviewCaptureSession;  //Used to capture images from the camera, provides set of target output surfaces
    //Source: https://developer.android.com/reference/android/hardware/camera2/CameraCaptureSession
    CaptureRequest.Builder mCapReqBuild;  //Needed to display preview. See startPreview() method for details
    //Source: https://developer.android.com/reference/android/hardware/camera2/CaptureRequest.Builder

    CameraDevice mCameraDevice;  //Camera Devices classes used to represent a camera
    //Source: https://developer.android.com/reference/android/hardware/camera2/CameraDevice
    CameraDevice.StateCallback mCamDevSCall = new CameraDevice.StateCallback() {  //Nested class - References the state of the camera
    //Source: https://developer.android.com/reference/android/hardware/camera2/CameraDevice.StateCallback.html
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    //Subclass used to save images
    private class ImageSaver implements Runnable {
        private final Image mImage;
        private ImageSaver(Image image) {
            mImage = image;
        }
        @Override
        public void run() {
            //Saves images via a bytebuffer
            //Java API: https://docs.oracle.com/javase/7/docs/api/java/nio/ByteBuffer.html
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();  //BB holds all image data
            byte[] bytes = new byte[byteBuffer.remaining()];  //Determines byte array size
            byteBuffer.get(bytes);  //Fills array of bytes with the data for the image
            //File output taken from previous labs
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(ImageFilePath);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //Closes image and FOS when finished
                mImage.close();
                if(fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //Determines camera orientation
    //Taken from official camera2 sample from google: https://github.com/googlesamples/android-Camera2Basic/pull/27/files
     SparseIntArray PHONE_ORIENTATIONS = new SparseIntArray();
     {
        PHONE_ORIENTATIONS.append(Surface.ROTATION_0, 0);
        PHONE_ORIENTATIONS.append(Surface.ROTATION_90, 90);
        PHONE_ORIENTATIONS.append(Surface.ROTATION_180, 180);
        PHONE_ORIENTATIONS.append(Surface.ROTATION_270, 270);
     }

    //Class is used to compare size of the pool of resolutions
    private static class CompareSize implements Comparator<Size> {
        @Override
        public int compare(Size leftSide, Size rightSide) {
            return Long.signum((long) (leftSide.getWidth() * leftSide.getHeight()) /
                    (long) (rightSide.getWidth() * rightSide.getHeight()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //These two lines hides the notification bar at the top of the screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        frontFacing = false;

        b_photos = (ImageButton) findViewById(R.id.btn_photos);
        b_photos.setOnClickListener(this);
        b_capture = (ImageButton) findViewById(R.id.btn_capture);
        b_capture.setOnClickListener(this);
        b_flip = (ImageButton) findViewById(R.id.btn_flip);
        b_flip.setOnClickListener(this);

        mTextureView = (TextureView) findViewById(R.id.tv_preview);

        createPictureFolder();
    }

    @Override
    protected void onPause() {
        closeCamera();//done before super constructor, otherwise, device will pause before anything will happen
        stopBackgroundThread();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            setUpCamera(mTextureView.getWidth(), mTextureView.getHeight());
            fixImage(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurTexLis);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_capture:
                //captures
                checkWriteStoragePermission();
                break;
            case R.id.btn_flip:
                //flips camera
                if(frontFacing)
                    frontFacing = false;
                else if(!frontFacing)
                    frontFacing = true;

                closeCamera();
                if (mTextureView.isAvailable()) {
                    setUpCamera(mTextureView.getWidth(), mTextureView.getHeight());
                    fixImage(mTextureView.getWidth(), mTextureView.getHeight());
                    connectCamera();
                } else {
                    mTextureView.setSurfaceTextureListener(mSurTexLis);
                }
                break;
            case R.id.btn_photos:
                //Goes to photo gallery
                Intent goToPhotosIntent = new Intent(CameraActivity.this, PhotoActivity.class);
                startActivity(goToPhotosIntent);
                break;
        }
    }

    void setUpCamera(int width, int height) {
        CameraManager CM = (CameraManager) getSystemService(Context.CAMERA_SERVICE); //Retrieves the appropriate CameraManager for the service
        //Camera manager is "A system service manager for detecting, characterizing, and connecting to CameraDevice"
        //Source: https://developer.android.com/reference/android/content/Context.html#CAMERA_SERVICE
        try {//Try-catch is here because it was yelling at me to put it here
            for (String cameraID : CM.getCameraIdList()) {  //Cycles through each cameraID
                CameraCharacteristics CC = CM.getCameraCharacteristics(cameraID);  //CameraCharacteristics get properties to describe the camera device
                //Source: https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics
                if ((CC.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT && !frontFacing) || (CC.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_FRONT && frontFacing)) {
                    //If front-facing and cc == front-facing, skip non-front-facing camera and vice versa
                    continue;
                }
                StreamConfigurationMap SCM = CC.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);  //Gets list of all "output formats" (resolutions)
                //Source: https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap

                //Rotates the view when the device rotates
                int phoneOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = phoneRotation(CC, phoneOrientation); //uses method below
                boolean changeRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (changeRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                //Determines size of the preview and image
                mPreviewSize = selectSize(SCM.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight); //see method below
                mImageSize = selectSize(SCM.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
                mCameraID = cameraID;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Connects to camera - must always be called after setUpCamera
    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==PackageManager.PERMISSION_GRANTED) { // If permissions are available
                cameraManager.openCamera(mCameraID, mCamDevSCall, mHandler); //Takes CameraID, State callback and background handler to open the camera
                //Source: https://developer.android.com/reference/android/hardware/camera2/CameraManager#openCamera(java.lang.String,%2520android.hardware.camera2.CameraDevice.StateCallback,%2520android.os.Handler)
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Please go enable camera permissions here or in App Settings", Toast.LENGTH_SHORT).show();
                }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Starts the preview - must always be called after
    private void startPreview(){
        SurfaceTexture ST = mTextureView.getSurfaceTexture();  //Turns texture into surface texture
        ST.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());  //sets surface texture's height and width
        Surface PreviewSurface = new Surface(ST);  //sets surface texture to surface

        try {
            mCapReqBuild = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);  //defines camera request builder
            mCapReqBuild.addTarget(PreviewSurface);  //sets the image of the texture view to be the camera
            //Source: https://developer.android.com/reference/android/hardware/camera2/CaptureRequest.Builder#addTarget(android.view.Surface)

            mCameraDevice.createCaptureSession(Arrays.asList(PreviewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            //Source: https://developer.android.com/reference/android/hardware/camera2/CameraDevice.html#createCaptureSession(android.hardware.camera2.params.SessionConfiguration)
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewCaptureSession = session;
                    try {
                        mPreviewCaptureSession.setRepeatingRequest(mCapReqBuild.build(), null, mHandler);   //Constantly updates preview
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Configuration Failed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread(){
        mHandlerThread = new HandlerThread("CameraActivity");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopBackgroundThread(){
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Uses sparseintarray above to get rotation
    private int phoneRotation(CameraCharacteristics CC, int phoneOrientation){
        int sensorOrientation = CC.get(CC.SENSOR_ORIENTATION);
        phoneOrientation = PHONE_ORIENTATIONS.get(phoneOrientation);

        return(sensorOrientation + phoneOrientation + 360) % 360;
    }

    private static Size selectSize(Size[] sizes, int width, int height){
        List<Size> sizeThreshold = new ArrayList<Size>();
        for(Size s : sizes) {
            if (s.getHeight() == s.getWidth() * height / width && s.getWidth() >= width && s.getHeight() >= height) {
                sizeThreshold.add(s);
            }
        }

        if (sizeThreshold.size() > 0) {
            return Collections.min(sizeThreshold, new CompareSize());
        } else {
            Log.d("Assessment", "Couldn't find any suitable preview size");
            return sizes[0];
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_RESULT:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "App will not run without camera permissions", Toast.LENGTH_SHORT).show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
                }
                return;
            case REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getApplicationContext(), "App will not run without writing permissions", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void createPictureFolder(){
        File pictureFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        ImageFolder = new File(pictureFile, "Photo App");
        if(!ImageFolder.exists()){
            if(ImageFolder.mkdirs())
                Toast.makeText(CameraActivity.this, "Folder made! :D", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(CameraActivity.this, "Folder not made... :(", Toast.LENGTH_LONG).show();
        }
    }

    private void checkWriteStoragePermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startCaptureRequest();
        } else {
            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Please provide permission to save files.", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
        }
    }

    //creates a picture
    private void startCaptureRequest() {
        try {
            mCapReqBuild = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE); //create a request to capture whatever is previewed
            mCapReqBuild.addTarget(mImageReader.getSurface());  //capture request builder gets the image reader for the target surface
            mCapReqBuild.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);
            CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                            try {
                                createPictureFileName();
                                Toast.makeText(getApplicationContext(), "Picture Taken!", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
            mPreviewCaptureSession.capture(mCapReqBuild.build(), stillCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private File createPictureFileName() throws IOException{
        //Naming convention inspired from camera api page: https://developer.android.com/guide/topics/media/camera
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "IMAGE_" + timestamp + "_";
        File pictureFile = File.createTempFile(prepend, ".jpg", ImageFolder);
        ImageFilePath = pictureFile.getAbsolutePath();
        return pictureFile;
    }

    //Solution came from this tutorial: https://www.youtube.com/watch?v=YvS3iGKhQ_g
    private void fixImage(int width, int height) {
        if(mPreviewSize == null || mTextureView == null) {
            return;
        }
        Matrix matrix = new Matrix();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int rotationAmount = 0;
        RectF textureRectF = new RectF(0, 0, width, height);
        RectF previewRectF = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();
        if(frontFacing || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)) {
            //This if/else ladder was a slight modification made by me to fix the front-facing camera
            if(!frontFacing && (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)){
                rotationAmount = -2;
            } else if(frontFacing && !(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)){
                rotationAmount = 1;
            } else if(frontFacing && (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)){
                rotationAmount = 0;
            }
            previewRectF.offset(centerX - previewRectF.centerX(), centerY - previewRectF.centerY());
            matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float)width / mPreviewSize.getWidth(), (float)height / mPreviewSize.getHeight());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation + rotationAmount), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }
}