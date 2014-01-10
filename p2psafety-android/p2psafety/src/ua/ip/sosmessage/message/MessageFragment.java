package ua.ip.sosmessage.message;


import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;
import ua.ip.sosmessage.R;
import ua.ip.sosmessage.SendMessageFragment;
import ua.ip.sosmessage.SosActivity;
import ua.ip.sosmessage.data.Prefs;

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
        if (!getActivity().getString(R.string.strissmall).equals("small"))
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (rootView == null || getActivity() == null)
                        return;
                    final View pic_protect = rootView.findViewById(R.id.img_protect);
                    final View frame_indent = rootView.findViewById(R.id.frame_indent2);
                    int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight() - ((SosActivity)
                            getActivity()).getSupportActionBar().getHeight();
                    if (heightDiff > 150) {
                        Log.d("KeyBoard", "mVisible" + heightDiff);
                        pic_protect.setVisibility(View.GONE);
                        frame_indent.setVisibility(View.VISIBLE);
                    } else if (pic_protect.getVisibility() == View.GONE) {
                        Log.d("KeyBoard", "minVisible" + heightDiff);
                        pic_protect.setVisibility(View.VISIBLE);
                        frame_indent.setVisibility(View.GONE);
                    }
                }
            });
        else {

            final View pic_protect = rootView.findViewById(R.id.img_protect);
            final View frame_indent = rootView.findViewById(R.id.frame_indent2);
            pic_protect.setVisibility(View.GONE);
            frame_indent.setVisibility(View.VISIBLE);
        }
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
