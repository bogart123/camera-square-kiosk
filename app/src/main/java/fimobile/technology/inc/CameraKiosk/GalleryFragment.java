package fimobile.technology.inc.CameraKiosk;

import android.Manifest;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by FM-JMK on 23/02/2018.
 */

public class GalleryFragment extends Fragment {

    public static final String TAG = GalleryFragment.class.getSimpleName();
    public static final String BITMAP_KEY = "bitmap_byte_array";
    public static final String ROTATION_KEY = "rotation";
    public static final String Path = "Pictures/SquareCamera";

    private static final int REQUEST_STORAGE = 1;
    private static final int REQUEST_SHARE_IMAGE = 2;
//    public static File Environ;
//    String Lugar;
    Uri myUri;
    ImageButton deleteGallery;
    ImageButton shareGallery;

    public static Fragment newInstance(Bitmap bitmap, String photoUri) {
        Fragment fragment = new GalleryFragment();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Bundle b = new Bundle();
        b.putByteArray("image",byteArray);
        b.putString("uri", photoUri);

        fragment.setArguments(b);
        return fragment;
    }

    public GalleryFragment(){}


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.camerakiosk__fragment_galleryedit, container, false);

        byte[] byteArray = getArguments().getByteArray("image");
        Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        final ImageView photoImageView = (ImageView) v.findViewById(R.id.gallery_photo);
        deleteGallery = (ImageButton) v.findViewById(R.id.gallery_delete);
        shareGallery = (ImageButton) v.findViewById(R.id.gallery_share);
        photoImageView.setImageBitmap(bmp);

        myUri = Uri.parse(getArguments().getString("uri"));

        shareGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final View view = getView();
                ImageView photoImageView = (ImageView) view.findViewById(R.id.gallery_photo);
                Bitmap bitmap = ((BitmapDrawable) photoImageView.getDrawable()).getBitmap();
                onShare(getActivity(), bitmap);
            }
        });

        deleteGallery.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final Dialog dialog = new Dialog(getActivity());
                dialog.setContentView(R.layout.custom_dialog_delete_image);
                dialog.setTitle("Delete Photo");
                dialog.setCancelable(false);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                Button OKtn = (Button) dialog.findViewById(R.id.btnYes);
                Button Cancelbtn = (Button) dialog.findViewById(R.id.btnNo);

                OKtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestForPermission();
                        dialog.dismiss();
                    }
                });

                Cancelbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });
        return v;
    }


    public static String getPath(Context context, Uri uri ) {
        String result = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver( ).query( uri, proj, null, null, null );
        if(cursor != null){
            if ( cursor.moveToFirst( ) ) {
                int column_index = cursor.getColumnIndexOrThrow( proj[0] );
                result = cursor.getString( column_index );
            }
            cursor.close( );
        }
        if(result == null) {
            result = "Not found";
        }
        return result;
    }

    public void callBroadCast(final File fi) {
        if (Build.VERSION.SDK_INT >= 14) {
            MediaScannerConnection.scanFile(getContext(), new String[]{Environment.getExternalStorageDirectory().toString()
                    + "/Pictures/SquareCamera/"
                    + fi.getName()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                /*
                 *   (non-Javadoc)
                 * @see android.media.MediaScannerConnection.OnScanCompletedListener#onScanCompleted(java.lang.String, android.net.Uri)
                 */
                public void onScanCompleted(String path, Uri uri) {
                    Log.e("ExternalStorage", "Scanned " + path + ":");
                    Log.e("ExternalStorage", "-> uri=" + uri);
                }
            });
        }
        else {
            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }

    private void requestForPermission() {
        RuntimePermissionActivity.startActivity(this,
                REQUEST_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (REQUEST_STORAGE == requestCode && data != null) {
            File fdelete = new File(getPath(getActivity(),myUri) );
            if (fdelete.exists())
            {
                if (fdelete.delete()) {
                    callBroadCast(fdelete);
                }
            }
            getFragmentManager().popBackStack();
        }
        super.onActivityResult(requestCode, resultCode, data);
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
}
