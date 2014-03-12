package ua.p2psafety.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;

import ua.p2psafety.util.EventManager;
import ua.p2psafety.Notifications;
import ua.p2psafety.data.Prefs;

public class DelayedSosService extends Service {
    public static final String SOS_DELAY_START = "ua.p2psafety.services.DelayedSosService.TimerStart";
    public static final String SOS_DELAY_TICK = "ua.p2psafety.services.DelayedSosService.TimerTick";
    public static final String SOS_DELAY_FINISH = "ua.p2psafety.services.DelayedSosService.TimerFinish";
    public static final String SOS_DELAY_CANCEL = "ua.p2psafety.services.DelayedSosService.TimerCancel";
    public static final String SOS_DELAY_CHANGE = "ua.p2psafety.services.DelayedSosService.TimerChange";

    private static Boolean mTimerOn = false;
    private static long mSosDelay = 0;
    private static long mTimeLeft = 0;

    private static DelayedSosTimer mTimer;

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

            EventManager.getInstance(getApplicationContext()).startSos();
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