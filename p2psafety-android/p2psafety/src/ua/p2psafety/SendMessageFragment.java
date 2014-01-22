package ua.p2psafety;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ua.p2psafety.data.Prefs;

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
                    if (!Prefs.getUsePassword(mActivity)) {
                        sosManager.stopSos();
                    } else {
                        askPasswordAndCancelSos();
                    }
                } else {
                    // stop delayed SOS if it is on
                    if (DelayedSosService.isTimerOn()) {
                        mActivity.stopService(new Intent(mActivity, DelayedSosService.class));
                    }
                    // start normal sos
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

    // builds dialog with password prompt
    private void askPasswordAndCancelSos() {
        LayoutInflater li = LayoutInflater.from(mActivity);
        View promptsView = li.inflate(R.layout.password_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.pd_password_edit);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                checkPasswordAndCancelSos(userInput.getText().toString());
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        userInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    checkPasswordAndCancelSos(userInput.getText().toString());
                    alertDialog.dismiss();
                }
                return true;
            }
        });
    }

    // cancels sos or builds dialog with retry/cancel buttons
    private void checkPasswordAndCancelSos(String password) {
        if (password.equals(Prefs.getPassword(mActivity))) {
            SosManager.getInstance(mActivity).stopSos();
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(R.string.wrong_password);
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    askPasswordAndCancelSos();
                }
            });
            builder.create().show();
        }
    }
}
