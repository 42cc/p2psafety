package ua.p2psafety;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.DateFormat;

import ua.p2psafety.util.Utils;

public class PowerButtonListener extends BroadcastReceiver{
    final int mPressThreshold = 6; // 6 presses to activate sos
    final int mPressTimeout = 6000; // no more than 1 sec between presses
    final int mVibrationLength = 2000;
    int mPressCount = 0;
    long mLastPressTime = 0;

    @Override
    // user presses button 3 times, waits for vibration
    // and then presses button 3 times more
    public void onReceive(Context context, Intent intent) {
        if (SosManager.getInstance(context).isSosStarted())
            return; // Sos is already On, do nothing

        if (mPressCount == 2) {
            Utils.startVibration(context);
        }

        long max_timeout, min_timeout;
        if (mPressCount == 3) {
            max_timeout = mVibrationLength + mPressTimeout;
            min_timeout = mVibrationLength;
        } else {
            max_timeout = mPressTimeout;
            min_timeout = 500;
        }

        if (System.currentTimeMillis() - mLastPressTime < min_timeout)
            // don't let presses be too fast; let it be "calm" presses
            // I hope this helps against automatic events
            return;
        else if (System.currentTimeMillis() - mLastPressTime > max_timeout)
           mPressCount = 1;
        else
           ++mPressCount;
        mLastPressTime = System.currentTimeMillis();

        Log.i("OnReceive", "count: " + mPressCount + "  last: " + mLastPressTime);

        if (mPressCount == mPressThreshold) {
            // ATTN: vibrate every time when something's activated by hardware buttons
            // (startSos has vibration by itself though)
            SosManager.getInstance(context).startSos();
            mPressCount = 0;
        }
    }
}
