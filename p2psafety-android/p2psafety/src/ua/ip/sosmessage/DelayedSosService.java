package ua.ip.sosmessage;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.widget.Toast;

import ua.ip.sosmessage.data.Prefs;

public class DelayedSosService extends Service {
    public static final String SOS_DELAY_START = "ua.ip.sosmessage.DelayedSosService.TimerStart";
    public static final String SOS_DELAY_TICK = "ua.ip.sosmessage.DelayedSosService.TimerTick";
    public static final String SOS_DELAY_FINISH = "ua.ip.sosmessage.DelayedSosService.TimerFinish";
    public static final String SOS_DELAY_CANCEL = "ua.ip.sosmessage.DelayedSosService.TimerCancel";
    public static final String SOS_DELAY_CHANGE = "ua.ip.sosmessage.DelayedSosService.TimerChange";

    private static Boolean mTimerOn = false;
    private static long mSosDelay = 0;
    private static long mTimeLeft = 0;

    private static DelayedSosTimer mTimer;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void onCreate(Context context) {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mTimerOn) {
            // let our notification manager know when things happen
            registerReceiver(getApplicationContext(), new Notifications());

            mTimeLeft = mSosDelay;
            mTimer = new DelayedSosTimer(mSosDelay, 1000);
            mTimer.start();
            mTimerOn = true;

            Intent i = new Intent(SOS_DELAY_START);
            sendBroadcast(i);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public static void registerReceiver(Context context, BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DelayedSosService.SOS_DELAY_START);
        filter.addAction(DelayedSosService.SOS_DELAY_TICK);
        filter.addAction(DelayedSosService.SOS_DELAY_FINISH);
        filter.addAction(DelayedSosService.SOS_DELAY_CANCEL);
        context.registerReceiver(receiver, filter);
    }

    private class DelayedSosTimer extends CountDownTimer {
        public DelayedSosTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            Intent i = new Intent(SOS_DELAY_FINISH);
            sendBroadcast(i);

            Toast.makeText(getApplicationContext(), "Here we send SOS message", Toast.LENGTH_LONG).show();
            mTimerOn = false;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mTimeLeft = millisUntilFinished;
            Intent i = new Intent(SOS_DELAY_TICK);
            sendBroadcast(i);
        }
    }

    @Override
    public void onDestroy() {
        if (mTimerOn) {
            mTimer.cancel();
            mTimerOn = false;
            Intent i = new Intent(SOS_DELAY_CANCEL);
            sendBroadcast(i);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public static void setSosDelay(Context context, long val) {
        mSosDelay = val;
        Prefs.putSosDelay(context, mSosDelay);

        // send broadcast that delay time changed
        Intent i = new Intent(SOS_DELAY_CHANGE);
        context.sendBroadcast(i);

    }

    public static long getSosDelay(Context context) {
        if (mSosDelay == 0) {
            mSosDelay = Prefs.getSosDelay(context);
        }
        return mSosDelay;
    }

    public static boolean isTimerOn() {
        return mTimerOn;
    }

    public static long getTimeLeft() {
        return mTimeLeft;
    }
}