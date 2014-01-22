package ua.p2psafety.media;


import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ua.p2psafety.DelayedSosService;
import ua.p2psafety.R;
import ua.p2psafety.SosActivity;

public class SetMediaFragment extends Fragment {
    public static final String TAG = "SetMediaFragment";
    private View vParent;

    public SetMediaFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final View rootView = inflater.inflate(R.layout.setup_media, container, false);

        final View frame_indent = rootView.findViewById(R.id.frame_indent2);
        frame_indent.setVisibility(View.VISIBLE);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        vParent = rootView;
        final Context context = getActivity();

        View btn_save = vParent.findViewById(R.id.btn_save);
        ((Button) btn_save).setTypeface(font);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DelayedSosService.isTimerOn()) {
                    // handle this case correctly
                } else {
                    Toast.makeText(getActivity(), getString(R.string.save), Toast.LENGTH_LONG).show();
                }
            }
        });

        ((TextView) rootView.findViewById(R.id.captionText)).setTypeface(font);
        ((TextView) rootView.findViewById(R.id.typeText)).setTypeface(font);
        ((TextView) rootView.findViewById(R.id.lengthText)).setTypeface(font);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}