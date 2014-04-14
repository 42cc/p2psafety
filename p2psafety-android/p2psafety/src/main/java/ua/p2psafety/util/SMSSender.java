package ua.p2psafety.util;

import android.content.Context;
import android.telephony.SmsManager;

import java.util.ArrayList;

/**
 * Created by ihorpysmennyi on 12/7/13.
 */
public class SMSSender {
    public static void send(String number, String message, Context context) {
        MessageResolver.LOGS.info("SMSSender. Sending SMS. Phone: " + number);
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> msgStringArray = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(number, null, msgStringArray, null, null);
        } catch (Exception e) {
            MessageResolver.LOGS.error("Can't send sms", e);
        }
    }
}
