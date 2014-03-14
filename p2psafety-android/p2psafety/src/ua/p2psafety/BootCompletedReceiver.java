package ua.p2psafety;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ua.p2psafety.services.LocationService;
import ua.p2psafety.services.PowerButtonService;
import ua.p2psafety.services.XmppService;

/**
 * Created by Taras Melon on 14.03.14.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, PowerButtonService.class));
        context.startService(new Intent(context, XmppService.class));
        context.startService(new Intent(context, LocationService.class));
    }
}
