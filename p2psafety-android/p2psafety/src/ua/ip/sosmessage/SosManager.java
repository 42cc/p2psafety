package ua.ip.sosmessage;

import android.content.Context;
import android.content.Intent;

import ua.ip.sosmessage.sms.MessageResolver;

public class SosManager {
    private static SosManager mInstance;

    private boolean mSosStarted;

    private Context mContext;

    private SosManager(Context context) {
        mContext = context;

        // TODO: load mSosStarted from Prefs
        mSosStarted = false;
    }

    public static SosManager getInstance(Context context) {
        if (mInstance == null)
            mInstance = new SosManager(context);
        return mInstance;
    }

    public void startSos() {
        // send SMS and email messages
        MessageResolver resolver = new MessageResolver(mContext, false);
        resolver.sendMessages();

        // start media recording
        mContext.startService(new Intent(mContext, AudioRecordService.class));

        // show hint in notifications panel
        Notifications.notifSosStarted(mContext);

        // TODO: contact server

        // TODO: save mSosStarted to Prefs
        mSosStarted = true;
    }

    public void stopSos() {
        // stop media recording
        mContext.stopService(new Intent(mContext, AudioRecordService.class));

        // TODO: remove notification

        // TODO: contact server

        // TODO: save mSosStarted to Prefs
        mSosStarted = false;
    }

    public boolean isSosStarted() {
        return mSosStarted;
    }
}
