package com.android.qian.testfacialprocessing;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.qualcomm.snapdragon.sdk.face.FaceData;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing;

public class MainActivity extends AppCompatActivity
        implements Camera.PreviewCallback {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    Camera cameraObj;
    FrameLayout preview;
    private CameraSurfacePreview mPreview;
    private int FRONT_CAMERA_INDEX = 1;
    private int BACK_CAMERA_INDEX = 0;

    private boolean qcSDKEnable;
    FacialProcessing faceProc;

    Display display;
    private int displayAngle;

    private int numFaces;
    FaceData[] faceArray = null;

    DrawView drawView;

    private static boolean switchCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preview = (FrameLayout)findViewById(R.id.camera_preview);
        startCamera();

        Button switchCameraButton = (Button)findViewById(R.id.switchCamera);
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCamera();
                switchCamera = !switchCamera;
                startCamera();
            }
        });

        display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraObj != null) {
            stopCamera();
        }
        startCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        int dRotation = display.getRotation();
        FacialProcessing.PREVIEW_ROTATION_ANGLE angleEnum
                = FacialProcessing.PREVIEW_ROTATION_ANGLE.ROT_0;
        switch (dRotation) {
            case 0: // Not rotated
                displayAngle = 90;
                angleEnum = FacialProcessing.PREVIEW_ROTATION_ANGLE.ROT_90;
                break;
            case 1: // Landscape left
                displayAngle = 0;
                angleEnum = FacialProcessing.PREVIEW_ROTATION_ANGLE.ROT_0;
                break;
            case 2: // Upside down
                displayAngle = 270;
                angleEnum = FacialProcessing.PREVIEW_ROTATION_ANGLE.ROT_270;
                break;
            case 3: // Landscape right
                displayAngle = 180;
                angleEnum = FacialProcessing.PREVIEW_ROTATION_ANGLE.ROT_180;
                break;
        }
        cameraObj.setDisplayOrientation(displayAngle);

        if (qcSDKEnable) {
            if (faceProc == null) {
                faceProc = FacialProcessing.getInstance();
            }
            Camera.Parameters params = cameraObj.getParameters();
            Camera.Size previewSize = params.getPreviewSize();

            if (this.getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE && !switchCamera) {
                faceProc.setFrame(data, previewSize.width, previewSize.height,
                        true, angleEnum);
            } else if (this.getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE && switchCamera) {
                faceProc.setFrame(data, previewSize.width, previewSize.height,
                        false, angleEnum);
            } else if (this.getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT && !switchCamera) {
                faceProc.setFrame(data, previewSize.width, previewSize.height,
                        true, angleEnum);
            } else {
                faceProc.setFrame(data, previewSize.width, previewSize.height,
                        false, angleEnum);
            }
        }

        int surfaceWidth = mPreview.getWidth();
        int surfaceHeight = mPreview.getHeight();
        faceProc.normalizeCoordinates(surfaceWidth, surfaceHeight);

        numFaces = faceProc.getNumFaces();
        if (numFaces > 0) {
            Log.d(LOG_TAG, "Face Detected");
            faceArray = faceProc.getFaceData();
            preview.removeView(drawView);

            drawView = new DrawView(this, faceArray, true);
            preview.addView(drawView);
        } else {
            preview.removeView(drawView);
            drawView = new DrawView(this, null, false);
            preview.addView(drawView);
        }
    }

    private void startCamera() {
        qcSDKEnable = FacialProcessing.isFeatureSupported(
                FacialProcessing.FEATURE_LIST.FEATURE_FACIAL_PROCESSING);
        if (qcSDKEnable && faceProc == null) {
            Log.e(LOG_TAG, "Feature supported");
            faceProc = FacialProcessing.getInstance();
        } else if (!qcSDKEnable) {
            Toast.makeText(this, "\"Feature not supported", Toast.LENGTH_SHORT)
                    .show();
        }

        if (!switchCamera) {
            cameraObj = Camera.open(FRONT_CAMERA_INDEX);
        } else {
            cameraObj = Camera.open(BACK_CAMERA_INDEX);
        }
        mPreview = new CameraSurfacePreview(MainActivity.this, cameraObj);
        preview = (FrameLayout)findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        cameraObj.setPreviewCallback(MainActivity.this);
    }

    private void stopCamera() {
        if (cameraObj != null) {
            cameraObj.stopPreview();
            cameraObj.setPreviewCallback(null);
            preview.removeView(mPreview);
            cameraObj.release();
            if (qcSDKEnable) {
                faceProc.release();
                faceProc = null;
            }
        }
        cameraObj = null;
    }
}
