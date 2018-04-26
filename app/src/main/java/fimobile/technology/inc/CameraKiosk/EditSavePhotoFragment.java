package fimobile.technology.inc.CameraKiosk;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
//import android.support.v4.app.Fragment;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 *
 */
public class EditSavePhotoFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = EditSavePhotoFragment.class.getSimpleName();
    public static final String BITMAP_KEY = "bitmap_byte_array";
    public static final String ROTATION_KEY = "rotation";
    public static final String IMAGE_INFO = "image_info";

    private static final int REQUEST_STORAGE = 1;
    private static final int REQUEST_SHARE_IMAGE = 2;

    private ImageButton backBtn;
    private MainActivity mainActivity;
    private static String imgBitmap;
    private ImageView photoImageView;
    private Bitmap myBitmap;

    public static Fragment newInstance(String bitmap) {
        Fragment fragment = new EditSavePhotoFragment();

        Bundle args = new Bundle();

        args.putString("bitmap", bitmap);
//        args.putByteArray(BITMAP_KEY, bitmapByteArray);
//        args.putInt(ROTATION_KEY, rotation);
//        args.putParcelable(IMAGE_INFO, parameters);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, " oncreate ");
    }

    public EditSavePhotoFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.camerakiosk__fragment_edit_save_photo, container, false);

        backBtn = (ImageButton) v.findViewById(R.id.cancel);
        backBtn.setOnClickListener(this);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        int rotation = getArguments().getInt(ROTATION_KEY);
//        byte[] data = getArguments().getByteArray(BITMAP_KEY);
        imgBitmap = getArguments().getString("bitmap");
        Log.d(TAG, "imgBitmap " + imgBitmap);

        photoImageView = (ImageView) view.findViewById(R.id.photo);

        File file = new File(imgBitmap);

        if(file.exists()){
            myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Log.d(TAG, "myBitmap " + myBitmap);
            photoImageView.setImageBitmap(myBitmap);
        }


        view.findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraSettingPreferences.setForceStop(getActivity(),true);
                shareImage();
            }
        });

//        view.findViewById(R.id.save_photo).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                savePicture();
//            }
//        });
    }

    private void shareImage() {

        //  requestForPermission();
        final View view = getView();
        ImageView photoImageView = (ImageView) view.findViewById(R.id.photo);
        Bitmap bitmap = ((BitmapDrawable) photoImageView.getDrawable()).getBitmap();
        onShare(getActivity(), bitmap);
    }

    public void onShare(Context context, Bitmap bitmap) {

        int cropHeight;
        if (bitmap.getHeight() > bitmap.getWidth())
        {
            cropHeight = bitmap.getWidth();
        }
        else
        {
            cropHeight = bitmap.getHeight();
        }

//        bitmap = ThumbnailUtils.extractThumbnail(bitmap, cropHeight, cropHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "SquareCamera"
        );

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                //    return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(
                mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg"
        );
        Log.d(TAG,"PATH: " + mediaFile.getAbsolutePath());
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            FileOutputStream stream = new FileOutputStream(mediaFile);
            stream.write(out.toByteArray());
            stream.close();
            out.close();
            BitmapFactory.decodeByteArray(out.toByteArray(),0, out.size());

        } catch (IOException exception) {
            exception.printStackTrace();
        }
        String paths = mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg";
        //String paths = "storage/emulated/0/Picture/SquareCamera/IMG_20180222_114056.jpg";
        File file = new File(paths);

        Log.d(TAG, "FILEPATHFILE" + file);
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivityForResult(Intent.createChooser(intent, "Share Image"), REQUEST_SHARE_IMAGE);

    }

    private void rotatePicture(Bitmap bitmap, ImageView photoImageView) {
//        Bitmap bitmap = ImageUtility.decodeSampledBitmapFromByte(getActivity(), data);
//        Log.d(TAG, "original bitmap width " + bitmap.getWidth() + " height " + bitmap.getHeight());
//        if (rotation != 0) {
//            Bitmap oldBitmap = bitmap;
//
//            Matrix matrix = new Matrix();
//            matrix.postRotate(rotation);
//            matrix.preScale(-1.0f, 1.0f);
//
//            bitmap = Bitmap.createBitmap(
//                    oldBitmap, 0, 0, oldBitmap.getWidth(), oldBitmap.getHeight(), matrix, false
//            );
//
//            oldBitmap.recycle();
//        }

        photoImageView.setImageBitmap(bitmap);
    }

//    private void rotatePicture(byte[] data, ImageView photoImageView) {
//        Bitmap bitmap = ImageUtility.decodeSampledBitmapFromByte(getActivity(), data);
//        Log.d(TAG, "original bitmap width " + bitmap.getWidth() + " height " + bitmap.getHeight());
////        if (rotation != 0) {
////            Bitmap oldBitmap = bitmap;
////
////            Matrix matrix = new Matrix();
////            matrix.postRotate(rotation);
////            matrix.preScale(-1.0f, 1.0f);
////
////            bitmap = Bitmap.createBitmap(
////                    oldBitmap, 0, 0, oldBitmap.getWidth(), oldBitmap.getHeight(), matrix, false
////            );
////
////            oldBitmap.recycle();
////        }
//
//        photoImageView.setImageBitmap(bitmap);
//    }

    private void savePicture() {
        requestForPermission();
    }

    private void requestForPermission() {
        RuntimePermissionActivity.startActivity(this,
                REQUEST_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK != resultCode) return;

        Log.d(TAG, "requestCode: "+ requestCode);
        if (REQUEST_STORAGE == requestCode && data != null) {
            final boolean isGranted = data.getBooleanExtra(RuntimePermissionActivity.REQUESTED_PERMISSION, false);
            final View view = getView();
            if (isGranted && view != null) {
                ImageView photoImageView = (ImageView) view.findViewById(R.id.photo);

                Bitmap bitmap = ((BitmapDrawable) photoImageView.getDrawable()).getBitmap();
                Uri photoUri = ImageUtility.savePicture(getActivity(), bitmap);
                Log.d(TAG, "photoURI"+ photoUri);

//                ((MainActivity) getActivity()).returnPhotoUri(photoUri);
            }

        } else if (REQUEST_SHARE_IMAGE == requestCode && data != null){
           Log.d(TAG, "im here!");
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            Log.d(TAG, "photoURIsave" );
        }
    }


    @Override
    public void onClick(View v) {
        Log.d(TAG, "backback" );
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy" );
        if(myBitmap !=null){
            myBitmap.recycle();
            myBitmap = null;
        }

    }
}
