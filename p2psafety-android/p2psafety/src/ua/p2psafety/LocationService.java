/*
package ua.p2psafety;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.sms.MyLocation;
import ua.p2psafety.util.Logs;

public class LocationService extends Service {
    static final long TIME_INTERVAL = 2*60*1000; // 2 min
    static final long DISTANCE_INTERVAL = 50; // 50 meters

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
        mLogs.info("LocationService.onStartCommand()");

        initValues();

        mLogs.info("LocationService. Sheduling location sending");
        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                mLogs.info("LocationService. Checking if we need to send new location");
                getCurrentLoc();
                if (System.currentTimeMillis() - mLastSendTime > TIME_INTERVAL ||
                    mLastLoc.distanceTo(mCurrentLoc) > DISTANCE_INTERVAL)
                {
                    mLogs.info("LocationService. Trying to send new location");
                    sendLocation();
                    mLastLoc = mCurrentLoc;
                    mLastSendTime = System.currentTimeMillis();
                } else {
                    mLogs.info("LocationService. No need");
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
        mLogs.info("LocationService. Getting current location");
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
        if (mCurrentLoc != null) {
            mLogs.info("LocationService. We have location");
            data.put("loc", mCurrentLoc);
        } else {
            mLogs.info("LocationService. Location IS NULL");
        }
        data.put("text", "");
        NetworkManager.updateEvent(this, data);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mLogs.info("LocationService. Service shutdown");
        mExecutor.shutdown(); // stop sending locs
        if (mLogs != null)
            mLogs.close();
    }
}
*/
