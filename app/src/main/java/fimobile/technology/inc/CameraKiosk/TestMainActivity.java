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

import fimobile.technology.inc.CameraKiosk.imageeditor.EditImageActivity;
import fimobile.technology.inc.CameraKiosk.photoeditor.OnPhotoEditorListener;
import fimobile.technology.inc.CameraKiosk.photoeditor.PhotoEditor;
import fimobile.technology.inc.CameraKiosk.photoeditor.PhotoEditorView;
import fimobile.technology.inc.CameraKiosk.photoeditor.ViewType;

import static android.graphics.Bitmap.Config.ARGB_8888;

/**
 * Created by FM-JMK on 08/03/2018.
 */

public class TestMainActivity extends BaseActivity implements CameraDialog.CameraDialogParent, View.OnClickListener, OnPhotoEditorListener {

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
    private static final int PREVIEW_HEIGHT = 640;
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

    private static String newBmp = "bitmap";
    private PhotoEditorView mPhotoEditorView;
    private PhotoEditor mPhotoEditor;
    String oldBmp;

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

        checkPermissionWriteExternalStorage();

//        Intent intent = getIntent();
//        value = intent.getStringExtra("key"); //if it's a string you stored.


//        mPhotoEditorView = findViewById(R.id.photoEditorView);
//
//        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
//                .setPinchTextScalable(true) // set flag to make text scalable when pinch
//                //.setDefaultTextTypeface(mTextRobotoTf)
//                //.setDefaultEmojiTypeface(mEmojiTypeFace)
//                .build(); // build photo editor sdk
//
//        mPhotoEditor.setOnPhotoEditorListener(this);


    }
    ///AbsUVCCameraHandler: supportedSize:{"formats":[{"index":1,"type":4,"default":1,"size":["640x480","160x120","176x144","320x176","320x240","352x288","432x240","544x288","640x360","752x416","800x448","800x600","864x480","960x544","960x720","1024x576","1184x656","1280x720","1280x960"]},{"index":2,"type":6,"default":1,"size":["640x480","160x120","176x144","320x176","320x240","352x288","432x240","544x288","640x360","752x416","800x448","800x600","864x480","960x544","960x720","1024x576","1184x656","1280x720","1280x960"]}]}
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(TestMainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();

            if (!mCameraHandler.isOpened() ) {
                Log.d(TAG, " isopened ");
                CameraDialog.showDialog(TestMainActivity.this);
            } else {
                mCameraHandler.close();
                Log.d(TAG, " isopened not");
            }

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
        }
    };

    @Override
    public void onClick(View v) {
        Log.d(TAG, " onclick ");
        if (mCameraHandler.isOpened()) {
//            save_photo.setEnabled(false);
//            save_photo.setClickable(false);
            mCameraHandler.captureStill();
            updateBitmap();
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
        Log.v(TAG, "una onStop:");
        mCameraHandler.close();
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
//        setCameraButton(false);
        super.onStop();
    }

    @Override
    public void onDestroy() {
//		if (DEBUG)
        Log.v(TAG, "una onDestroy:");
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


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "una onresume " );
//        updateItems();
//        save_photo.setVisibility(View.VISIBLE);



        save_photo.setEnabled(true);
        save_photo.setClickable(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "una onpause " + save_photo.isEnabled());
        save_photo.setEnabled(false);
        save_photo.setClickable(false);
    }

    @Override
    public void onEditTextChangeListener(View rootView, String text, int colorCode) {

    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {

    }

    @Override
    public void onRemoveViewListener(int numberOfAddedViews) {

    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {

    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {

    }

    private class bitmapTask extends AsyncTask<String, Void, String>
    {

        @Override
        protected void onPreExecute()
        {
            Log.d(TAG, " onpreexecute");
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            oldBmp = null;
            while (oldBmp == null){
                oldBmp = mCameraHandler.getBitmap();
                Log.d(TAG,"newBmp 111 " + oldBmp);
                Log.d(TAG,"newBmp 11 " + newBmp);
                if(oldBmp == null ){
                       Log.d(TAG,"newBmp 44 " + newBmp);
                    if(!newBmp.equals(oldBmp)){
//                        newBmp = oldBmp;
                        Log.d(TAG,"newBmp 121 " + newBmp);
                    }else{
                        Log.d(TAG,"newBmp 22 " + newBmp);
                        oldBmp = null;
                    }
                }
//                newBmp = oldBmp;
                Log.d(TAG,"newBmp 33 " + oldBmp + " newBmp " + newBmp);
            }
            return newBmp;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG,"newBmp 444 " + newBmp );
            if(newBmp.equals(s)){
//                FragmentTransaction ft = getFragmentManager().beginTransaction();
//                ft.add(android.R.id.content, EditSavePhotoFragment.newInstance(newBmp)).commit();

                Intent myIntent = new Intent(TestMainActivity.this, EditImageActivity.class);
                myIntent.putExtra("bitmap", oldBmp); //Optional parameters
                TestMainActivity.this.startActivity(myIntent);
            }

        }
    }

    public void onEdit (View view)
    {
        Log.d(TAG, "onEdit");
        Toast.makeText(this, " edit napindot mo", Toast.LENGTH_SHORT).show();
    }

    public void onPictureframe(View view)
    {
        Log.d(TAG, "onPictureframe");
        Toast.makeText(this, " frame napindot mo", Toast.LENGTH_LONG).show();
    }

    public void onTimer(View view)
    {
        Log.d(TAG, "onTimer");
        Toast.makeText(this, " timer napindot mo", Toast.LENGTH_SHORT).show();
    }
}
