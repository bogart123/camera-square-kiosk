package fimobile.technology.inc.CameraKiosk;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by desmond on 4/8/15.
 */
public class ResizeAnimation extends Animation {
    private static final String TAG = "null";
//    public static final String TAG = ResizeAnimation.class.getSimpleName();

    final int mStartLength;
    final int mFinalLength;
    int x,y;
    final boolean mIsPortrait;
    final View mView;


    public ResizeAnimation(@NonNull View view, final ImageParameters imageParameters) {
        mIsPortrait = imageParameters.isPortrait();
        mView = view;
        mStartLength = mIsPortrait ? mView.getHeight() : mView.getWidth();

        mFinalLength = imageParameters.getAnimationParameter();
//        Log.d(TAG, "Start: " + mStartLength + " final: " + mFinalLength);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int newLength = (int) (mStartLength + (mFinalLength - mStartLength) * interpolatedTime);

        if (mIsPortrait) {
            mView.getLayoutParams().height = newLength;
        } else {
            mView.getLayoutParams().width = newLength;
        }

        x = mView.getLayoutParams().height;
        y = mView.getLayoutParams().width;
        Log.d(TAG, "RESQ"+x+","+y);
        mView.requestLayout();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
