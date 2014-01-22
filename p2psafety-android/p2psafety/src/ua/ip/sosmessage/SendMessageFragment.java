package ua.ip.sosmessage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import ua.ip.sosmessage.message.MessageFragment;
import ua.ip.sosmessage.setphones.SetPhoneFragment;
import ua.ip.sosmessage.sms.MessageResolver;

/**
 * Created by ihorpysmennyi on 12/14/13.
 */
public class SendMessageFragment extends Fragment {
    Button mDelayedSosBtn;
    Activity mActivity;

    private View.OnClickListener lsnr = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Fragment mfragment;
            FragmentManager mfragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = mfragmentManager.beginTransaction();
            //fragmentTransaction.setCustomAnimations(R.anim.slide_left_in, R.anim.slide_left_out, R.anim.slide_right_in, R.anim.slide_right_out);

            switch (v.getId()) {
                case R.id.delayedSosBtn:
                    mfragment = new DelayedSosFragment();
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                    break;
            }
        }
    };

    public SendMessageFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = getActivity();

        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(false);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        View rootView = inflater.inflate(R.layout.frag_sendmessage, container, false);

        mDelayedSosBtn = (Button) rootView.findViewById(R.id.delayedSosBtn);
        mDelayedSosBtn.setOnClickListener(lsnr);

        View btn_send = rootView.findViewById(R.id.button);
        btn_send.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                SosManager sosManager = SosManager.getInstance(mActivity);
                if (sosManager.isSosStarted()) {
                    sosManager.stopSos();
                } else {
                    sosManager.startSos();
                }
                return false;
            }
        });

        Button settingsBtn = (Button) rootView.findViewById(R.id.btn_settings);
        settingsBtn.setTypeface(font);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment mFragment = new SettingsFragment();
                FragmentManager mFragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                for (int i = 0; i < mFragmentManager.getBackStackEntryCount(); ++i) {
                    mFragmentManager.popBackStack();
                }
                //fragmentTransaction.setCustomAnimations(R.anim.slide_right_in, R.anim.slide_right_out, R.anim.slide_left_in, R.anim.slide_left_out);
                fragmentTransaction.addToBackStack("SettingsFragment");
                fragmentTransaction.replace(R.id.content_frame, mFragment).commit();
            }
        });

        ((TextView)rootView.findViewById(R.id.textView)).setTypeface(font);
        rootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if( keyCode == KeyEvent.KEYCODE_BACK )
                {   getActivity().finish();
                    return true;
                }
                return false;
            }
        });
        return rootView;
    }


}
