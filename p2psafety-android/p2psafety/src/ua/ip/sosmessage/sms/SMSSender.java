package ua.ip.sosmessage.sms;

import android.content.Context;
import android.telephony.SmsManager;

/**
 * Created by ihorpysmennyi on 12/7/13.
 */
public class SMSSender {
    public static void send(String number, String message, Context context) {

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, message, null, null);

    }
}
