package fimobile.technology.inc.CameraKiosk;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;

import java.util.List;

/**
 * Created by FM-JMK on 08/03/2018.
 */

public class TestMainActivity extends BaseActivity implements CameraDialog.CameraDialogParent, View.OnClickListener {

    private static final String TAG = "TestMainActivity";
    public static final String MY_PREFS_NAME = "MyPrefsFile";

    private static final boolean USE_SURFACE_ENCODER = false;
    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;
    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    /**
     * for camera preview display
     */
    private CameraViewInterface mUVCCameraView;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 640;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 480;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 1;
    private UVCCamera mUVCCamera;

    private View mToolsLayout, mValueLayout;
    ImageButton save_photo;
    ToggleButton camera_button;
    boolean isFirstRun = false;
    private ImageParameters mImageParameters;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testmain_layout);
        Log.d(TAG, "una oncreate ");
        final View view = findViewById(R.id.camera_view);
        mUVCCameraView = (CameraViewInterface)view;

        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        Log.d(TAG, " after usb monitor ");
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
        Log.d(TAG, " after usb handler ");
        save_photo = (ImageButton)findViewById(R.id.save_photo);
        save_photo.setOnClickListener(this);
        camera_button = (ToggleButton) findViewById(R.id.camera_button);
        camera_button.setOnCheckedChangeListener(mOnCheckedChangeListener);



    }
    ///AbsUVCCameraHandler: supportedSize:{"formats":[{"index":1,"type":4,"default":1,"size":["640x480","160x120","176x144","320x176","320x240","352x288","432x240","544x288","640x360","752x416","800x448","800x600","864x480","960x544","960x720","1024x576","1184x656","1280x720","1280x960"]},{"index":2,"type":6,"default":1,"size":["640x480","160x120","176x144","320x176","320x240","352x288","432x240","544x288","640x360","752x416","800x448","800x600","864x480","960x544","960x720","1024x576","1184x656","1280x720","1280x960"]}]}
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(TestMainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();


        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
//			if (DEBUG)
            Log.v(TAG, "onConnect:");
            mCameraHandler.open(ctrlBlock);
            startPreview();
            updateItems();
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
//			if (DEBUG)
            Log.v(TAG, "onDisconnect:");
            if (mCameraHandler != null) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCameraHandler.close();
                    }
                }, 0);
//                setCameraButton(false);
                updateItems();
            }
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(TestMainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
//            setCameraButton(false);
        }
    };

    private void startPreview() {
        Log.d(TAG, "startPreview");
        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
        mCameraHandler.startPreview(new Surface(st));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mCaptureButton.setVisibility(View.VISIBLE);
            }
        });
        updateItems();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
//		if (DEBUG)
        Log.v(TAG, "onDialogResult:canceled=" + canceled);
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        if (canceled) {
            editor.putBoolean("name", false);
        } else{
            editor.putBoolean("name", true);
        }
        editor.apply();
        editor.commit();
    }

    private void updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 100);
    }

    private void updateBitmap(){
//        runOnUiThread(mUpdateBitmapOnUITask, 10);
        new bitmapTask().execute("");

    }

    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    private int getValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    private int setValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    private int resetValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    private final Runnable mUpdateItemsOnUITask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
//            mToolsLayout.setVisibility(visible_active);
//            mBrightnessButton.setVisibility(checkSupportFlag(UVCCamera.PU_BRIGHTNESS)
//                            ? visible_active : View.INVISIBLE);
//            mContrastButton.setVisibility(checkSupportFlag(UVCCamera.PU_CONTRAST)
//                            ? visible_active : View.INVISIBLE);
        }
    };

    private final Runnable mUpdateBitmapOnUITask = new Runnable() {
        @Override
        public void run() {
//            Bitmap bitmap = mCameraHandler.getBitmap();
//            Log.d(TAG, " bitmap " + bitmap );
//            if(bitmap != null){
//                Log.d(TAG, " bitmap 1 " + bitmap );
//
////                Fragment newFragment = new EditSavePhotoFragment();
//                FragmentTransaction ft = getFragmentManager().beginTransaction();
//                ft.add(android.R.id.content, EditSavePhotoFragment.newInstance(bitmap)).commit();
//            }
        }
    };




    @Override
    public void onClick(View v) {
        Log.d(TAG, " onclick ");
        if (mCameraHandler.isOpened()) {
            if (checkPermissionWriteExternalStorage()) {
                mCameraHandler.captureStill();
                updateBitmap();
//                Bitmap bitmap = mCameraHandler.getBitmap();
//                Log.d(TAG, " bitmap 1 " + mCameraHandler.getBitmap( ) + " mCameraHandler " + mCameraHandler);

//                Fragment newFragment = new EditSavePhotoFragment();
//                FragmentTransaction ft = getFragmentManager().beginTransaction();
//                ft.add(android.R.id.content, EditSavePhotoFragment.newInstance(bitmap)).commit();
            }
//            return true;
        }


    }

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
            = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
            switch (compoundButton.getId()) {
                case R.id.camera_button:
                    Log.d(TAG, " oncheckchange ");
                    if (isChecked && !mCameraHandler.isOpened()) {
                        CameraDialog.showDialog(TestMainActivity.this);
                    } else {
                        mCameraHandler.close();
//                        setCameraButton(false);
                    }
                    break;
            }
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
//		if (DEBUG)
        Log.v(TAG, "una onStart:");
        mUSBMonitor.register();
        if (mUVCCameraView != null)
            mUVCCameraView.onResume();
    }

    @Override
    protected void onStop() {
//		if (DEBUG)
        Log.v(TAG, "onStop:");
        mCameraHandler.close();
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
//        setCameraButton(false);
        super.onStop();
    }

    @Override
    public void onDestroy() {
//		if (DEBUG)
        Log.v(TAG, "onDestroy:");
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraView = null;
//        mCameraButton = null;
//        mCaptureButton = null;
        super.onDestroy();
    }

    public void returnPhotoUri() {
//        Intent data = new Intent();
//        data.setData(uri);

//        if (getParent() == null) {
//            setResult(RESULT_OK, data);
//        } else {
//            getParent().setResult(RESULT_OK, data);
//        }
////        finish();
//        FragmentManager fm= getFragmentManager();
//        fm.popBackStack();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "una onresume ");

        mUSBMonitor.register();
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
//        String restoredText = prefs.getString("text", null);
//        if (restoredText != null) {
//            boolean name = prefs.getBoolean("name", true);//"No name defined" is the default value.
////            int idName = prefs.getInt("idName", 0); //0 is the default value.
//        }

                Log.d(TAG, " mUSBMonitor: " + mUSBMonitor);
            if (!mCameraHandler.isOpened() ) {
                boolean permit = prefs.getBoolean("name", false);
                Log.d(TAG, " isopened: " + permit);
                if(!permit)
                {
                    Log.d(TAG, " isopened ");
                    CameraDialog.showDialog(TestMainActivity.this);
                }

            } else {
                mCameraHandler.close();
                Log.d(TAG, " isopened not");
//                setCameraButton(false);
            }

    }

    private class bitmapTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            Bitmap bitmap = null;
            while (bitmap == null){
                bitmap = mCameraHandler.getBitmap();
                Log.d(TAG, " bitmap " + bitmap);
            }
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(android.R.id.content, EditSavePhotoFragment.newInstance(bitmap)).commit();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            save_photo.setVisibility(View.GONE);
        }
    }

//    public void onCancel(View view)
//    {
//        Log.d(TAG, " oncancel22 ");
////        getSupportFragmentManager().popBackStack();
//        FragmentManager fm= getFragmentManager();
////        fm.popBackStack();
//
//
//    }
}
