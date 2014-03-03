package ua.p2psafety;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ua.p2psafety.data.Prefs;
import ua.p2psafety.util.Utils;

public class SupporterFragment extends Fragment {
    TextView mEventInfo;
    Button mAudioBtn, mVideoBtn;
    Button mCloseEventBtn;
    Activity mActivity;

    public SupporterFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = getActivity();
        ((SosActivity) mActivity).getSupportActionBar().setHomeButtonEnabled(false);
        ((SosActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        View view = inflater.inflate(R.layout.frag_supporter, container, false);

        mEventInfo = (TextView) view.findViewById(R.id.txt_info);
        mAudioBtn = (Button) view.findViewById(R.id.btn_audio);
        mVideoBtn = (Button) view.findViewById(R.id.btn_video);
        mCloseEventBtn = (Button) view.findViewById(R.id.btn_close_event);

        return view;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        setupMediaButtons();

        mAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop all record services
                mActivity.stopService(new Intent(mActivity, AudioRecordService.class));
                mActivity.stopService(new Intent(mActivity, VideoRecordService.class));
                // start audio record if we that's what user wants
                if (!Utils.isServiceRunning(mActivity, AudioRecordService.class))
                    mActivity.startService(new Intent(mActivity, AudioRecordService.class));
                setupMediaButtons();
            }
        });

        mVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop all record services
                mActivity.stopService(new Intent(mActivity, AudioRecordService.class));
                mActivity.stopService(new Intent(mActivity, VideoRecordService.class));
                // start audio record if we that's what user whants
                if (!Utils.isServiceRunning(mActivity, VideoRecordService.class))
                    mActivity.startService(new Intent(mActivity, VideoRecordService.class));
                setupMediaButtons();
            }
        });

        mCloseEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: network request goes here

                Prefs.putSupporterMode(mActivity, false);
                mActivity.onBackPressed();
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            String support_url = bundle.getString(XmppService.SUPPORTER_URL_KEY);
            Location event_loc = (Location) bundle.get(XmppService.LOCATION_KEY);

            Log.i("SupporterFragment", "url: " + support_url);
            Log.i("SupporterFragment", "location: " + event_loc);
        }
    }

    private void setupMediaButtons() {
        if (Utils.isServiceRunning(mActivity, AudioRecordService.class))
            mAudioBtn.setText("Stop Audio record");
        else
            mAudioBtn.setText("Start Audio record");

        if (Utils.isServiceRunning(mActivity, VideoRecordService.class))
            mVideoBtn.setText("Stop Video record");
        else
            mVideoBtn.setText("Start Video record");
    }

    @Override
    public void onResume() {
        Prefs.putSupporterMode(mActivity, true);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}