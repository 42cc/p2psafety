package ua.p2psafety.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.listeners.OnTouchContinuousListener;
import ua.p2psafety.services.PassiveSosService;

public class PassiveSosFragment extends Fragment {
    TextView mTimerText;
    Button mTimerBtn;
    ImageButton mArrowUpBtn, mArrowDownBtn;
    private AlertDialog mAlertDialog;

    Activity mActivity;

    public PassiveSosFragment() {
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

        return view;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        PassiveSosService.registerReceiver(mActivity, mBroadcastReceiver);
        showSosDelay(Prefs.getPassiveSosInterval(mActivity));

        if (Prefs.isPassiveSosStarted(mActivity)) {
            onTimerStart();
        } else {
            onTimerStop();
        }

        mTimerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Prefs.isPassiveSosStarted(mActivity)) {
                    // stop timer
                    mActivity.stopService(new Intent(mActivity, PassiveSosService.class));
                    onTimerStop();
                    Prefs.setPassiveSosStarted(mActivity, false);
                    Toast.makeText(mActivity, "Passive SOS stopped", Toast.LENGTH_SHORT).show();
                } else {
                    // start timer
                    mActivity.startService(new Intent(mActivity, PassiveSosService.class));
                    onTimerStart();
                    Prefs.setPassiveSosStarted(mActivity, true);
                    askSosReason();
                    Toast.makeText(mActivity, "Passive SOS started", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mArrowUpBtn.setOnTouchListener(new OnTouchContinuousListener() {
            @Override
            public void onTouchRepeat(View view) {
                long sosDelay = Prefs.getPassiveSosInterval(mActivity);
                sosDelay += 1*60*1000; // +1 min
                sosDelay = Math.min(sosDelay, 120 * 60 * 1000); // max 120 min
                //DelayedSosService.setSosDelay(mActivity, sosDelay);
                Prefs.setPassiveSosInterval(mActivity, sosDelay);
                showSosDelay(sosDelay);
            }
        });

        mArrowDownBtn.setOnTouchListener(new OnTouchContinuousListener() {
            @Override
            public void onTouchRepeat(View view) {
                long sosDelay = Prefs.getPassiveSosInterval(mActivity);
                sosDelay -= 1*60*1000; // -1 min
                sosDelay = Math.max(sosDelay, 1*60*1000); // min 1 min
                //DelayedSosService.setSosDelay(mActivity, sosDelay);
                Prefs.setPassiveSosInterval(mActivity, sosDelay);
                showSosDelay(sosDelay);
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null)
        {
            if (bundle.getBoolean(PassiveSosService.ASK_FOR_PASSWORD))
            {
                askForPassword();
            }
        }
    }

    private void askForPassword() {
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
                                checkPassword(userInput.getText().toString());
                                mAlertDialog.dismiss();
                            }
                        });

        mAlertDialog = alertDialogBuilder.create();
        mAlertDialog.show();
        mAlertDialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        userInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    checkPassword(userInput.getText().toString());
                    mAlertDialog.dismiss();
                }
                return true;
            }
        });
    }

    // stops timer or builds dialog with retry/cancel buttons
    private void checkPassword(String password) {
        String savedPassword = Prefs.getPassword(mActivity);
        if (!savedPassword.equals("") && !password.equals(savedPassword))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(R.string.wrong_password);
            builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    askForPassword();
                }
            });
            builder.create().show();
        }
    }

    private void askSosReason() {
        LayoutInflater li = LayoutInflater.from(mActivity);
        View promptsView = li.inflate(R.layout.sos_reason_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // TODO: start PassiveSosService
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

        final EditText userInput = (EditText) promptsView.findViewById(R.id.srd_sos_reason_edit);
        userInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // TODO: start passive SOS service
                    alertDialog.dismiss();
                }
                return true;
            }
        });
    }

//    // builds dialog with password prompt
//    private void askPasswordAndStopTimer() {
//        LayoutInflater li = LayoutInflater.from(mActivity);
//        View promptsView = li.inflate(R.layout.password_dialog, null);
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
//        alertDialogBuilder.setView(promptsView);
//
//        final EditText userInput = (EditText) promptsView.findViewById(R.id.pd_password_edit);
//
//        alertDialogBuilder
//                .setCancelable(false)
//                .setPositiveButton(android.R.string.ok,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                checkPassword(userInput.getText().toString());
//                            }
//                        })
//                .setNegativeButton(android.R.string.cancel,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                dialog.cancel();
//                            }
//                        });
//
//        final AlertDialog alertDialog = alertDialogBuilder.create();
//        alertDialog.show();
//        alertDialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
//
//        userInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    checkPassword(userInput.getText().toString());
//                    alertDialog.dismiss();
//                }
//                return true;
//            }
//        });
//    }
//
//    // stops timer or builds dialog with retry/cancel buttons
//    private void checkPassword(String password) {
//        if (password.equals(Prefs.getPassword(mActivity)))
//        {
//            mActivity.stopService(new Intent(mActivity, DelayedSosService.class));
//            onTimerStop();
//        }
//        else{
//            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
//            builder.setTitle(R.string.wrong_password);
//            builder.setNegativeButton(android.R.string.cancel, null);
//            builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    askPasswordAndStopTimer();
//                }
//            });
//            builder.create().show();
//        }
//    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        mActivity.unregisterReceiver(mBroadcastReceiver);
    }

    private void onTimerStart() {
        mArrowUpBtn.setEnabled(false);
        mArrowDownBtn.setEnabled(false);
        mArrowUpBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_up_inactive));
        mArrowDownBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_down_inactive));
        mTimerBtn.setText(getResources().getString(R.string.stop));
    }

    private void onTimerStop() {
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

            if (action.equals(PassiveSosService.PASSIVE_SOS_PASSWORD)) {
                askForPassword();
            }
        }
    };
}