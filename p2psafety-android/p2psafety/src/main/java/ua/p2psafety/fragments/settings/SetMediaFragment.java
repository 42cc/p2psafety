package ua.p2psafety.fragments.settings;


import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
import ua.p2psafety.util.EventManager;
import ua.p2psafety.data.Prefs;

public class SetMediaFragment extends Fragment {
    public static final String TAG = "SetMediaFragment";

    private Button mSaveBtn;
    private Spinner mMediaTypeView;
    private Spinner mMediaLengthView;

    private Activity mActivity;

    public SetMediaFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_media, container, false);

        mActivity = getActivity();
        mSaveBtn = (Button) view.findViewById(R.id.btn_save);
        mMediaTypeView = (Spinner) view.findViewById(R.id.media_type);
        mMediaLengthView = (Spinner) view.findViewById(R.id.media_length);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((SosActivity) mActivity).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Typeface font = Typeface.createFromAsset(mActivity.getAssets(), "fonts/RobotoCondensed-Bold.ttf");
        ((TextView) view.findViewById(R.id.captionText)).setTypeface(font);
        ((TextView) view.findViewById(R.id.typeText)).setTypeface(font);
        ((TextView) view.findViewById(R.id.lengthText)).setTypeface(font);

        View frame_indent = view.findViewById(R.id.frame_indent2);
        frame_indent.setVisibility(View.VISIBLE);

        mSaveBtn.setTypeface(font);
    }

    @Override
    public void onResume() {
        super.onResume();

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EventManager.getInstance(mActivity).isSosStarted() || Prefs.isPassiveSosStarted(mActivity)) {
                    Toast.makeText(mActivity,
                            getString(R.string.no_settings_while_sos), Toast.LENGTH_LONG)
                         .show();
                } else {
                    int optionPos = mMediaLengthView.getSelectedItemPosition();
                    Prefs.putMediaRecordLength(mActivity, optionPosToRecordLength(optionPos));
                    Prefs.putMediaRecordType(mActivity, mMediaTypeView.getSelectedItemPosition());

                    Toast.makeText(mActivity, R.string.save, Toast.LENGTH_LONG).show();
                }
            }
        });

        long recordLength = Prefs.getMediaRecordLength(mActivity);
        mMediaLengthView.setSelection(recordLengthToOptionPos(recordLength));
        mMediaTypeView.setSelection(Prefs.getMediaRecordType(mActivity));
    }

    private long optionPosToRecordLength(int pos) {
        long recordLength = 1*1000*30; // default is 30 sec
        switch (pos) {
            case 0: // 30 sec
                recordLength = 1*1000*30;
                break;
            case 1: // 1 min
                recordLength = 1*1000*60;
                break;
            case 2: // 2 min
                recordLength = 2*1000*60;
                break;
            case 3: // 3 min
                recordLength = 3*1000*60;
                break;
            case 4: // 5 min
                recordLength = 5*1000*60;
                break;
        }
        return recordLength;
    }

    private int recordLengthToOptionPos(long recordLength) {
        int pos = 0;

        if (recordLength == 1*1000*30) // 30 sec
            pos = 0;
        else if (recordLength == 1*1000*60) // 1 min
            pos = 1;
        else if (recordLength == 2*1000*60) // 2 min
            pos = 2;
        else if (recordLength == 3*1000*60) // 3 min
            pos = 3;
        else if (recordLength == 5*1000*60) // 5 min
            pos = 4;

        return pos;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}