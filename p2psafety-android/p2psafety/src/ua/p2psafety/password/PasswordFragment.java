package ua.p2psafety.password;


import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import ua.p2psafety.DelayedSosService;
import ua.p2psafety.EventManager;
import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
import ua.p2psafety.data.Prefs;

public class PasswordFragment extends Fragment {
    public static final String TAG = "PasswordFragment";
    private View vParent;
    private Activity mActivity;

    public PasswordFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final View rootView = inflater.inflate(R.layout.setup_password, container, false);
        mActivity = getActivity();

        final View frame_indent = rootView.findViewById(R.id.frame_indent2);
        frame_indent.setVisibility(View.VISIBLE);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        vParent = rootView;
        final Context context = getActivity();
        final EditText editText = (EditText) vParent.findViewById(R.id.edt_msg);
        final CheckBox usePasswordBtn = (CheckBox) vParent.findViewById(R.id.fst_sos_lock);
        usePasswordBtn.setChecked(Prefs.getUsePassword(context));

        editText.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Light.ttf"));
        editText.setText(Prefs.getPassword(context));
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (EventManager.getInstance(mActivity).isSosStarted() || DelayedSosService.isTimerOn()) {
                    Toast.makeText(getActivity(), R.string.no_settings_while_sos,
                            Toast.LENGTH_LONG).show();
                    editText.clearFocus();
                }
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() <= 0) {
                    usePasswordBtn.setChecked(false);
                }
            }
        });

        usePasswordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (EventManager.getInstance(mActivity).isSosStarted() || DelayedSosService.isTimerOn()) {
                    Toast.makeText(getActivity(), R.string.no_settings_while_sos, Toast.LENGTH_LONG)
                         .show();
                    usePasswordBtn.setChecked(!isChecked); // reset changes
                    return;
                }
                if (isChecked && editText.length() <= 0)
                    usePasswordBtn.setChecked(false);
            }
        });

        View btn_save = vParent.findViewById(R.id.btn_save);
        ((Button) btn_save).setTypeface(font);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EventManager.getInstance(mActivity).isSosStarted() || DelayedSosService.isTimerOn()) {
                    // tell user we can't
                    Toast.makeText(getActivity(), R.string.no_settings_while_sos, Toast.LENGTH_LONG)
                         .show();
                } else {
                    Prefs.putPassword(context, editText.getText().toString());
                    Prefs.putUsePassword(context, usePasswordBtn.isChecked());
                    Toast.makeText(getActivity(), getString(R.string.save), Toast.LENGTH_LONG).show();
                }
            }
        });

        ((TextView) rootView.findViewById(R.id.textView)).setTypeface(font);

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
