package ua.p2psafety;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PowerButtonListener extends BroadcastReceiver{
    final int mPressThreshold = 3; // 3 presses to activate sos
    final int mPressTimeout = 1000; // no more than 1sec between presses
    int mPressCount = 0;
    long mLastPressTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SosManager.getInstance(context).isSosStarted())
            return; // Sos is already On, do nothing

        if (System.currentTimeMillis() - mLastPressTime > mPressTimeout)
           mPressCount = 1;
        else
           ++mPressCount;
        mLastPressTime = System.currentTimeMillis();

        if (mPressCount == mPressThreshold) {
            SosManager.getInstance(context).startSos();
            mPressCount = 0;
        }
    }
}
