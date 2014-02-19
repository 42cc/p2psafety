package ua.p2psafety;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

import java.util.HashMap;
import java.util.Map;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.sms.MessageResolver;
import ua.p2psafety.sms.MyLocation;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.Utils;

public class SosManager {
    private static SosManager mInstance;
    private Context mContext;
    private Logs logs;

    private Event mEvent;
    private boolean mSosStarted = false;

    private SosManager(Context context) {
        mContext = context;
        mSosStarted = Prefs.getSosStarted(mContext);
        mEvent = Prefs.getEvent(mContext);

        logs = new Logs(context);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (logs != null)
            logs.close();
    }

    public static SosManager getInstance(Context context) {
        if (mInstance == null)
            mInstance = new SosManager(context);
        return mInstance;
    }

    public void startSos() {
        Utils.startVibration(mContext);

        // send SMS and email messages
        MessageResolver resolver = new MessageResolver(mContext);
        resolver.sendMessages();

        // start media recording
        switch (Prefs.getMediaRecordType(mContext)) {
            case 1:
                // record audio
                mContext.startService(new Intent(mContext, AudioRecordService.class));
                break;
            case 2:
                // record video
                mContext.startService(new Intent(mContext, VideoRecordService.class));
                break;
        }

        // show hint in notifications panel
        Notifications.notifSosStarted(mContext);

        // report event to the server
        if (Utils.isServerAuthenticated(mContext))
            serverStartSos();

        // make phone call
        mContext.startService(new Intent(mContext, PhoneCallService.class));

        setSosStarted(true);
    }

    public void stopSos() {
        // stop media recording
        mContext.stopService(new Intent(mContext, AudioRecordService.class));
        mContext.stopService(new Intent(mContext, VideoRecordService.class));

        Notifications.removeNotification(mContext, Notifications.NOTIF_SOS_STARTED_CODE);
        Notifications.notifSosCanceled(mContext);

        // TODO: send "i'm safe now" SMS and email messages (ask if needed)

        // report event to the server
        if (Utils.isFbAuthenticated(mContext))
            serverStopSos();

        mContext.stopService(new Intent(mContext, PhoneCallService.class));

        setSosStarted(false);
    }

    public boolean isSosStarted() {
        return mSosStarted;
    }

    public void setEvent(Event event) {
        mEvent = event;
        Prefs.putEvent(mContext, mEvent);
        if (mEvent == null)
            return;

        if (mEvent.getStatus() == Event.STATUS_ACTIVE)
            mSosStarted = true;
        else
            mSosStarted = false;
    }

    public Event getEvent() {
        return mEvent;
    }

    private void serverStartSos() {
        // if you have no event try to create one on server
        // (you must have an event at this point though)
        if (mEvent == null) {
            NetworkManager.createEvent(mContext,
                    new NetworkManager.DeliverResultRunnable<Event>() {
                        @Override
                        public void deliver(Event event) {
                            if (event != null) {
                                setEvent(event);
                                serverUpdateLocation(); // make this event active
                            }
                        }
                    });
        } else {
            serverUpdateLocation();
        }
    }

    private void serverUpdateLocation() {
        mEvent.setStatus(Event.STATUS_ACTIVE);
        MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                Map data = new HashMap();
                if (location != null)
                    data.put("loc", location);
                data.put("text", Prefs.getMessage(mContext));

                NetworkManager.updateEvent(mContext, data, null);
            }
        };
        MyLocation myLocation = new MyLocation(logs);
        myLocation.getLocation(mContext, locationResult);
    }

    private void serverStopSos() {
        if (mEvent != null)
            mEvent.setStatus(Event.STATUS_FINISHED);

        NetworkManager.createEvent(mContext,
                new NetworkManager.DeliverResultRunnable<Event>() {
                    @Override
                    public void deliver(Event event) {
                        setEvent(event);
                    }
                });
    }

    private void setSosStarted(boolean started) {
        mSosStarted = started;
        Prefs.putSosStarted(mContext, mSosStarted);
    }
}
