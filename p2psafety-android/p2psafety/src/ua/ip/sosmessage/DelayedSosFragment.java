package ua.ip.sosmessage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import ua.ip.sosmessage.message.MessageFragment;
import ua.ip.sosmessage.setphones.SetPhoneFragment;
import ua.ip.sosmessage.sms.MessageResolver;

public class DelayedSosFragment extends Fragment {
    TextView mTimerText;
    Button mTimerBtn;
    ImageButton mArrowUpBtn, mArrowDownBtn;

    CountDownTimer mTimer;
    Boolean mTimerOn = false;
    long mSosDelay = 1*60*1000; // 2 min
    long mTimeLeft;

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

        return view;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        mTimerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimerOn) {
                    stopTimer();
                } else {
                    startTimer();
                }
            }
        });

        mArrowUpBtn.setOnTouchListener(new OnTouchContinuousListener() {
            @Override
            public void onTouchRepeat(View view) {
                if (!mTimerOn) {
                    mSosDelay += 1*60*1000; // +1 min
                    mSosDelay = Math.min(mSosDelay, 120 * 60 * 1000);
                    showSosDelay();
                }
            }
        });

        mArrowDownBtn.setOnTouchListener(new OnTouchContinuousListener() {
            @Override
            public void onTouchRepeat(View view) {
                if (!mTimerOn) {
                    mSosDelay -= 1*60*1000; // -1 min
                    mSosDelay = Math.max(mSosDelay, 1*60*1000);
                    showSosDelay();
                }
            }
        });

        stopTimer();
    }

    private void startTimer() {
        mTimer = new CountDownTimer(mSosDelay, 1000) {
            @Override
            public void onFinish() {
                stopTimer();

                new AlertDialog.Builder(mActivity)
                        .setMessage("Here we send SOS message")
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }

            public void onTick(long millisUntilFinished) {
                mTimeLeft = millisUntilFinished;
                mTimerText.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toSeconds(mTimeLeft) / 60,
                        TimeUnit.MILLISECONDS.toSeconds(mTimeLeft) % 60));
            }
        };
        mTimer.start();
        mTimerOn = true;

        mArrowUpBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_up_inactive));
        mArrowDownBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_down_inactive));
        mTimerBtn.setText("Стоп");
    }

    private void stopTimer() {
        if (mTimer != null)
            mTimer.cancel();
        mTimerOn = false;

        mArrowUpBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_up));
        mArrowDownBtn.setImageDrawable(
                getResources().getDrawable(R.drawable.arrow_down));
        showSosDelay();
        mTimerBtn.setText("Старт");
    }
      
    private void showSosDelay() {
        mTimerText.setText(String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toSeconds(mSosDelay) / 60,
                TimeUnit.MILLISECONDS.toSeconds(mSosDelay) % 60));
    }
}