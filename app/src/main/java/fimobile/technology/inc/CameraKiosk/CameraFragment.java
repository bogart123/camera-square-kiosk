package fimobile.technology.inc.CameraKiosk;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.common.BaseFragment;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;

import java.util.List;

public class CameraFragment extends BaseFragment implements  Camera.PictureCallback, FragmentCompat.OnRequestPermissionsResultCallback, View.OnClickListener, CameraDialog.CameraDialogParent {

    public static final String TAG = CameraFragment.class.getSimpleName();
    public static final String CAMERA_ID_KEY = "camera_id";
    public static final String CAMERA_FLASH_KEY = "flash_mode";
    public static final String IMAGE_INFO = "image_info";
    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_EXTERNAL_PERMISSION = 1;

    private static final int PICTURE_SIZE_MAX_WIDTH = 1280;
    private static final int PREVIEW_SIZE_MAX_WIDTH = 640;

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


    private int mCameraID;
    private String mFlashMode;
//    private Camera mCamera;
//    private SquareCameraPreview mPreviewView;
    private SurfaceHolder mSurfaceHolder;

    private boolean mIsSafeToTakePhoto = false;

    private ImageParameters mImageParameters;

    private CameraOrientationListener mOrientationListener;

    private static boolean has_permission = false;

    public static Fragment newInstance() {
        return new CameraFragment();
    }

    public CameraFragment() {
    }

    private static final int SELECT_IMAGE = 1;
    private Bitmap currentImage;
    private ImageView selectedImage;
//    private ImageView image;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_CAMERA = 0;
    ImageButton timerImg;



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mOrientationListener = new CameraOrientationListener(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restore your state here because a double rotation with this fragment
        // in the backstack will cause improper state restoration
        // onCreate() -> onSavedInstanceState() instead of going through onCreateView()
        Log.d(TAG, " oncreate 1 ");
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                CameraSettingPreferences.setPermission(getActivity(), false);
                requestCamerapermission();
            }
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                CameraSettingPreferences.setPermission(getActivity(), false);
                requestExternalStorage();
            }
        }

        if (mUSBMonitor == null) {
            mUSBMonitor = new USBMonitor(getActivity().getApplicationContext(), mOnDeviceConnectListener);
            final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(getActivity(), R.xml.device_filter);
            mUSBMonitor.setDeviceFilter(filters);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        Log.d(TAG, "oncreate 2 ");
        View v = inflater.inflate(R.layout.main_layout, container, false);

//        image = (ImageView) v.findViewById(R.id.image);

        final View view = v.findViewById(R.id.camera_view);
        mUVCCameraView = (CameraViewInterface)view;

        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
//        mUSBMonitor = new USBMonitor(getActivity().getApplicationContext(), mOnDeviceConnectListener);
        Log.d(TAG, " after usb monitor ");
        mCameraHandler = UVCCameraHandler.createHandler(getActivity(), mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
        Log.d(TAG, " after usb handler ");
        save_photo = (ImageButton)v.findViewById(R.id.save_photo);
        save_photo.setOnClickListener(this );
        camera_button = (ToggleButton) v.findViewById(R.id.camera_button);
        camera_button.setOnCheckedChangeListener(mOnCheckedChangeListener);
        timerImg = (ImageButton)v.findViewById(R.id.timer);
        timerImg.setOnClickListener(this);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mOrientationListener.enable();

//        mPreviewView = (SquareCameraPreview) view.findViewById(R.id.camera_preview_view);
//        mPreviewView.getHolder().addCallback(CameraFragment.this);

//        final View topCoverView = view.findViewById(R.id.cover_top_view);
//        final View btnCoverView = view.findViewById(R.id.cover_bottom_view);

//        if (savedInstanceState == null) {
//            mCameraID = getBackCameraID();
//            mFlashMode = CameraSettingPreferences.getCameraFlashMode(getActivity());
//            mImageParameters = new ImageParameters();
//        } else {
//            mCameraID = savedInstanceState.getInt(CAMERA_ID_KEY);
//            mFlashMode = savedInstanceState.getString(CAMERA_FLASH_KEY);
//            mImageParameters = savedInstanceState.getParcelable(IMAGE_INFO);
//        }
//
//        mImageParameters.mIsPortrait =
//                getDeviceDefaultOrientation();

//        if (savedInstanceState == null) {
//            ViewTreeObserver observer = mPreviewView.getViewTreeObserver();
//            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                @Override
//                public void onGlobalLayout() {
//                    mImageParameters.mPreviewWidth = mPreviewView.getWidth();
//                    mImageParameters.mPreviewHeight = mPreviewView.getHeight();
//
//                    mImageParameters.mCoverWidth = mImageParameters.mCoverHeight
//                            = mImageParameters.calculateCoverWidthHeight();
//
////                    Log.d(TAG, "parameters: " + mImageParameters.getStringValues());
////                    Log.d(TAG, "cover height " + topCoverView.getHeight());
//                    resizeTopAndBtmCover(topCoverView, btnCoverView);
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                        mPreviewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                    } else {
//                        mPreviewView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                    }
//                }
//            });
//        } else {
//            if (mImageParameters.isPortrait()) {
//                topCoverView.getLayoutParams().height = mImageParameters.mCoverHeight;
//                btnCoverView.getLayoutParams().height = mImageParameters.mCoverHeight;
//            } else {
//                topCoverView.getLayoutParams().width = mImageParameters.mCoverWidth;
//                btnCoverView.getLayoutParams().width = mImageParameters.mCoverWidth;
//            }
//        }

//        final ImageButton swapCameraBtn = (ImageButton) view.findViewById(R.id.change_camera);
//        swapCameraBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = new Intent();
//// Show only images, no videos or anything else
//                intent.setType("image/*");
//                intent.setAction(Intent.ACTION_PICK);
//// Always show the chooser (if there are multiple options available)
//                startActivityForResult(Intent.createChooser(intent, "Select App"), 1);
//
//                CameraSettingPreferences.setForceStop(getActivity(),true);
//
//            }
//        });

//        final View changeCameraFlashModeBtn = view.findViewById(R.id.flash);
//        changeCameraFlashModeBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_AUTO)) {
//                    mFlashMode = Camera.Parameters.FLASH_MODE_ON;
//                } else if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_ON)) {
//                    mFlashMode = Camera.Parameters.FLASH_MODE_OFF;
//                } else if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_OFF)) {
//                    mFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
//                }
//
//                setupFlashMode();
//                setupCamera();
//            }
//        });
//        setupFlashMode();

//        final ImageView takePhotoBtn = (ImageView) view.findViewById(R.id.capture_image_button);
//        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                takePicture();
//            }
//        });
    }
    public void requestCamerapermission() {
        Log.d(TAG, "requestcamerapermission");
        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Log.d(TAG, "requestcamerapermission2");
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            Log.d(TAG, "requestcamerapermission3");
            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    public void requestExternalStorage() {
        Log.d(TAG, "requestExternalStorage");
        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.d(TAG, "requestExternalStorage1");
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            Log.d(TAG, "requestExternalStorage2");
            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_PERMISSION);
        }
    }

    private boolean getDeviceDefaultOrientation() {
        WindowManager windowManager = (WindowManager)this.getContext().getSystemService(Context.WINDOW_SERVICE);
        Configuration config = getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();
        if( ( (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE )
                || ( (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT ) ) {
            Log.d(TAG, "getDeviceDefaultOrientation: false");
            return false;
        }
        else {
            Log.d(TAG, "getDeviceDefaultOrientation: true" );
            return true;
        }
    }
    private void setupFlashMode() {
        View view = getView();
        if (view == null) return;

        final TextView autoFlashIcon = (TextView) view.findViewById(R.id.auto_flash_icon);
        if (Camera.Parameters.FLASH_MODE_AUTO.equalsIgnoreCase(mFlashMode)) {
            autoFlashIcon.setText("Auto");
        } else if (Camera.Parameters.FLASH_MODE_ON.equalsIgnoreCase(mFlashMode)) {
            autoFlashIcon.setText("On");
        } else if (Camera.Parameters.FLASH_MODE_OFF.equalsIgnoreCase(mFlashMode)) {
            autoFlashIcon.setText("Off");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        Log.d(TAG, "onSaveInstanceState");
        outState.putInt(CAMERA_ID_KEY, mCameraID);
        outState.putString(CAMERA_FLASH_KEY, mFlashMode);
        outState.putParcelable(IMAGE_INFO, mImageParameters);
        super.onSaveInstanceState(outState);
    }

    private void resizeTopAndBtmCover(final View topCover, final View bottomCover) {
        ResizeAnimation resizeTopAnimation
                = new ResizeAnimation(topCover, mImageParameters);
        resizeTopAnimation.setDuration(800);
        resizeTopAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        topCover.startAnimation(resizeTopAnimation);

        ResizeAnimation resizeBtmAnimation
                = new ResizeAnimation(bottomCover, mImageParameters);
        resizeBtmAnimation.setDuration(800);
        resizeBtmAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        bottomCover.startAnimation(resizeBtmAnimation);
    }

//    private void getCamera(int cameraID) {
//        if(mCamera == null){
//            try {
//                mCamera = Camera.open(cameraID);
////                mPreviewView.setCamera(mCamera);
//            } catch (Exception e) {
//                Log.d(TAG, "Can't open camera with id " + cameraID);
//                e.printStackTrace();
//            }
//        }
//    }

    /**
     * Restart the camera preview
     */
//    private void restartPreview() {
//        if (mCamera != null) {
//            stopCameraPreview();
//            mCamera.release();
//            mCamera = null;
//        }
//
//        getCamera(mCameraID);
//        if(mCamera != null){
//            startCameraPreview();
//        }
//    }

    /**
     * Start the camera preview
     */
    private void startCameraPreview() {
        determineDisplayOrientation();
//        setupCamera();

//        try {
//            mCamera.setPreviewDisplay(mSurfaceHolder);
//            mCamera.startPreview();
//
//            setSafeToTakePhoto(true);
//            setCameraFocusReady(true);
//        } catch (IOException e) {
//            Log.d(TAG, "Can't start camera preview due to IOException " + e);
//            e.printStackTrace();
//        }
    }

    /**
     * Stop the camera preview
     */
    private void stopCameraPreview() {
        setSafeToTakePhoto(false);
        setCameraFocusReady(false);

        // Nulls out callbacks, stops face detection
//        mCamera.stopPreview();
//        mPreviewView.setCamera(null);
    }

    private void setSafeToTakePhoto(final boolean isSafeToTakePhoto) {
        mIsSafeToTakePhoto = isSafeToTakePhoto;
    }

    private void setCameraFocusReady(final boolean isFocusReady) {
//        if (this.mPreviewView != null) {
//            mPreviewView.setIsFocusReady(isFocusReady);
//        }
    }

    /**
     * Determine the current display orientation and rotate the camera preview
     * accordingly
     */
    private void determineDisplayOrientation() {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(mCameraID, cameraInfo);

        // Clockwise rotation needed to align the window display to the natural position
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: {
                degrees = 0;
                break;
            }
            case Surface.ROTATION_90: {
                degrees = 90;
                break;
            }
            case Surface.ROTATION_180: {
                degrees = 180;
                break;
            }
            case Surface.ROTATION_270: {
                degrees = 270;
                break;
            }
        }

        int displayOrientation;

        // CameraInfo.Orientation is the angle relative to the natural position of the device
        // in clockwise rotation (angle that is rotated clockwise from the natural position)
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            // Orientation is angle of rotation when facing the camera for
            // the camera image to match the natural orientation of the device
            displayOrientation = (cameraInfo.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
        }

        mImageParameters.mDisplayOrientation = displayOrientation;
        mImageParameters.mLayoutOrientation = degrees;
//        if(mCamera != null)
//        mCamera.setDisplayOrientation(mImageParameters.mDisplayOrientation);
    }

    /**
     * Setup the camera parameters
     */
//    private void setupCamera() {
//        // Never keep a global parameters
//        Camera.Parameters parameters = mCamera.getParameters();
//
//        Size bestPreviewSize = determineBestPreviewSize(parameters);
//        Size bestPictureSize = determineBestPictureSize(parameters);
//
//        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
//        parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);
//
//
//        // Set continuous picture focus, if it's supported
//        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        }
//
////        final View changeCameraFlashModeBtn = getView().findViewById(R.id.flash);
////        List<String> flashModes = parameters.getSupportedFlashModes();
////        if (flashModes != null && flashModes.contains(mFlashMode)) {
////            parameters.setFlashMode(mFlashMode);
////            changeCameraFlashModeBtn.setVisibility(View.VISIBLE);
////        } else {
////            changeCameraFlashModeBtn.setVisibility(View.INVISIBLE);
////        }
//
//        // Lock in the changes
//        mCamera.setParameters(parameters);
//    }

    private Size determineBestPreviewSize(Camera.Parameters parameters) {
        return determineBestSize(parameters.getSupportedPreviewSizes(), PREVIEW_SIZE_MAX_WIDTH);
    }

    private Size determineBestPictureSize(Camera.Parameters parameters) {
        return determineBestSize(parameters.getSupportedPictureSizes(), PICTURE_SIZE_MAX_WIDTH);
    }

    private Size determineBestSize(List<Size> sizes, int widthThreshold) {
        Size bestSize = null;
        Size size;
        int numOfSizes = sizes.size();
        for (int i = 0; i < numOfSizes; i++) {
            size = sizes.get(i);
            boolean isDesireRatio = (size.width / 4) == (size.height / 3);
            boolean isBetterSize = (bestSize == null) || size.width > bestSize.width;

            if (isDesireRatio && isBetterSize) {
                bestSize = size;
            }
        }

        if (bestSize == null) {
            Log.d(TAG, "cannot find the best camera size");
            return sizes.get(sizes.size() - 1);
        }

        return bestSize;
    }

    private int getFrontCameraID() {
        PackageManager pm = getActivity().getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return CameraInfo.CAMERA_FACING_FRONT;
        }

        return getBackCameraID();
    }

    private int getBackCameraID() {
        return CameraInfo.CAMERA_FACING_FRONT;
    }

    /**
     * Take a picture
     */
//    private void takePicture() {
//
//        if (mIsSafeToTakePhoto) {
//            setSafeToTakePhoto(false);
//
//            mOrientationListener.rememberOrientation();
//
//            // Shutter callback occurs after the image is captured. This can
//            // be used to trigger a sound to let the user know that image is taken
//            Camera.ShutterCallback shutterCallback = null;
//
//            // Raw callback occurs when the raw image data is available
//            Camera.PictureCallback raw = null;
//
//            // postView callback occurs when a scaled, fully processed
//            // postView image is available.
//            Camera.PictureCallback postView = null;
//
//            // jpeg callback occurs when the compressed image is available
////            mCamera.takePicture(shutterCallback, raw, postView, this);
//        }
//    }

    @Override
    public void onResume() {
        super.onResume();
        mUSBMonitor.register();
//        if (mUVCCameraView != null)
//            mUVCCameraView.onResume();

//        if (mCamera == null) {
//            CameraSettingPreferences.setForceStop(getContext(), false);
//            restartPreview();
//        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(TAG, "onStart:");
//        mUSBMonitor.register();
//        if (mUVCCameraView != null)
//            mUVCCameraView.onResume();
    }

    @Override
    public void onStop() {
        mOrientationListener.disable();

        // stop the preview
//        if (mCamera != null) {
//            stopCameraPreview();
//            mCamera.release();
//            mCamera = null;
//        }
//        CameraSettingPreferences.saveCameraFlashMode(getActivity(), mFlashMode);
        Log.v(TAG, "onStop:");
        mCameraHandler.close();
        if (mUVCCameraView != null) {
            mUVCCameraView.onPause();
        }
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


//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//        mSurfaceHolder = holder;

//        getCamera(mCameraID);
//        if(mCamera != null) {
//            CameraSettingPreferences.setPermission(getActivity(), true);
//            startCameraPreview();
//        }
//    }

//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        // The surface is destroyed with the visibility of the SurfaceView is set to View.Invisible
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, " onActivityResult CameraFragment");
        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "photoURIsave333" );
            Uri photoUri = data.getData();
            if (photoUri != null) {

                Log.d(TAG, " onActivityResult11 " + photoUri.getPath() );
                try {
                    currentImage = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoUri);
//                    image.setImageBitmap(currentImage);
                    getFragmentManager()
                            .beginTransaction()
                            .replace(
                                    R.id.fragment_container,
                                    GalleryFragment.newInstance(currentImage, photoUri.toString()),
                                    GalleryFragment.TAG)
                            .addToBackStack(null)
                            .commit();


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (resultCode != Activity.RESULT_OK) return;

        switch (requestCode) {
            case 1:
                Uri imageUri = data.getData();
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * A picture has been taken
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        int rotation = getPhotoRotation();
//        Log.d(TAG, "normal orientation: " + orientation);
//        Log.d(TAG, "Rotate Picture by: " + rotation);
//        getFragmentManager()
//                .beginTransaction()
//                .replace(
//                        R.id.fragment_container,
//                        EditSavePhotoFragment.newInstance(data, rotation, mImageParameters.createCopy()),
//                        EditSavePhotoFragment.TAG)
//                .addToBackStack(null)
//                .commit();

        setSafeToTakePhoto(true);
    }



    private int getPhotoRotation() {
        int rotation;
        int orientation = mOrientationListener.getRememberedNormalOrientation();
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(mCameraID, info);

        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation ) % 180;
        } else {
            rotation = (info.orientation + orientation) % 360;
        }

        return rotation;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {

    }

    /**
     * When orientation changes, onOrientationChanged(int) of the listener will be called
     */
    private static class CameraOrientationListener extends OrientationEventListener {

        private int mCurrentNormalizedOrientation;
        private int mRememberedNormalOrientation;

        public CameraOrientationListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation != ORIENTATION_UNKNOWN) {
                mCurrentNormalizedOrientation = normalize(orientation);
            }
        }

        /**
         * @param degrees Amount of clockwise rotation from the device's natural position
         * @return Normalized degrees to just 0, 90, 180, 270
         */
        private int normalize(int degrees) {
            if (degrees > 315 || degrees <= 45) {
                return 0;
            }

            if (degrees > 45 && degrees <= 135) {
                return 90;
            }

            if (degrees > 135 && degrees <= 225) {
                return 180;
            }

            if (degrees > 225 && degrees <= 315) {
                return 270;
            }

            throw new RuntimeException("The physics as we know them are no more. Watch out for anomalies.");
        }

        public void rememberOrientation() {
            mRememberedNormalOrientation = mCurrentNormalizedOrientation;
        }

        public int getRememberedNormalOrientation() {
            rememberOrientation();
            return mRememberedNormalOrientation;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void openFolderChooserDialogSAF(boolean from_preferences) {
        Log.d(TAG, "openFolderChooserDialogSAF: " + from_preferences);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 42);
    }
    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage("Permission Required!")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Permission_ok");
                            FragmentCompat.requestPermissions(parent,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "Permission_no");
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "requestCode: "+requestCode);
        switch (requestCode){
            case REQUEST_CAMERA_PERMISSION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    CameraSettingPreferences.setPermission(getActivity(), true);
                }
                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(getContext(), "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();

//            if (!mCameraHandler.isOpened()) {
//                Log.d(TAG, " isopened ");
//                CameraDialog.showDialog(getActivity());
//            } else {
//                mCameraHandler.close();
//                Log.d(TAG, " isopened not");
////                setCameraButton(false);
//            }
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
            Toast.makeText(getContext(), "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mCaptureButton.setVisibility(View.VISIBLE);
            }
        });
        updateItems();
    }

    private void updateItems() {
//        getActivity().runOnUiThread(mUpdateItemsOnUITask, 100);
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

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
            = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
            switch (compoundButton.getId()) {
                case R.id.camera_button:
                    Log.d(TAG, " oncheckchange ");
                    if (isChecked && !mCameraHandler.isOpened()) {
                        CameraDialog.showDialog(getParentFragment().getActivity());
//                        CameraDialog.newInstance();
//                        updateCameraDialog();
                    } else {
                        mCameraHandler.close();
//                        setCameraButton(false);
                    }
                    break;
            }
        }
    };

    private void updateCameraDialog() {
        final Fragment fragment = getFragmentManager().findFragmentByTag("CameraDialog");
        if (fragment instanceof CameraDialog) {
            ((CameraDialog)fragment).updateDevices();
        }
    }



//    private final Runnable mUpdateItemsOnUITask = new Runnable() {
//        @Override
//        public void run() {
//            if (isFinishing()) return;
//            final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
////            mToolsLayout.setVisibility(visible_active);
////            mBrightnessButton.setVisibility(checkSupportFlag(UVCCamera.PU_BRIGHTNESS)
////                            ? visible_active : View.INVISIBLE);
////            mContrastButton.setVisibility(checkSupportFlag(UVCCamera.PU_CONTRAST)
////                            ? visible_active : View.INVISIBLE);
//        }
//    };
}
