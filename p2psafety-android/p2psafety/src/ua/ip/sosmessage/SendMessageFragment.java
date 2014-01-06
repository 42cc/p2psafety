package ua.ip.sosmessage;

import android.graphics.Typeface;
import android.os.Bundle;
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
    private View.OnClickListener lsnr = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Fragment mfragment;
            FragmentManager mfragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = mfragmentManager.beginTransaction();
            //fragmentTransaction.setCustomAnimations(R.anim.slide_left_in, R.anim.slide_left_out, R.anim.slide_right_in, R.anim.slide_right_out);

            switch (v.getId()) {
                case R.id.button:
                    MessageResolver resolver = new MessageResolver(getActivity(), false);
                    resolver.sendMessages();
                    break;
                case R.id.btn_phone:
                    mfragment = new SetPhoneFragment();
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                    break;
                case R.id.btn_edt:
                    mfragment = new MessageFragment();
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
        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(false);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        View rootView = inflater.inflate(R.layout.frag_sendmessage, container, false);
        View btn_send = rootView.findViewById(R.id.button);
        btn_send.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MessageResolver resolver = new MessageResolver(getActivity(), false);
                resolver.sendMessages();
                return false;
            }
        });
        View btn_phone=rootView.findViewById(R.id.btn_phone);
        ((Button)btn_phone).setTypeface(font);

        btn_phone.setOnClickListener(lsnr);
        View btn_edt=rootView.findViewById(R.id.btn_edt);
        ((Button)btn_edt).setTypeface(font);
        btn_edt.setOnClickListener(lsnr);

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
