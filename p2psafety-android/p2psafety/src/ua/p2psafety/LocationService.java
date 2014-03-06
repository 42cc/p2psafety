package ua.p2psafety;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.Utils;

public class LocationService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    static final long TIME_INTERVAL = 2*60*1000; // 2 min
    static final long DISTANCE_INTERVAL = 50; // 50 meters

    // milliseconds per second
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int LOCATION_FIX_TIMEOUT = MILLISECONDS_PER_SECOND * 10;

    Location mLastLoc = null;
    Location mCurrentLoc = null;
    long mLastSendTime  = 0;

    ScheduledExecutorService mExecutor;

    Logs mLogs;

    public static AWLocationListener locationListener;
    private static LocationClient mLocationClient;
    private static LocationManager mLocationManager;
    private EventManager mEventManager;
    private boolean mWithUpdates = true;

    @Override
    public void onCreate() {
        super.onCreate();

        mLocationClient = new LocationClient(this, this, this);
        locationListener = new AWLocationListener();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mEventManager = EventManager.getInstance(LocationService.this);
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
                if (mEventManager.isSosStarted() && Utils.isServerAuthenticated(LocationService.this))
                {
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
            }
        }, 20*1000, 20*1000, TimeUnit.MILLISECONDS); // check every 20 sec if we need to send coords

        mLocationClient.connect();

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
        mCurrentLoc = locationListener.getLastLocation(true);
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

        if (mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             */
            mLocationManager.removeUpdates(locationListener);
        }
        /*
         * After disconnect() is called, the client is
         * considered "dead".
         */
        mLocationClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        for (String provider : mLocationManager.getProviders(true)) {
            mLogs.info(provider.toUpperCase() + " is turned on");
            mLocationManager.requestLocationUpdates(provider, 1000, 0, locationListener);
        }
    }

    @Override
    public void onDisconnected() {
        // do nothing
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // ignore
    }

    public class AWLocationListener implements android.location.LocationListener {

        private Location mLocation;                                     // last known location

        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;

            if (mEventManager.isSosStarted())
                mLogs.info("Is user authenticated on server:" +
                        Utils.isServerAuthenticated(LocationService.this) + "; New location from " +
                        mLocation.getProvider().toUpperCase() + ": " + mLocation.getLongitude()
                        + ", " + mLocation.getLatitude());

            if (location.getProvider().equals("gps")) {
                // GPS location is most accurate, so stop updating
                mLocationManager.removeUpdates(this);
                mWithUpdates = false;
            }
            else if (!mWithUpdates)
            {
                onConnected(null);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) { }

        /**
         * Return last known location
         * @param isNeededLocationServices whether we need to Location Services should be active or not,
         *                                 if true, then we ask user for activate Services,
         *                                 if false, then we just ignore it, and return location,
         *                                 which we have
         * @return last known location
         */
        public Location getLastLocation(boolean isNeededLocationServices) {
            // if last known location is not null and not too old - return it
            if (mLocation != null
                    && (System.currentTimeMillis() - mLocation.getTime() <= LOCATION_FIX_TIMEOUT))
                return mLocation;

            // start listening GPS if it is enabled
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0,
                        locationListener);
            }

            // if some of providers are not active, then ask user to do it
            if (isNeededLocationServices
                    && (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    !mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
                // Build the alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(LocationService.this);
                builder.setTitle(getString(R.string.location_services_not_active));
                builder.setMessage(getString(R.string.please_enable_location_services));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Show location settings when the user acknowledges the alert dialog
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                });
                Dialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            } else {
                // return some location from PASSIVE provider
                mLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
            return mLocation;
        }
    }
}
