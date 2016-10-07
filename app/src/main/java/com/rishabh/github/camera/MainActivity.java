package com.rishabh.github.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements MediaRecorder.OnInfoListener{

    private static final String DEBUG_TAG = "Gestures";
    MarshMellowPermissions marshMellowPermissions;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    private boolean isRecording = false;
//    private GestureDetectorCompat mDetector;
    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private CameraPreview mPreview;
    private Button captureButton,videoButton,stop;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {


            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d("tag", "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                if(reduceBitmap(pictureFile)) {
                    Toast.makeText(getApplicationContext(), "" + pictureFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("tag", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("tag", "Error accessing file: " + e.getMessage());
            }
        }
    };

    FrameLayout frameLayout;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        marshMellowPermissions=new MarshMellowPermissions(this);
        frameLayout=(FrameLayout)findViewById(R.id.camera_preview);
//        mDetector = new GestureDetectorCompat(getApplicationContext(),this);
        Log.i("tag", "onCreate");
        stop=(Button)findViewById(R.id.stop);
//        mDetector.setOnDoubleTapListener(this);
        if (!marshMellowPermissions.checkPermissionForCamera()) {
            marshMellowPermissions.requestPermissionForCamera();
        } else if (!marshMellowPermissions.checkPermissionForExternalStorage()) {
                marshMellowPermissions.requestPermissionForExternalStorage();
            }
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickPicture();
            }
        });
        frameLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                clickVideo();
                return true;
            }
        });

// Add a listener to the Capture button
        captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        clickPicture();
                    }
                }
        );

        // Add a listener to the Capture button
        videoButton = (Button) findViewById(R.id.video_capture);
        videoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickVideo();
                                            }
                }
        );


/**
 * check to see whether the camera is present in t-he phone!
 *
 if(checkCameraHardware(this)){
 Log.i("tag","hardware present");
 Log.i("tag",""+android.hardware.Camera.getNumberOfCameras());

 }else{
 Log.i("tag","hardware not present");
 }

 **/
    }

    private void clickVideo() {
        if (isRecording) {
             stopRecording();
        } else {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();
                stop.setVisibility(View.VISIBLE);
                captureButton.setVisibility(View.GONE);
                videoButton.setVisibility(View.GONE);
                // inform the user that recording has started
                //setCaptureButtonText("Stop");
                Toast.makeText(getApplicationContext(), "Recording started!", Toast.LENGTH_LONG).show();

                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                // inform user
            }
        }

    }

    private void stopRecording() {
        // stop recording and release camera
        mMediaRecorder.stop();  // stop the recording
        releaseMediaRecorder(); // release the MediaRecorder object
        mCamera.lock();         // take camera access back from MediaRecorder

        // inform the user that recording has stopped
        //  setCaptureButtonText("Capture");
        Toast.makeText(getApplicationContext(), "Stopped!", Toast.LENGTH_LONG).show();
        isRecording = false;

        stop.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
        videoButton.setVisibility(View.VISIBLE);
        //to start the camera again
        //  mCamera.stopPreview();
        mCamera.startPreview();
    }

    public void stop(View v){
        stopRecording();
    }

    private void clickPicture() {
        // get an image from the camera
        mCamera.takePicture(null, null, mPicture);
        Toast.makeText(getApplicationContext(),"touch to continue",Toast.LENGTH_SHORT).show();
        mCamera.startPreview();
    }


    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * method to get the camera instance
     **/
    public static android.hardware.Camera getCameraInstance() {
        android.hardware.Camera camera = null;

        try {

            camera = android.hardware.Camera.open();
            //open specific camera using open(int)
            Camera.Parameters parameters = camera.getParameters();


        } catch (Exception e) {
            Log.i("tag", "camera not present");
        }
        return camera;
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpeg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    protected void onPause() {
        super.onPause();
       /* //releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
        releaseMediaRecorder();
        // two new lines for removing the old CameraPreview
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.removeView(mPreview);
        */

        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            frameLayout.removeView(mPreview);
            mCamera=null;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCamera == null)
        {
            mCamera = getCameraInstance();
            // three new lines, creating a new CameraPreview, then adding it to the FrameLayout
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }


    private boolean prepareVideoRecorder() {


        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            //time duration of the vid
            mMediaRecorder.setMaxDuration(30000);
            mMediaRecorder.setOnInfoListener(this);
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("tag", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("tag", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }



    public boolean reduceBitmap(File path) {
        int scaleFactor=2;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        /* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

    /* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(path.getAbsolutePath(), bmOptions);

        if (bitmap != null) {
    /* Test compress */
            File imageFile = path;
            try {
                OutputStream out = null;
                out = new FileOutputStream(imageFile);
                //Bitmap bitmap = BitmapFactory.decodeFile(picturePath);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                return true;
            } catch (Exception e) {
                Log.e("Dak", "Erreur compress : " + e.toString());
            }
        }
            return false;

    }

    @Override
    public void onInfo(MediaRecorder mediaRecorder, int i, int i1) {
        Log.i("tag","inside onInfo()"+i);
        if (i == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.v("VIDEOCAPTURE","Maximum Duration Reached");
            Toast.makeText(getApplicationContext(),"Max TIme:30 sec exceeded!",Toast.LENGTH_LONG).show();
           // mediaRecorder.stop();
            stopRecording();

        }
    }

}