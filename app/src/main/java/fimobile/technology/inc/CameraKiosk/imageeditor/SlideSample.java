package fimobile.technology.inc.CameraKiosk.imageeditor;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.transitionseverywhere.Slide;
import com.transitionseverywhere.TransitionManager;

import fimobile.technology.inc.CameraKiosk.R;

/**
 * Created by FM-JMK on 25/05/2018.
 */

public class SlideSample extends Fragment {


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camerakiosk_fragment_slide, container, false);

        final ViewGroup transitionsContainer = (ViewGroup) view.findViewById(R.id.transitions_container);
        final TextView text = (TextView) transitionsContainer.findViewById(R.id.text);

//        transitionsContainer.findViewById(R.id.button).setOnClickListener(new VisibleToggleClickListener() {
//            @Override
//            protected void changeVisibility(boolean visible) {
//                TransitionManager.beginDelayedTransition(transitionsContainer, new Slide(Gravity.RIGHT));
//                text.setVisibility(visible ? View.VISIBLE : View.GONE);
//            }
//        });

        return view;
    }
}
