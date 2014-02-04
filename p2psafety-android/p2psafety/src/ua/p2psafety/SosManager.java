package ua.p2psafety;

import android.content.Context;
import android.content.Intent;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.sms.MessageResolver;
import ua.p2psafety.util.Utils;

public class SosManager {
    private static SosManager mInstance;
    private Context mContext;

    private Event mEvent;
    private boolean mSosStarted = false;

    private SosManager(Context context) {
        mContext = context;
    }

    public static SosManager getInstance(Context context) {
        if (mInstance == null)
            mInstance = new SosManager(context);
        return mInstance;
    }

    public void startSos() {
        Utils.startVibration(mContext);

        // send SMS and email messages
//        MessageResolver resolver = new MessageResolver(mContext);
//        resolver.sendMessages();
//
//        // start media recording
//        switch (Prefs.getMediaRecordType(mContext)) {
//            case 1:
//                // record audio
//                mContext.startService(new Intent(mContext, AudioRecordService.class));
//                break;
//            case 2:
//                // record video
//                mContext.startService(new Intent(mContext, VideoRecordService.class));
//                break;
//        }

        // show hint in notifications panel
        Notifications.notifSosStarted(mContext);

        // report event to the server
        NetworkManager.updateEvent(mContext, null, null);

        mSosStarted = true;
    }

    public void stopSos() {
        // stop media recording
        mContext.stopService(new Intent(mContext, AudioRecordService.class));
        mContext.stopService(new Intent(mContext, VideoRecordService.class));

        Notifications.removeNotification(mContext, Notifications.NOTIF_SOS_STARTED_CODE);
        Notifications.notifSosCanceled(mContext);

        // TODO: contact server

        // TODO: send "i'm safe now" SMS and email messages (ask if needed)

        // TODO: save mSosStarted to Prefs
        mSosStarted = false;
    }

    public boolean isSosStarted() {
        return mSosStarted;
    }

    public void setEvent(Event event) {
        mEvent = event;
        if (mEvent.getStatus() == Event.STATUS_ACTIVE)
            mSosStarted = true;
        else
            mSosStarted = false;
    }

    public Event getEvent() {
        return mEvent;
    }
}
