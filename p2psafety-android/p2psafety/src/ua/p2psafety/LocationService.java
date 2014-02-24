package ua.p2psafety;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.sms.MyLocation;
import ua.p2psafety.util.Logs;

public class LocationService extends Service {
    static final long TIME_INTERVAL = 5*60*1000; // 5 min
    static final long DISTANCE_INTERVAL = 500; // 500 meters

    Location mLastLoc = null;
    Location mCurrentLoc = null;
    long mLastSendTime  = 0;

    ScheduledExecutorService mExecutor;

    Logs mLogs;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLogs = new Logs(this);

        initValues();

        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                getCurrentLoc();
                if (System.currentTimeMillis() - mLastSendTime > TIME_INTERVAL ||
                    mLastLoc.distanceTo(mCurrentLoc) > DISTANCE_INTERVAL)
                {
                    sendLocation();
                    mLastLoc = mCurrentLoc;
                    mLastSendTime = System.currentTimeMillis();
                }
            }
        }, 20*1000, 20*1000, TimeUnit.MILLISECONDS); // check every 20 sec if we need to send coords

        return Service.START_STICKY;
    }

    private void initValues() {
        mLastSendTime  = 0;

        mLastLoc = new Location("");
        mLastLoc.setLatitude(0);
        mLastLoc.setLongitude(0);

        mCurrentLoc = new Location("");
        mCurrentLoc.setLatitude(0);
        mCurrentLoc.setLongitude(0);
    }

    private void getCurrentLoc() {
        MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                mCurrentLoc = location;
            }
        };
        MyLocation myLocation = new MyLocation(mLogs);
        myLocation.getLocation(getApplicationContext(), locationResult);
    }

    private void sendLocation() {
        Map data = new HashMap();
        if (mCurrentLoc != null)
            data.put("loc", mCurrentLoc);
        data.put("text", "");
        NetworkManager.updateEvent(this, data, new NetworkManager.DeliverResultRunnable<Boolean>() {
            @Override
            public void deliver(Boolean aBoolean) {
                // event updated
            }
        });
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdown(); // stop sending locs
        if (mLogs != null)
            mLogs.close();
    }
}
