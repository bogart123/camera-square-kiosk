package fimobile.technology.inc.CameraKiosk;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;

import fimobile.technology.inc.CameraKiosk.imageeditor.EditImageActivity;
import fimobile.technology.inc.CameraKiosk.photoeditor.OnPhotoEditorListener;
import fimobile.technology.inc.CameraKiosk.photoeditor.PhotoEditor;
import fimobile.technology.inc.CameraKiosk.photoeditor.PhotoEditorView;
import fimobile.technology.inc.CameraKiosk.photoeditor.ViewType;

/**
 * Created by FM-JMK on 08/03/2018.
 */

public class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent, View.OnClickListener, OnPhotoEditorListener {

    private static final String TAG = "MainActivity";
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
    private ImageButton save_photo;
    private ToggleButton camera_button;
    private ImageParameters mImageParameters;

    private static String newBmp = "bitmap";
    private PhotoEditorView mPhotoEditorView;
    private PhotoEditor mPhotoEditor;
    private String oldBmp = null;
    private USBMonitor.UsbControlBlock usbCtrlBlock;
    private UsbDevice usbDevice;
    private boolean isPrefname;
    private TextView countTextView;
    private ImageButton timer;
    private String capture;
    private int[] images = { R.drawable.camerakiosk_sec_3, R.drawable.camerakiosk_sec_5,
            R.drawable.camerakiosk_timer };
    private int currentImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        final View view = findViewById(R.id.camera_view);
        mUVCCameraView = (CameraViewInterface)view;

        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
        save_photo = (ImageButton)findViewById(R.id.save_photo);
        save_photo.setOnClickListener(this);
        camera_button = (ToggleButton) findViewById(R.id.camera_button);
        camera_button.setOnCheckedChangeListener(mOnCheckedChangeListener);
        countTextView = (TextView) findViewById(R.id.countdown);
        currentImage = 2;
        timer = (ImageButton) findViewById(R.id.timer);
        timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentImage++;
                currentImage = currentImage % images.length;

                timer.setImageResource(images[currentImage]);
            }
        });
        checkPermissionWriteExternalStorage();

        isPrefname = false;
    }
    ///AbsUVCCameraHandler: supportedSize:{"formats":[{"index":1,"type":4,"default":1,"size":["640x480","160x120","176x144","320x176","320x240","352x288","432x240","544x288","640x360","752x416","800x448","800x600","864x480","960x544","960x720","1024x576","1184x656","1280x720","1280x960"]},{"index":2,"type":6,"default":1,"size":["640x480","160x120","176x144","320x176","320x240","352x288","432x240","544x288","640x360","752x416","800x448","800x600","864x480","960x544","960x720","1024x576","1184x656","1280x720","1280x960"]}]}
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();

            if (!mCameraHandler.isOpened() ) {
                CameraDialog.showDialog(MainActivity.this);
            } else {
                mCameraHandler.close();
            }

        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            usbDevice = device;
            usbCtrlBlock = ctrlBlock;
            mCameraHandler.open(ctrlBlock);
            startPreview();
            updateItems();
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
//			if (DEBUG)
            Log.v(TAG, "onDisconnect:");
//            if (mCameraHandler != null) {
//                queueEvent(new Runnable() {
//                    @Override
//                    public void run() {
//                        mCameraHandler.close();
//                    }
//                }, 0);
////                setCameraButton(false);
//                updateItems();
//            }
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
//            setCameraButton(false);
        }
    };

    private void startPreview() {
        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
        if(st != null){
            mCameraHandler.startPreview(new Surface(st));
            updateItems();
        }

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
        if (currentImage == 0 )
        {
            countTextView.setVisibility(View.VISIBLE);
            new CountDownTimer(4000, 1000)
            {
                public void onTick(long millisUntilFinished) {
                    countTextView.setText("" + millisUntilFinished / 1000);
                }
                public void onFinish() {
                    countTextView.setVisibility(View.GONE);
                    if (mCameraHandler.isOpened()) {
                        mCameraHandler.captureStill();
                        updateBitmap();
                    }
                }
            }.start();
        }
        else if (currentImage == 1)
        {
            countTextView.setVisibility(View.VISIBLE);
            new CountDownTimer(6000, 1000)
            {
                public void onTick(long millisUntilFinished) {
                    countTextView.setText("" + millisUntilFinished / 1000);
                }
                public void onFinish() {
                    countTextView.setVisibility(View.GONE);
                    if (mCameraHandler.isOpened()) {
                        mCameraHandler.captureStill();
                        updateBitmap();
                    }
                }
            }.start();
        }
        else if (currentImage == 2)
        {
            if (mCameraHandler.isOpened()) {
                mCameraHandler.captureStill();
                updateBitmap();
            }
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
                        CameraDialog.showDialog(MainActivity.this);
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

        final UsbManager mUsbManager;
        mUsbManager = (UsbManager)this.getSystemService(Context.USB_SERVICE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isPrefname = prefs.getBoolean("MY_PREFS_NAME", false);

        if (isPrefname)
        {
            CameraDialog.showDialog(MainActivity.this);
        }
        save_photo.setEnabled(true);
        save_photo.setClickable(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
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
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            oldBmp = null;
            while (oldBmp == null){
                oldBmp = mCameraHandler.getBitmap();
                if(oldBmp != null ){
                    if(!newBmp.equals(oldBmp)){
                        newBmp = oldBmp;
                    }else{
                        oldBmp = null;
                    }
                }
//                newBmp = oldBmp;
            }
            return newBmp;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(newBmp.equals(s))
            {
                Intent myIntent = new Intent(MainActivity.this, EditImageActivity.class);
                myIntent.putExtra("bitmap", oldBmp); //Optional parameters
                MainActivity.this.startActivity(myIntent);
            }

        }
    }

}
