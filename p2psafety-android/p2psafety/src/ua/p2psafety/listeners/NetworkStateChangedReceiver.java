package ua.p2psafety.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Utils.isServerAuthenticated(context))
        {
            if (Utils.isNetworkConnected(context, new Logs(context)))
            {
                final EventManager eventManager = EventManager.getInstance(context);
                try {
                    NetworkManager.getInfoAboutEvent(context, eventManager.getEvent().getId(),
                            new NetworkManager.DeliverResultRunnable<Event>() {
                        @Override
                        public void deliver(Event event) {
                            if (event != null) {
                                NetworkManager.init(context);

                                if (eventManager.isSosStarted())
                                {
                                    if (event.getStatus().equals(Event.STATUS_ACTIVE))
                                    {
                                        if (event.getType().equals(Event.TYPE_SUPPORT))
                                        {
                                            eventManager.serverStartSos();
                                        }
                                        else if (event.getType().equals(Event.TYPE_VICTIM))
                                        {
                                            //good enough
                                        }
                                    }
                                    else
                                    {
                                        eventManager.serverStartSos();
                                    }
                                }
                                else
                                {
                                    if (eventManager.isSupportStarted())
                                    {
                                        if (event.getStatus().equals(Event.STATUS_ACTIVE))
                                        {
                                            if (event.getType().equals(Event.TYPE_SUPPORT))
                                            {
                                                //good enough
                                            }
                                            else if (event.getType().equals(Event.TYPE_VICTIM))
                                            {
                                                //can not be handle in real life, because we can not
                                                //create situation, when on mobile device we turn on support
                                                // mode without internet, but on server you are victim

                                                //but let code remain
                                                eventManager.createNewEvent(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        NetworkManager.supportEvent(context, XmppService.VICTIM_DATA.getSupporterUrl(), null);
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
                                            NetworkManager.supportEvent(context, XmppService.VICTIM_DATA.getSupporterUrl(), null);
                                        }
                                    }
                                    else
                                    {
                                        if (event.getStatus().equals(Event.STATUS_ACTIVE))
                                        {
                                            eventManager.createNewEvent();
                                        }
                                        else
                                        {
                                            //good enough
                                        }
                                    }
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    eventManager.createNewEvent();
                }
            }
        }
    }
}
