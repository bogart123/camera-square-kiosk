package fimobile.technology.inc.CameraKiosk.imageeditor;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import fimobile.technology.inc.CameraKiosk.R;
import fimobile.technology.inc.CameraKiosk.photoeditor.OnPhotoEditorListener;
import fimobile.technology.inc.CameraKiosk.photoeditor.PhotoEditor;
import fimobile.technology.inc.CameraKiosk.photoeditor.PhotoEditorView;
import fimobile.technology.inc.CameraKiosk.photoeditor.ViewType;


public class EditImageActivity extends BaseActivity implements OnPhotoEditorListener,
        View.OnClickListener,
        PropertiesBSFragment.Properties,
        EmojiBSFragment.EmojiListener,
        StickerBSFragment.StickerListener {

    private static final String TAG = EditImageActivity.class.getSimpleName();
    public static final String EXTRA_IMAGE_PATHS = "extra_image_paths";
    private static final int CAMERA_REQUEST = 52;
    private static final int PICK_REQUEST = 53;
    private static final int REQUEST_SHARE_IMAGE = 2;
    private PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;
    private PropertiesBSFragment mPropertiesBSFragment;
    private EmojiBSFragment mEmojiBSFragment;
    private StickerBSFragment mStickerBSFragment;
    private TextView mTxtCurrentTool;
    private Typeface mWonderFont;
    private static String imgBitmap;
    private Bitmap myBitmap;
    private String newString;
    private File file;
    public static final String MY_PREFS_NAME = "MyPrefsFile";

    /**
     * launch editor with multiple image
     *
     * @param context
     * @param imagesPath
     */
    public static void launch(Context context, ArrayList<String> imagesPath) {
        Intent starter = new Intent(context, EditImageActivity.class);
        starter.putExtra(EXTRA_IMAGE_PATHS, imagesPath);
        context.startActivity(starter);
    }

    /**
     * launch editor with single image
     *
     * @param context
     * @param imagePath
     */
    public static void launch(Context context, String imagePath) {
        ArrayList<String> imagePaths = new ArrayList<>();
        imagePaths.add(imagePath);
        launch(context, imagePaths);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.camerakiosk_edit_image);
        initViews();

        mWonderFont = Typeface.createFromAsset(getAssets(), "beyond_wonderland.ttf");
        mPropertiesBSFragment = new PropertiesBSFragment();
        mEmojiBSFragment = new EmojiBSFragment();
        mStickerBSFragment = new StickerBSFragment();
        mStickerBSFragment.setStickerListener(this);
        mEmojiBSFragment.setEmojiListener(this);
        mPropertiesBSFragment.setPropertiesChangeListener(this);

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                //.setDefaultTextTypeface(mTextRobotoTf)
                //.setDefaultEmojiTypeface(mEmojiTypeFace)
                .build(); // build photo editor sdk
        mPhotoEditor.setOnPhotoEditorListener(this);

        newString = getIntent().getExtras().getString("bitmap","bitmap");
        File file = new File(newString);
        Log.d(TAG, "imgBitmap " + imgBitmap + " newString " + newString + " file " + file);
        if(file.exists()){

        myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        mPhotoEditorView.getSource().setImageBitmap(myBitmap);
        }

        //Set Image Dynamically
        //mPhotoEditorView.getSource().setImageResource(R.drawable.got);
    }

    private void initViews() {
        ImageView imgPencil;
        ImageView imgEraser;
        ImageView imgUndo;
        ImageView imgRedo;
        ImageView imgText;
        ImageView imgCamera;
        ImageView imgGallery;
        ImageView imgSticker;
        ImageView imgEmo;
        ImageView imgSave;
        ImageView imgShare;
        ImageView imgClose;
        ImageView imgFrame;

        mPhotoEditorView = findViewById(R.id.photoEditorView);

        imgEmo = findViewById(R.id.imgEmoji);
        imgEmo.setOnClickListener(this);

        imgSticker = findViewById(R.id.imgSticker);
        imgSticker.setOnClickListener(this);

        imgPencil = findViewById(R.id.imgPencil);
        imgPencil.setOnClickListener(this);

        imgText = findViewById(R.id.imgText);
        imgText.setOnClickListener(this);

        imgEraser = findViewById(R.id.btnEraser);
        imgEraser.setOnClickListener(this);

        imgUndo = findViewById(R.id.imgUndo);
        imgUndo.setOnClickListener(this);

        imgRedo = findViewById(R.id.imgRedo);
        imgRedo.setOnClickListener(this);

//        imgCamera = findViewById(R.id.imgCamera);
//        imgCamera.setOnClickListener(this);
//
//        imgGallery = findViewById(R.id.imgGallery);
//        imgGallery.setOnClickListener(this);

        imgShare = findViewById(R.id.imgShare);
        imgShare.setOnClickListener(this);

//        imgSave = findViewById(R.id.imgSave);
//        imgSave.setOnClickListener(this);

        imgClose = findViewById(R.id.imgClose);
        imgClose.setOnClickListener(this);

        imgFrame = findViewById(R.id.imgFrame);
        imgFrame.setOnClickListener(this);
    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode) {
        TextEditorDialogFragment textEditorDialogFragment =
                TextEditorDialogFragment.show(this, text, colorCode);
        textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
            @Override
            public void onDone(String inputText, int colorCode) {
                mPhotoEditor.editText(rootView, inputText, colorCode);
//                mTxtCurrentTool.setText(R.string.camerakiosk_label_text);
            }
        });
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onAddViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onRemoveViewListener(int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener() called with: numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.imgPencil:
                Log.d(TAG, " pencil " + " mPhotoEditor " + mPhotoEditor + " mTxtCurrentTool " + mTxtCurrentTool + " mPropertiesBSFragment "
                        + mPropertiesBSFragment);
                mPhotoEditor.setBrushDrawingMode(true);
//                mTxtCurrentTool.setText(R.string.camerakiosk_label_brush);
                mPropertiesBSFragment.show(getSupportFragmentManager(), mPropertiesBSFragment.getTag());
                break;
            case R.id.btnEraser:
                mPhotoEditor.brushEraser();
//                mTxtCurrentTool.setText(R.string.camerakiosk_label_eraser);
                break;
            case R.id.imgText:
                TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this);
                textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
                    @Override
                    public void onDone(String inputText, int colorCode) {
                        mPhotoEditor.addText(inputText, colorCode);
//                        mTxtCurrentTool.setText(R.string.camerakiosk_label_text);
                    }
                });
                break;

            case R.id.imgUndo:
                mPhotoEditor.undo();
                break;

            case R.id.imgRedo:
                mPhotoEditor.redo();
                break;

//            case R.id.imgSave:
//                saveImage();
//                break;

            case R.id.imgClose:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

                if (!mPhotoEditor.isCacheEmpty()) {
                    showSaveDialog();
                } else {
                    Log.d(TAG, " locked finish ");
                    Boolean yourLocked = prefs.edit().putBoolean("MY_PREFS_NAME", true).commit();
                    finish();
                }
                break;

            case R.id.imgSticker:
                mStickerBSFragment.show(getSupportFragmentManager(), mStickerBSFragment.getTag());
                break;

            case R.id.imgEmoji:
                mEmojiBSFragment.show(getSupportFragmentManager(), mEmojiBSFragment.getTag());
                break;

//              case R.id.imgCamera:
//                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(cameraIntent, CAMERA_REQUEST);
//                break;
//
//            case R.id.imgGallery:
//                Intent intent = new Intent();
//                intent.setType("image/*");
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST);
//                break;

            case R.id.imgFrame :
                Log.d(TAG, " imgFrame 1");
                mPhotoEditorView.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.FILL_PARENT, ConstraintLayout.LayoutParams.FILL_PARENT));
                mPhotoEditorView.setPadding(50,0,50,50);
//                resizeView(view, 200, 200);
//                mPhotoEditorView.animate()
//                        .translationY(view.getHeight())
//                        .alpha(1.0f)
//                        .setListener(null);



                break;

            case R.id.imgShare:
                onShare();
                Log.d(TAG, " imgFrame 2");
                break;

        }
    }

    @SuppressLint("MissingPermission")
    private void saveImage() {
        if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading("Saving...");
//            File file = new File(Environment.getExternalStorageDirectory()
                    file = new File("storage/emulated/0/DCIM/USBCameraTest/"
                    + File.separator + ""
                    + System.currentTimeMillis() + ".png");
            try {
                file.createNewFile();
                mPhotoEditor.saveImage(file.getAbsolutePath(), new PhotoEditor.OnSaveListener() {
                    @Override
                    public void onSuccess(@NonNull String imagePath) {
                        hideLoading();
//                        showSnackbar("Image Saved Successfully");
                        mPhotoEditorView.getSource().setImageURI(Uri.fromFile(new File(imagePath)));
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        hideLoading();
                        showSnackbar("Failed to save Image");
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST:
                    mPhotoEditor.clearAllViews();
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    mPhotoEditorView.getSource().setImageBitmap(photo);
                    break;
                case PICK_REQUEST:
                    try {
                        mPhotoEditor.clearAllViews();
                        Uri uri = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        mPhotoEditorView.getSource().setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case REQUEST_SHARE_IMAGE:
                    Log.d(TAG, "DONE Sharing");
                    file.delete();
                    AccountManager manager = (AccountManager) this.getSystemService(Context.ACCOUNT_SERVICE);
                    Account[] accountlist = manager.getAccounts();
                    Log.d(TAG, "ACCOUNTSNITO");
                    for (int i = 0; i < accountlist.length; i++) {
                        Log.d(TAG, "ACCOUNTSNITO2" + accountlist[i]);
                        manager.removeAccount(accountlist[i], null, null);
                    }
                    //clear accounts
                    break;
            }
        }
    }

    @Override
    public void onColorChanged(int colorCode) {
        mPhotoEditor.setBrushColor(colorCode);
//        mTxtCurrentTool.setText(R.string.camerakiosk_label_brush);
    }

    @Override
    public void onOpacityChanged(int opacity) {
        mPhotoEditor.setOpacity(opacity);
//        mTxtCurrentTool.setText(R.string.camerakiosk_label_brush);
    }

    @Override
    public void onBrushSizeChanged(int brushSize) {
        mPhotoEditor.setBrushSize(brushSize);
//        mTxtCurrentTool.setText(R.string.camerakiosk_label_brush);
    }

    @Override
    public void onEmojiClick(String emojiUnicode) {
        mPhotoEditor.addEmoji(emojiUnicode);
//        mTxtCurrentTool.setText(R.string.camerakiosk_label_emoji);

    }

    @Override
    public void onStickerClick(Bitmap bitmap) {
        mPhotoEditor.addImage(bitmap);
        Log.d(TAG, " onStickerClick ");
//        mTxtCurrentTool.setText(R.string.camerakiosk_label_sticker);
    }

    @Override
    public void isPermissionGranted(boolean isGranted, String permission) {
        if (isGranted) {
            saveImage();
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you want to exit without saving image ?");
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveImage();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNeutralButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.create().show();

    }

    public void onShare()
    {
        saveImage();
        Log.d(TAG, "FILEPATHFILE" + file);
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivityForResult(Intent.createChooser(intent, "Share Image"), REQUEST_SHARE_IMAGE);
    }

}
