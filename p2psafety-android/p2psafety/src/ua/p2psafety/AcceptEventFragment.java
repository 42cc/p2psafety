package ua.p2psafety;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import ua.p2psafety.data.Prefs;

public class AcceptEventFragment extends Fragment {
    TextView mEventInfo;
    Button mAcceptBtn, mIgnoreBtn;
    Activity mActivity;

    Location mEventLocation;
    String mEventSupportUrl;

    Boolean mAccepted = false;

    public AcceptEventFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = getActivity();
        ((SosActivity) mActivity).getSupportActionBar().setHomeButtonEnabled(false);
        ((SosActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        View view = inflater.inflate(R.layout.frag_accept_event, container, false);

        mEventInfo = (TextView) view.findViewById(R.id.txt_info);
        mAcceptBtn = (Button) view.findViewById(R.id.btn_accept);
        mIgnoreBtn = (Button) view.findViewById(R.id.btn_ignore);

        return view;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        mAccepted = false;

        mAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptEvent();
            }
        });

        mIgnoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ignoreEvent();
                mActivity.finish();
            }
        });

        Bundle bundle = getArguments();
        mEventLocation = (Location) bundle.get(XmppService.LOCATION_KEY);
        mEventSupportUrl = bundle.getString(XmppService.SUPPORTER_URL_KEY);

        System.out.println("onViewCreated. location: " + mEventLocation);
        System.out.println("onViewCreated. url: " + mEventSupportUrl);
    }

    private void acceptEvent() {
        mAccepted = true;

        // open Supporter screen
        Bundle bundle = new Bundle();
        bundle.putString(XmppService.SUPPORTER_URL_KEY, mEventSupportUrl);
        bundle.putParcelable(XmppService.LOCATION_KEY, mEventLocation);
        Fragment fragment = new SupporterFragment();
        fragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.content_frame, fragment).commit();
    }

    private void ignoreEvent() {
        mAccepted = false;
        Toast.makeText(mActivity, "You decided to abandon that poor guy. He's in trouble :(", Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i("AcceptEventFragment", "onPause");
        //if (!mAccepted)
        //    ignoreEvent();
        super.onPause();
    }
}