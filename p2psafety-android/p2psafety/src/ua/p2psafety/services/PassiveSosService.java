package ua.p2psafety.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ua.p2psafety.SosActivity;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.fragments.PassiveSosFragment;

/**
 * Created by Taras Melon on 25.03.14.
 */
public class PassiveSosService extends Service {

    public static final String ASK_FOR_PASSWORD = "ASK_FOR_PASSWORD";
    public static final String PASSIVE_SOS_PASSWORD = "ua.p2psafety.services.PassiveSosService.PassiveSosPassword";
    private static long mPassiveSosInterval;

    private ScheduledExecutorService mExecutor;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mPassiveSosInterval = Prefs.getPassiveSosInterval(this);

        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                openPassiveSosFragment();
       }}, mPassiveSosInterval, mPassiveSosInterval, TimeUnit.MILLISECONDS);

       return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void openPassiveSosFragment() {
        Intent i = new Intent(this, SosActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // fix for navigation bug
        i.putExtra(SosActivity.FRAGMENT_KEY, PassiveSosFragment.class.getName());
        // put parsed data
        i.putExtra(ASK_FOR_PASSWORD, true);
        startActivity(i);
    }

    public static void registerReceiver(Context context, BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PASSIVE_SOS_PASSWORD);
        context.registerReceiver(receiver, filter);
    }
}
