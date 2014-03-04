package ua.p2psafety;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.util.Utils;

public class SupporterFragment extends Fragment {

    TextView mEventInfo;
    Button mAudioBtn, mVideoBtn;
    Button mCloseEventBtn;
    Activity mActivity;
    Event mEvent;
    P2PMapView mMapView;
    GoogleMap mMap;

    ScheduledExecutorService mExecutor;

    String mSupportUrl;
    Location mEventLocation;

    public SupporterFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = getActivity();
        ((SosActivity) mActivity).getSupportActionBar().setHomeButtonEnabled(false);
        ((SosActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        View view = inflater.inflate(R.layout.frag_supporter, container, false);

        mEventInfo = (TextView) view.findViewById(R.id.txt_info);
        mAudioBtn = (Button) view.findViewById(R.id.btn_audio);
        mVideoBtn = (Button) view.findViewById(R.id.btn_video);
        mCloseEventBtn = (Button) view.findViewById(R.id.btn_close_event);

        mEvent = Prefs.getEvent(mActivity);

//        mActivity.getIntent().putExtra("event_id", mEvent.getId());

//        NetworkManager.getInfoAboutEvent(mActivity, mActivity.getIntent().getStringExtra("event_id")
//                , new NetworkManager.DeliverResultRunnable<Boolean>() {
//            @Override
//            public void deliver(Boolean aBoolean) {
//                //good
//            }
//        });

        mMapView = (P2PMapView) view.findViewById(R.id.supporter_map);
        mMapView.onCreate(savedInstanceState);

        mMap = mMapView.getMap();
        if (mMap != null) {
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.setMyLocationEnabled(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        return view;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        setupMediaButtons();

        mAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop all record services
                mActivity.stopService(new Intent(mActivity, AudioRecordService.class));
                mActivity.stopService(new Intent(mActivity, VideoRecordService.class));
                // start audio record if we that's what user wants
                if (!Utils.isServiceRunning(mActivity, AudioRecordService.class))
                    mActivity.startService(new Intent(mActivity, AudioRecordService.class));
                else
                    mActivity.stopService(new Intent(mActivity, AudioRecordService.class));
                setupMediaButtons();
            }
        });

        mVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop audio record services
                mActivity.stopService(new Intent(mActivity, AudioRecordService.class));

                // start video record if we that's what user wants
                if (!Utils.isServiceRunning(mActivity, VideoRecordService.class))
                    mActivity.startService(new Intent(mActivity, VideoRecordService.class));
                else
                    mActivity.stopService(new Intent(mActivity, VideoRecordService.class));
                setupMediaButtons();
            }
        });

        mCloseEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEvent();
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            mSupportUrl = bundle.getString(XmppService.SUPPORTER_URL_KEY);
            mEventLocation = (Location) bundle.get(XmppService.LOCATION_KEY);

            LatLng eventLatLng = new LatLng(mEventLocation.getLatitude(), mEventLocation.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(eventLatLng)
                    .title("Victim"));

            MapsInitializer.initialize(mActivity);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, 15.0f));
        } else {
            mSupportUrl = Prefs.getSupportUrl(mActivity);
        }

        Log.i("SupporterFragment", "url: " + String.valueOf(mSupportUrl));
        Log.i("SupporterFragment", "location: " + String.valueOf(mEventLocation));

        startAutoUpdates(mSupportUrl);
    }

    public void startAutoUpdates(final String support_url) {
        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateMap(support_url);
            }
        }, 0, 60*1000, TimeUnit.MILLISECONDS); // update map every 60 sec
    }

    private void stopAutoUpdates() {
        mExecutor.shutdown();
    }

    private void updateMap(String support_url) {
        // getting event id from url
        String str = support_url.replaceAll("[^0-9]+", " ");
        String event_id = Arrays.asList(str.trim().split(" ")).get(1);
        Log.i("SupporterFragment", "Event id: " + event_id);

        NetworkManager.getEventUpdates(mActivity, event_id,
                new NetworkManager.DeliverResultRunnable<List<Event>>() {
                    @Override
                    public void deliver(List<Event> updates) {
                        if (updates == null || updates.size() < 1)
                            return;

                        // sort updates from newest to oldest
                        Collections.sort(updates, new Comparator<Event>() {
                            @Override
                            public int compare(Event a, Event b) {
                                return Integer.valueOf(b.getId()) - Integer.valueOf(a.getId());
                            }
                        });

                        // try to get latest loc
                        for (Event update: updates) {
                            Log.i("SupporterFragment", "update id: " + update.getId());
                            Location loc = update.getLocation();
                            if (loc != null) {
                                mMap.clear();
                                Log.i("SupporterFragment", "new loc on map: " + loc);
                                LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                                mMap.addMarker(new MarkerOptions()
                                        .position(latLng).title("Victim name"));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));

                                break;
                            }
                        }
                    }
                });
    }

    private void closeEvent() {
        // TODO: network request goes here
        // stop all record services
        mActivity.stopService(new Intent(mActivity, AudioRecordService.class));
        mActivity.stopService(new Intent(mActivity, VideoRecordService.class));
        mActivity.stopService(new Intent(mActivity, LocationService.class));

        if (mEvent != null)
            mEvent.setStatus(Event.STATUS_FINISHED);

        EventManager.getInstance(mActivity).createNewEvent();

        Prefs.putSupporterMode(mActivity, false);
        mActivity.onBackPressed();
    }

    private void setupMediaButtons() {
        if (Utils.isServiceRunning(mActivity, AudioRecordService.class))
            mAudioBtn.setText(getString(R.string.stop_media_record).replace("#media#",
                    getString(R.string.upper_audio)));
        else
            mAudioBtn.setText(getString(R.string.start_media_record).replace("#media#",
                    getString(R.string.upper_audio)));

        if (Utils.isServiceRunning(mActivity, VideoRecordService.class))
            mVideoBtn.setText(getString(R.string.stop_media_record).replace("#media#",
                    getString(R.string.upper_video)));
        else
            mVideoBtn.setText(getString(R.string.start_media_record).replace("#media#",
                    getString(R.string.upper_video)));
    }

    @Override
    public void onResume() {
        Prefs.putSupporterMode(mActivity, true);
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        stopAutoUpdates();

        Prefs.putSupportUrl(mActivity, mSupportUrl);
    }

    @Override
     public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

}