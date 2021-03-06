package ua.p2psafety.util;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import ua.p2psafety.Notifications;
import ua.p2psafety.SosActivity;
import ua.p2psafety.json.Event;
import ua.p2psafety.services.AudioRecordService;
import ua.p2psafety.services.LocationService;
import ua.p2psafety.services.PassiveSosService;
import ua.p2psafety.services.PhoneCallService;
import ua.p2psafety.services.VideoRecordService;
import ua.p2psafety.services.XmppService;
import ua.p2psafety.data.Prefs;

public class EventManager {
    private static EventManager mInstance;
    private Context mContext;
    private Logs logs;

    private Event mEvent;
    private boolean mSosStarted = false;

    private EventManager(Context context) {
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

    public static EventManager getInstance(Context context) {
        if (mInstance == null)
            mInstance = new EventManager(context);
        mInstance.mContext = context;
        return mInstance;
    }

    public void startSos() {

        if (isPassiveSosStarted() && !Prefs.isActiveTrue(mContext))
        {
            mContext.stopService(new Intent(mContext, PassiveSosService.class));
            setPassiveSosStarted(false);
            Toast.makeText(mContext, "Passive SOS stopped", Toast.LENGTH_SHORT).show();
            Prefs.putActiveTrue(mContext, true);
        }

        logs.info("EventManager. StartSos()");
        logs.info("EventManager. StartSos. Start vibration");
        Utils.startVibration(mContext);

        // stop listening XMPP
        mContext.stopService(new Intent(mContext, XmppService.class));
        XmppService.processing_event = false;

        // send SMS and email messages
        logs.info("EventManager. StartSos. Send SMS and mail messages");
        MessageResolver resolver = new MessageResolver(mContext);
        resolver.sendMessages();

        // start media recording
        logs.info("EventManager. StartSos. Check if we need to record media");
        switch (Prefs.getMediaRecordType(mContext)) {
            case 1:
                // record audio
                logs.info("EventManager. StartSos. Start Audio recording");
                mContext.startService(new Intent(mContext, AudioRecordService.class));
                break;
            case 2:
                // record video
                logs.info("EventManager. StartSos. Start Video recording");
                mContext.startService(new Intent(mContext, VideoRecordService.class));
                break;
        }

        // show hint in notifications panel
        logs.info("EventManager. StartSos. Show notification");
        Notifications.notifSosStarted(mContext);

        // report event to the server
        if (Utils.isServerAuthenticated(mContext)) {
            logs.info("EventManager. StartSos. User is authenticated at server. Sendng SOS request");
            serverStartSos();
        }
        else {
            logs.info("EventManager. StartSos. User is NOT authenticated at server.");
            Utils.setLoading(mContext, false);
        }

        // make phone call
        logs.info("EventManager. StartSos. Making phone call");
        mContext.startService(new Intent(mContext, PhoneCallService.class));

        setSosStarted(true);
    }

    public void stopSos() {
        logs.info("SosManager. StopSos()");

        // stop media recording
        logs.info("SosManager. StopSos. Stop Media recording");
        mContext.stopService(new Intent(mContext, AudioRecordService.class));
        mContext.stopService(new Intent(mContext, VideoRecordService.class));

        logs.info("SosManager. StopSos. Changing Notifications");
        Notifications.removeNotification(mContext, Notifications.NOTIF_SOS_STARTED_CODE);
        Notifications.notifSosCanceled(mContext);

        // TODO: send "i'm safe now" SMS and email messages (ask if needed)

        // report event to the server
        if (Utils.isServerAuthenticated(mContext)) {
            logs.info("SosManager. StopSos. User is authenticated at server. Sendng stop SOS request");
            createNewEvent();
        } else {
            logs.info("SosManager. StopSos. User is NOT authenticated at server.");
            Utils.setLoading(mContext, false);
        }

        logs.info("SosManager. StopSos. Stop PhoneCall and Location services");

        // start listening xmpp
        mContext.startService(new Intent(mContext, XmppService.class));

        setSosStarted(false);
    }

    public boolean isSosStarted() {
        return Prefs.getSosStarted(mContext);
    }

    public boolean isPassiveSosStarted(){
        return Prefs.isPassiveSosStarted(mContext);
    }

    public void setPassiveSosStarted(boolean isStarted)
    {
        Prefs.setPassiveSosStarted(mContext, isStarted);
    }

    public boolean isSupportStarted() {
        if (mEvent != null && mEvent.getType() == Event.TYPE_SUPPORT &&
            mEvent.getStatus() == Event.STATUS_ACTIVE)
        {
            return true;
        } else
            return false;
    }

    public boolean isEventActive() {
        if (mEvent != null && mEvent.getStatus() == Event.STATUS_ACTIVE)
            return true;
        else
            return false;
    }

    public void setEvent(Event event) {
        mEvent = event;
        Prefs.putEvent(mContext, mEvent);
        if (mEvent == null)
            return;

        logs.info("SosManager. Set new event: " + event.getId());

        if (mEvent.getStatus() == Event.STATUS_ACTIVE &&
            mEvent.getType() == Event.TYPE_VICTIM)
        {
            mSosStarted = true;
        } else
            mSosStarted = false;
    }

    public synchronized Event getEvent() throws Exception {
        if (mEvent != null) return mEvent;
        else throw new NullPointerException();
    }

    public void serverStartSos() {
        // if you have no event try to create one on server
        // (you must have an event at this point though)
        if (mEvent == null || mEvent.getType() == Event.TYPE_SUPPORT) {
            logs.info("EventManager. StartSos. We don't have Victim event, trying to create one.");
            NetworkManager.createEvent(mContext,
                    new NetworkManager.DeliverResultRunnable<Event>() {
                        @Override
                        public void onError(int errorCode) {
                            super.onError(errorCode);

                            Utils.setLoading(mContext, false);
                        }

                        @Override
                        public void deliver(Event event) {
                            if (event != null) {
                                logs.info("EventManager. StartSos. Event created: " +
                                    event.getId());
                                setEvent(event);
                                logs.info("EventManager. StartSos. Activating event");
                                serverActivateSos(); // make this event active
                            } else {
                                logs.info("EventManager. StartSos. We were unable to create event");
                                Utils.setLoading(mContext, false);
                            }
                        }
                    });
        } else {
            logs.info("EventManager. StartSos. We have event, activating it");
            serverActivateSos();
        }
    }

    public void serverPassiveStartSos() {
        Location location = LocationService.locationListener.getLastLocation(false);
        SosActivity.mLogs.info("EventManager. StartSos. LocationResult");
        Map data = new HashMap();
        if (location != null) {
            SosActivity.mLogs.info("EventManager. StartSos. Location is not null");
            data.put("loc", location);
        } else {
            SosActivity.mLogs.info("EventManager. StartSos. Location is NULL");
        }
        data.put("text", Prefs.getPassiveMessage(mContext));

        NetworkManager.updateEvent(mContext, data, new NetworkManager.DeliverResultRunnable<Boolean>() {
            @Override
            public void deliver(Boolean aBoolean) {
                // start sending location updates
                SosActivity.mLogs.info("EventManager. StartSos. Event activated. Starting LocationService");
                Utils.setLoading(mContext, false);
            }
        });
    }

    public void serverPassiveStartSos(final Runnable moreOperations) {
        Location location = LocationService.locationListener.getLastLocation(false);
        SosActivity.mLogs.info("EventManager. StartSos. LocationResult");
        Map data = new HashMap();
        if (location != null) {
            SosActivity.mLogs.info("EventManager. StartSos. Location is not null");
            data.put("loc", location);
        } else {
            SosActivity.mLogs.info("EventManager. StartSos. Location is NULL");
        }
        data.put("text", Prefs.getPassiveMessage(mContext));

        NetworkManager.updateEvent(mContext, data, new NetworkManager.DeliverResultRunnable<Boolean>() {
            @Override
            public void deliver(Boolean aBoolean) {
                // start sending location updates
                SosActivity.mLogs.info("EventManager. StartSos. Event activated. Starting LocationService");
                moreOperations.run();
            }
        });
    }

    public void serverStartSos(final Runnable moreOperations) {
        // if you have no event try to create one on server
        // (you must have an event at this point though)
        if (mEvent == null || mEvent.getType() == Event.TYPE_SUPPORT) {
            logs.info("EventManager. StartSos. We don't have Victim event, trying to create one.");
            NetworkManager.createEvent(mContext,
                    new NetworkManager.DeliverResultRunnable<Event>() {
                        @Override
                        public void deliver(Event event) {
                            if (event != null) {
                                logs.info("EventManager. StartSos. Event created: " +
                                        event.getId());
                                setEvent(event);
                                logs.info("EventManager. StartSos. Activating event");
                                serverActivateSos(moreOperations); // make this event active
                            } else {
                                logs.info("EventManager. StartSos. We were unable to create event");
                                moreOperations.run();
                            }
                        }
                    });
        } else {
            logs.info("EventManager. StartSos. We have event, activating it");
            serverActivateSos(moreOperations);
        }
    }

    private void serverActivateSos() {
        LocationService.AWLocationListener listener = LocationService.locationListener;
        if (listener == null)
            return;

        mEvent.setStatus(Event.STATUS_ACTIVE);

        Location location = listener.getLastLocation(false);
        logs.info("EventManager. StartSos. LocationResult");
        Map data = new HashMap();
        if (location != null) {
            logs.info("EventManager. StartSos. Location is not null");
            data.put("loc", location);
        } else {
            logs.info("EventManager. StartSos. Location is NULL");
        }
        data.put("text", Prefs.getMessage(mContext));

        NetworkManager.updateEvent(mContext, data, new NetworkManager.DeliverResultRunnable<Boolean>() {
            @Override
            public void onError(int errorCode) {
                super.onError(errorCode);

                Utils.setLoading(mContext, false);
            }

            @Override
            public void deliver(Boolean aBoolean) {
                // start sending location updates
                logs.info("EventManager. StartSos. Event activated. Starting LocationService");
                Utils.setLoading(mContext, false);
            }
        });
    }

    private void serverActivateSos(final Runnable moreOperations) {
        mEvent.setStatus(Event.STATUS_ACTIVE);

        Location location = LocationService.locationListener.getLastLocation(false);
        logs.info("EventManager. StartSos. LocationResult");
        Map data = new HashMap();
        if (location != null) {
            logs.info("EventManager. StartSos. Location is not null");
            data.put("loc", location);
        } else {
            logs.info("EventManager. StartSos. Location is NULL");
        }
        data.put("text", Prefs.getMessage(mContext));

        NetworkManager.updateEvent(mContext, data, new NetworkManager.DeliverResultRunnable<Boolean>() {
            @Override
            public void deliver(Boolean aBoolean) {
                // start sending location updates
                logs.info("EventManager. StartSos. Event activated. Starting LocationService");
                moreOperations.run();
            }
        });
    }

    public void createNewEvent() {
        if (mEvent != null)
            mEvent.setStatus(Event.STATUS_FINISHED);

        NetworkManager.createEvent(mContext,
                new NetworkManager.DeliverResultRunnable<Event>() {
                    @Override
                    public void onError(int errorCode) {
                        super.onError(errorCode);

                        Utils.setLoading(mContext, false);
                    }

                    @Override
                    public void deliver(Event event) {
                        setEvent(event);
                        Utils.setLoading(mContext, false);
                    }
                });
    }

    public void createNewEvent(final Runnable moreOperations) {
        if (mEvent != null)
            mEvent.setStatus(Event.STATUS_FINISHED);

        NetworkManager.createEvent(mContext,
                new NetworkManager.DeliverResultRunnable<Event>() {
                    @Override
                    public void onError(int errorCode) {
                        super.onError(errorCode);

                        Utils.setLoading(mContext, false);
                    }

                    @Override
                    public void deliver(Event event) {
                        setEvent(event);
                        moreOperations.run();
                        Utils.setLoading(mContext, false);
                    }
                });
    }

    private void setSosStarted(boolean started) {
        mSosStarted = started;
        Prefs.putSosStarted(mContext, mSosStarted);
    }
}
