package ua.p2psafety;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ua.p2psafety.util.Utils;

public class PowerButtonListener extends BroadcastReceiver{
    final int mPressThreshold = 6; // 6 presses to activate sos
    final int mPressTimeout = 1000; // no more than 1 sec between presses
    final int mVibrationLength = 2000;
    int mPressCount = 0;
    long mLastPressTime = 0;

    @Override
    // user presses button 3 times, waits for vibration
    // and then presses button 3 times more
    public void onReceive(Context context, Intent intent) {
        mainFunctionality(context);
    }

    private void mainFunctionality(Context context) {
        if (SosManager.getInstance(context).isSosStarted())
            return; // Sos is already On, do nothing

        long max_timeout;
        if (mPressCount == 3) {
            max_timeout = mVibrationLength + mPressTimeout;
        } else {
            max_timeout = mPressTimeout;
        }

        //if it is first click or time between each click is less than max_timeout, then pressCount++
        if (mLastPressTime == 0 || mPressCount ==0 ||
                ((System.currentTimeMillis() - mLastPressTime) <= max_timeout))
        {
            ++mPressCount;

            //if current pressCount==3 then we start vibration
            if (mPressCount == 3) {
                Utils.startVibration(context);
            }
        }
        else
        {
            //else mPressCount=0 and we start again all functionality with first time clicked button
            mPressCount = 0;
            mainFunctionality(context);
        }
        //save current time
        mLastPressTime = System.currentTimeMillis();

        if (mPressCount == mPressThreshold) {
            // ATTN: vibrate every time when something's activated by hardware buttons
            // (startSos has vibration by itself though)
            SosManager.getInstance(context).startSos();
            mPressCount = 0;
        }
    }
}
