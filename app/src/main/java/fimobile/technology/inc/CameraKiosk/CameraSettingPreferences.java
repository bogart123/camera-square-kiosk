package fimobile.technology.inc.CameraKiosk;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by desmond on 4/10/15.
 */
public class CameraSettingPreferences {

    private static final String TAG = "CameraSetting";
    private static final String FLASH_MODE = "squarecamera__flash_mode";
    private static boolean has_permission;
    private static boolean force_stop;

    private static SharedPreferences getCameraSettingPreferences(@NonNull final Context context) {
        return context.getSharedPreferences("fimobile.technology.inc.CameraKiosk", Context.MODE_PRIVATE);
    }

    protected static void saveCameraFlashMode(@NonNull final Context context, @NonNull final String cameraFlashMode) {
        final SharedPreferences preferences = getCameraSettingPreferences(context);

        if (preferences != null) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(FLASH_MODE, cameraFlashMode);
            editor.apply();
        }
    }

    protected static String getCameraFlashMode(@NonNull final Context context) {
        final SharedPreferences preferences = getCameraSettingPreferences(context);

        if (preferences != null) {
            return preferences.getString(FLASH_MODE, Camera.Parameters.FLASH_MODE_AUTO);
        }

        return Camera.Parameters.FLASH_MODE_AUTO;
    }

    protected static void setPermission(@NonNull final Context context, boolean permission){
        Log.d("TAG", "Permission: "+ permission );
        has_permission = permission;
    }

    protected static boolean hasPermission(){
        return has_permission;
    }

    protected static void setForceStop(@NonNull final Context context, boolean stop){
        Log.d("TAG", "setForceStop: "+ stop );
        force_stop = stop;
    }

    protected static boolean forceStop(){
        return force_stop;
    }

}
