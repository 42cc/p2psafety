package ua.p2psafety.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ua.p2psafety.SosActivity;
import ua.p2psafety.json.Event;
import ua.p2psafety.services.XmppService;
import ua.p2psafety.util.EventManager;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.NetworkManager;
import ua.p2psafety.util.Utils;

/**
 * Created by Taras Melon on 27.03.14.
 */
public class NetworkStateChangedReceiver extends BroadcastReceiver {

    private Context mContext;
    private Logs mLogs;

    private Runnable mUnsetLoading = new Runnable() {
        @Override
        public void run() {
            unsetLoading();
        }
    };

    private NetworkManager.DeliverResultRunnable<Boolean> mUnsetLoadingOnDeliver = new NetworkManager.DeliverResultRunnable<Boolean>()
    {
        @Override
        public void deliver(Boolean aBoolean) {
            super.deliver(aBoolean);

            unsetLoading();
        }

        @Override
        public void onError(int errorCode) {
            super.onError(errorCode);

            unsetLoading();
        }
    };

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Utils.isServerAuthenticated(context))
        {
            mLogs = new Logs(context);
            if (Utils.isNetworkConnected(context, mLogs))
            {
                mLogs.info("NetworkStateChangedReceiver. We are starting");
                mContext = context;
                setLoading();
                final EventManager eventManager = EventManager.getInstance(context);
                try {
                    mLogs.info("NetworkStateChangedReceiver. execute getInfoAboutEvent()");
                    NetworkManager.getInfoAboutEvent(context, eventManager.getEvent().getId(),
                            new NetworkManager.DeliverResultRunnable<Event>() {
                        @Override
                        public void deliver(Event event) {
                            if (event != null) {
                                NetworkManager.init(context);

                                mLogs.info("NetworkStateChangedReceiver. getInfoAboutEvent() " +
                                        "returned good event");

                                if (eventManager.isSosStarted())
                                {
                                    mLogs.info("NetworkStateChangedReceiver. Sos started on device");
                                    mLogs.info("NetworkStateChangedReceiver. Sos status on server is "
                                            + event.getStatus());
                                    mLogs.info("NetworkStateChangedReceiver. Sos type on server is "
                                            + event.getType());
                                    if (event.getStatus().equals(Event.STATUS_ACTIVE))
                                    {
                                        if (event.getType().equals(Event.TYPE_SUPPORT))
                                        {
                                            mLogs.info("NetworkStateChangedReceiver. Execute serverStartSos()");
                                            eventManager.serverStartSos(mUnsetLoading);
                                        }
                                        else if (event.getType().equals(Event.TYPE_VICTIM))
                                        {
                                            mLogs.info("NetworkStateChangedReceiver. Execute unsetLoading()");
                                            unsetLoading();
                                            //good enough
                                        }
                                    }
                                    else
                                    {
                                        mLogs.info("NetworkStateChangedReceiver. Execute serverStartSos()");
                                        eventManager.serverStartSos(mUnsetLoading);
                                    }
                                }
                                else
                                {
                                    if (eventManager.isSupportStarted())
                                    {
                                        mLogs.info("NetworkStateChangedReceiver. Support started on device");
                                        mLogs.info("NetworkStateChangedReceiver. Support status on server is "
                                                + event.getStatus());
                                        mLogs.info("NetworkStateChangedReceiver. Support type on server is "
                                                + event.getType());
                                        if (event.getStatus().equals(Event.STATUS_ACTIVE))
                                        {
                                            if (event.getType().equals(Event.TYPE_SUPPORT))
                                            {
                                                mLogs.info("NetworkStateChangedReceiver. Execute unsetLoading()");
                                                unsetLoading();
                                                //good enough
                                            }
                                            else if (event.getType().equals(Event.TYPE_VICTIM))
                                            {
                                                //can not be handle in real life, because we can not
                                                //create situation, when on mobile device we turn on support
                                                // mode without internet, but on server you are victim

                                                //but let code remain
                                                mLogs.info("NetworkStateChangedReceiver. Execute createnewEvent() and supportEvent()");
                                                eventManager.createNewEvent(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        NetworkManager.supportEvent(context,
                                                                XmppService.VICTIM_DATA.getSupporterUrl(),
                                                                mUnsetLoadingOnDeliver);
                                                    }
                                                });
                                            }
                                        }
                                        else
                                        {
                                            //can not be handle in real life, because we can not
                                            //create situation, when on mobile device we turn on support
                                            // mode without internet, but on server you are victim

                                            //but let code remain
                                            mLogs.info("NetworkStateChangedReceiver. Execute supportEvent()");
                                            NetworkManager.supportEvent(context,
                                                    XmppService.VICTIM_DATA.getSupporterUrl(),
                                                    mUnsetLoadingOnDeliver);
                                        }
                                    }
                                    else
                                    {
                                        mLogs.info("NetworkStateChangedReceiver. Sos and support did not start");
                                        mLogs.info("NetworkStateChangedReceiver. Support status on server is "
                                                + event.getStatus());
                                        mLogs.info("NetworkStateChangedReceiver. Support type on server is "
                                                + event.getType());
                                        if (event.getStatus().equals(Event.STATUS_ACTIVE))
                                        {
                                            mLogs.info("NetworkStateChangedReceiver. Execute createNewEvent()");
                                            eventManager.createNewEvent(mUnsetLoading);
                                        }
                                        else
                                        {
                                            mLogs.info("NetworkStateChangedReceiver. Execute unsetLoading()");
                                            unsetLoading();
                                            //good enough
                                        }
                                    }
                                }
                            }
                            else
                            {
                                mLogs.info("NetworkStateChangedReceiver. getInfoAboutEvent() returned null");
                            }
                        }
                    });
                } catch (Exception e) {
                    eventManager.createNewEvent(mUnsetLoading);
                }
            }
        }
    }

    private void setLoading() {
        Intent intent = new Intent();
        intent.setAction(SosActivity.ACTION_SET_LOADING);
        mContext.sendBroadcast(intent);
        mLogs.info("NetworkStateChangedReceiver. Set loading");
    }

    private void unsetLoading() {
        Intent intent = new Intent();
        intent.setAction(SosActivity.ACTION_UNSET_LOADING);
        mContext.sendBroadcast(intent);
        mLogs.info("NetworkStateChangedReceiver. Unset loading");
    }
}
