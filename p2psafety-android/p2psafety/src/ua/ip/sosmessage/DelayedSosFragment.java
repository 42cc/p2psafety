package ua.ip.sosmessage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import ua.ip.sosmessage.data.Prefs;
import ua.ip.sosmessage.message.MessageFragment;
import ua.ip.sosmessage.setphones.SetPhoneFragment;
import ua.ip.sosmessage.sms.MessageResolver;

public class DelayedSosFragment extends Fragment {
    TextView mTimerText;
    Button mTimerBtn;
    ImageButton mArrowUpBtn, mArrowDownBtn;
    CheckBox mSosLockView;
    EditText mPasswordText;

    Activity mActivity;

    public DelayedSosFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = getActivity();
        ((SosActivity) mActivity).getSupportActionBar().setHomeButtonEnabled(false);
        ((SosActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        View view = inflater.inflate(R.layout.frag_sos_timer, container, false);

        mTimerText = (TextView) view.findViewById(R.id.timerText);
        mTimerBtn = (Button) view.findViewById(R.id.timerBtn);
        mArrowUpBtn = (ImageButton) view.findViewById(R.id.arrowUpBtn);
        mArrowDownBtn = (ImageButton) view.findViewById(R.id.arrowDownBtn);
        mSosLockView = (CheckBox) view.findViewById(R.id.fst_sos_lock);
        mPasswordText = (EditText) view.findViewById(R.id.fst_password_text);

        return view;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        mPasswordText.setText(Prefs.getPassword(mActivity));
        mPasswordText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                Prefs.putPassword(mActivity, s.toString());
                if (s.length() <= 0) {
                    mSosLockView.setChecked(false);
                }
            }
        });

        mSosLockView.setChecked(Prefs.getUsePassword(mActivity));
        mSosLockView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putUsePassword(mActivity, isChecked);
                if (isChecked && mPasswordText.length() <= 0)
                    mSosLockView.setChecked(false);
            }
        });

        mTimerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DelayedSosService.isTimerOn()) {
                    // stop timer
                    if (!mSosLockView.isChecked()) {
                        mActivity.stopService(new Intent(mActivity, DelayedSosService.class));
                        onTimerStop();
                    } else {
                        askPasswordAndStopTimer();
                    }
                } else {
                    // start timer
                    mActivity.startService(new Intent(mActivity, DelayedSosService.class));
                    onTimerStart();
                }
            }
        });

        mArrowUpBtn.setOnTouchListener(new OnTouchContinuousListener() {
            @Override
            public void onTouchRepeat(View view) {
                long sosDelay = DelayedSosService.getSosDelay(mActivity);
                sosDelay += 1*60*1000; // +1 min
                sosDelay = Math.min(sosDelay, 120 * 60 * 1000); // max 120 min
                DelayedSosService.setSosDelay(mActivity, sosDelay);
                showSosDelay(sosDelay);
            }
        });

        mArrowDownBtn.setOnTouchListener(new OnTouchContinuousListener() {
            @Override
            public void onTouchRepeat(View view) {
                long sosDelay = DelayedSosService.getSosDelay(mActivity);
                sosDelay -= 1*60*1000; // -1 min
                sosDelay = Math.max(sosDelay, 1*60*1000); // min 1 min
                DelayedSosService.setSosDelay(mActivity, sosDelay);
                showSosDelay(sosDelay);
            }
        });
    }

    // builds dialog with password prompt
    private void askPasswordAndStopTimer() {
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
                                checkPasswordAndStopTimer(userInput.getText().toString());
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
                    checkPasswordAndStopTimer(userInput.getText().toString());
                    alertDialog.dismiss();
                }
                return true;
            }
        });
    }

    // stops timer or builds dialog with retry/cancel buttons
    private void checkPasswordAndStopTimer(String password) {
        if (password.equals(mPasswordText.getText().toString()))
        {
            mActivity.stopService(new Intent(mActivity, DelayedSosService.class));
            onTimerStop();
        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(R.string.wrong_password);
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    askPasswordAndStopTimer();
                }
            });
            builder.create().show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // start listen to delayed sos timer
        IntentFilter filter = new IntentFilter();
        filter.addAction(DelayedSosService.SOS_DELAY_TICK);
        filter.addAction(DelayedSosService.SOS_DELAY_FINISH);
        filter.addAction(DelayedSosService.SOS_DELAY_CANCEL);
        mActivity.registerReceiver(mBroadcastReceiver, filter);

        if (DelayedSosService.isTimerOn())
            onTimerStart();
        else
            onTimerStop();
    }

    @Override
    public void onPause() {
        super.onPause();

        mActivity.unregisterReceiver(mBroadcastReceiver);
    }

    private void onTimerStart() {
        showSosDelay(DelayedSosService.getTimeLeft());
        mPasswordText.setEnabled(false);
        mSosLockView.setEnabled(false);
        mArrowUpBtn.setEnabled(false);
        mArrowDownBtn.setEnabled(false);
        mArrowUpBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_up_inactive));
        mArrowDownBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_down_inactive));
        mTimerBtn.setText(getResources().getString(R.string.stop));
    }

    private void onTimerStop() {
        showSosDelay(DelayedSosService.getSosDelay(mActivity));
        mPasswordText.setEnabled(true);
        mSosLockView.setEnabled(true);
        mArrowUpBtn.setEnabled(true);
        mArrowDownBtn.setEnabled(true);
        mArrowUpBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_up));
        mArrowDownBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_down));
        mTimerBtn.setText(getResources().getString(R.string.start));
    }

    private void showSosDelay(long sosDelay) {
        String timerText = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toSeconds(sosDelay) / 60,
                TimeUnit.MILLISECONDS.toSeconds(sosDelay) % 60);
        mTimerText.setText(timerText);
    }

    // Broadcast from DelayedSosService timer
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(DelayedSosService.SOS_DELAY_TICK)) {
                // show time left
                showSosDelay(DelayedSosService.getTimeLeft());
            }
            else if (action.equals(DelayedSosService.SOS_DELAY_FINISH)) {
                onTimerStop();
            }
            else if (action.equals(DelayedSosService.SOS_DELAY_CANCEL)) {
                onTimerStop();
            }
        }
    };
}