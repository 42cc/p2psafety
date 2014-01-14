package ua.ip.sosmessage;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ua.ip.sosmessage.setphones.SetPhoneFragment;

/**
 * Created by ihorpysmennyi on 12/17/13.
 */
public class FirstRunFragment extends Fragment {
    public FirstRunFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.frag_enter_lay, container, false);
        //((SosActivity) getActivity()).getSupportActionBar().hide();
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Fragment mfragment;
                FragmentManager mfragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = mfragmentManager.beginTransaction();
                //fragmentTransaction.setCustomAnimations(R.anim.slide_left_in, R.anim.slide_left_out, R.anim.slide_right_in, R.anim.slide_right_out);
                mfragment = new SetPhoneFragment();
                fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                return true;
            }
        });
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        //((SosActivity) getActivity()).getSupportActionBar().show();
    }
}
