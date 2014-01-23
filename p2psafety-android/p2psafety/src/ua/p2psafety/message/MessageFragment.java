package ua.p2psafety.message;


import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
import ua.p2psafety.data.Prefs;

/**
 * Created by ihorpysmennyi on 12/7/13.
 */
public class MessageFragment extends Fragment {
    public static final String TAG = "MessageFragment";
    private View vParent;

    public MessageFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final View rootView = inflater.inflate(R.layout.frag_message, container, false);
        final View frame_indent = rootView.findViewById(R.id.frame_indent2);
        frame_indent.setVisibility(View.VISIBLE);

        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        vParent = rootView;
        Context context = getActivity();
        CheckBox chk_geo = (CheckBox) vParent.findViewById(R.id.chk_geo);
        chk_geo.setChecked(Prefs.getIsLoc(context));
        EditText editText = (EditText) vParent.findViewById(R.id.edt_msg);
        editText.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Light.ttf"));
        editText.setText(Prefs.getMessage(context));
        View btn_save = vParent.findViewById(R.id.btn_save);
        ((Button) btn_save).setTypeface(font);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                Toast.makeText(getActivity(), getString(R.string.save), Toast.LENGTH_LONG).show();
            }
        });

        ((TextView) rootView.findViewById(R.id.textView)).setTypeface(font);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void save() {
        Context context = getActivity();
        EditText editText = (EditText) vParent.findViewById(R.id.edt_msg);
        CheckBox chk_geo = (CheckBox) vParent.findViewById(R.id.chk_geo);
        Prefs.putIsLoc(context, chk_geo.isChecked());
        Prefs.putMessage(context, editText.getText().toString());
    }

    @Override
    public void onPause() {
        super.onPause();
        save();
    }
}
