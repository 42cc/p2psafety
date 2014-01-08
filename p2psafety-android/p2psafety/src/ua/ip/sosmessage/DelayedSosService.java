package ua.ip.sosmessage;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class DelayedSosService extends Service {
    public static final String SOS_DELAY_TICK = "ua.ip.sosmessage.DelayedSosService.TimerTick";
    public static final String SOS_DELAY_FINISH = "ua.ip.sosmessage.DelayedSosService.TimerFinish";
    public static final String SOS_DELAY_CANCEL = "ua.ip.sosmessage.DelayedSosService.TimerCancel";

    public static Boolean mTimerOn = false;
    public static long mSosDelay = 2*60*1000; // 2 min
    public static long mTimeLeft = 0;

    static DelayedSosTimer mTimer;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mTimerOn) {
            mTimer = new DelayedSosTimer(mSosDelay, 1000);
            mTimer.start();
            mTimerOn = true;
        }

        return super.onStartCommand(intent, flags, startId);
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

}
