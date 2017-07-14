package io.github.gzhechu.dualcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.hardware.Camera;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "DualCameraActivity";

    private Camera mBackCamera;
    private Camera mFrontCamera;
    private LiveCameraView mBackCamPreview;
    private LiveCameraView mFrontCamPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_main);

        //If authorisation not granted for camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            //ask for authorisation
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 50);

        Log.i(TAG, "Number of cameras: " + Camera.getNumberOfCameras());

        releaseCameraAndPreview();

        // Create an instance of Camera
        mBackCamera = getCameraInstance(0);
        if (null != mBackCamera) {
            Log.d(TAG, "Got back camera ");
            // Create back camera Preview view and set it as the content of our activity.
            mBackCamPreview = new LiveCameraView(this, mBackCamera);
            FrameLayout backPreview = (FrameLayout) findViewById(R.id.back_camera_preview);
            backPreview.addView(mBackCamPreview);
        }

        mFrontCamera = getCameraInstance(1);
        if (null != mFrontCamera) {
            Log.d(TAG, "Got front camera ");
            mFrontCamPreview = new LiveCameraView(this, mFrontCamera);
            FrameLayout frontPreview = (FrameLayout) findViewById(R.id.front_camera_preview);
            frontPreview.addView(mFrontCamPreview);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCameraAndPreview();
        Log.e(TAG, "start onStop~~~");
    }

    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
            Log.d(TAG, "Got camera " + cameraId);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "Camera " + cameraId + " not available! " + e.toString());
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCameraAndPreview() {
        Log.i(TAG, "Try to release cameras: " + Camera.getNumberOfCameras());
//        FrontCameraPreview.setCamera(null);
//        BackCameraPreview.setCamera(null);
        if (mBackCamera != null) {
            if (mBackCamera != null) {
                mBackCamera.release();
                mBackCamera = null;
            }
        }
        if (mFrontCamPreview != null) {
            if (mFrontCamera != null) {
                mFrontCamera.release();
                mFrontCamera = null;
            }
        }
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
     * A basic Camera preview class
     */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    public static void followScreenOrientation(Context context, Camera camera) {
        final int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            camera.setDisplayOrientation(0);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            camera.setDisplayOrientation(90);
        }
    }

    public class LiveCameraView extends SurfaceView implements SurfaceHolder.Callback {
//        private final static String TAG = LiveCameraView.class.getSimpleName();
        private Camera mCamera;
        private SurfaceHolder mSurfaceHolder;

        public LiveCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            mSurfaceHolder = this.getHolder();
            mSurfaceHolder.addCallback(this);
        }

        public LiveCameraView(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mSurfaceHolder = getHolder();
            mSurfaceHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
//            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "Start preview display[SURFACE-CREATED]");
            startPreviewDisplay(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mSurfaceHolder.getSurface() == null) {
                return;
            }
            followScreenOrientation(getContext(), mCamera);
            Log.d(TAG, "Restart preview display[SURFACE-CHANGED]");
            stopPreviewDisplay();
            startPreviewDisplay(mSurfaceHolder);
        }

        public void setCamera(Camera camera) {
            mCamera = camera;
            final Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            params.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
        }

        private void startPreviewDisplay(SurfaceHolder holder) {
            checkCamera();
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.e(TAG, "Error while START preview for camera", e);
            }
        }

        private void stopPreviewDisplay() {
            checkCamera();
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error while STOP preview for camera", e);
            }
        }

        private void checkCamera() {
            if (mCamera == null) {
                throw new IllegalStateException("Camera must be set when start/stop preview, call <setCamera(Camera)> to set");
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "Stop preview display[SURFACE-DESTROYED]");
            stopPreviewDisplay();
        }
    }
}
