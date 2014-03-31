package ua.p2psafety.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ua.p2psafety.ObservableScrollView;
import ua.p2psafety.P2PMapView;
import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
import ua.p2psafety.adapters.StableArrayAdapter;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.json.Event;
import ua.p2psafety.services.AudioRecordService;
import ua.p2psafety.services.VideoRecordService;
import ua.p2psafety.services.XmppService;
import ua.p2psafety.util.EventManager;
import ua.p2psafety.util.NetworkManager;
import ua.p2psafety.util.Utils;

public class SupporterFragment extends Fragment implements ObservableScrollView.ScrollViewListener {
    String mVictimName;

    TextView mVictimNameText;
    Button mAudioBtn, mVideoBtn;
    Button mCloseEventBtn;
    Activity mActivity;
    Event mEvent;
    P2PMapView mMapView;
    GoogleMap mMap;
    ListView mCommentsList;

    ScheduledExecutorService mExecutor;

    String mSupportUrl;
    Location mEventLocation;

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final Calendar cal = Calendar.getInstance(Locale.getDefault());

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

        mAudioBtn = (Button) view.findViewById(R.id.btn_audio);
        mVideoBtn = (Button) view.findViewById(R.id.btn_video);
        mCloseEventBtn = (Button) view.findViewById(R.id.btn_close_event);
        mCommentsList = (ListView) view.findViewById(R.id.lsv_comments);
        mVictimNameText = (TextView) view.findViewById(R.id.txt_victim_name);

        mEvent = Prefs.getEvent(mActivity);

        ObservableScrollView observableScrollView = (ObservableScrollView) view.findViewById(R.id.scroll_view);
        observableScrollView.setScrollViewListener(this);

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
    public void onScrollChanged(ObservableScrollView sv, int x, int y, int oldx, int oldy) {
        sv.setVisibility(View.GONE);
        sv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        setupMediaButtons();

        mAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop video record services
                mActivity.stopService(new Intent(mActivity, VideoRecordService.class));
                // start audio record if that's what user wants
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
                // stop audio record service
                mActivity.stopService(new Intent(mActivity, AudioRecordService.class));
                // start video record if that's what user wants
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

        mCommentsList.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                v.onTouchEvent(event);
                return true;
            }
        });

        mSupportUrl = XmppService.VICTIM_DATA.getSupporterUrl();
        mEventLocation = XmppService.VICTIM_DATA.getLocation();
        mVictimName = XmppService.VICTIM_DATA.getName();

        mVictimNameText.setText(mVictimName);

        LatLng eventLatLng = new LatLng(mEventLocation.getLatitude(), mEventLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(eventLatLng)
                .title(mVictimName + ": " + dateFormat.format(cal.getTime()))
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_YELLOW)));

        MapsInitializer.initialize(mActivity);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, 15.0f));

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
        final String event_id = Arrays.asList(str.trim().split(" ")).get(1);
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

                        // add markers for victims path
                        mMap.clear();
                        MarkerOptions latestPosition = null;
                        for (int i = 0; i < updates.size(); ++i) {
                            Event update = updates.get(i);
                            Log.i("SupporterFragment", "update id: " + update.getId());
                            Location loc = update.getLocation();
                            if (loc != null) {
                                Log.i("SupporterFragment", "new loc on map: " + loc);
                                LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                                MarkerOptions marker = new MarkerOptions();
                                marker.position(latLng)
                                      .title(mVictimName + ": " + dateFormat.format(cal.getTime()));

                                if (latestPosition == null)
                                    latestPosition = marker;
                                else
                                    mMap.addMarker(marker);
                            }
                        }
                        // draw latest position after all others
                        // so this marker is always on top
                        if (latestPosition != null) {
                            latestPosition.icon(BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_YELLOW));
                            mMap.addMarker(latestPosition);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    latestPosition.getPosition(), 15.0f));
                        }

                        List<String> comments = new ArrayList<String>();
                        // try to get latest event info
                        for (Event update : updates) {
                            Log.i("SupporterFragment", "update id: " + update.getId());
                            String text = update.getText();
                            if (text != null && !text.isEmpty()) {
                                comments.add(update.getText());
                            }
                        }
                        StableArrayAdapter adapter = new StableArrayAdapter(mActivity,
                                android.R.layout.simple_list_item_1, comments);
                        mCommentsList.setAdapter(adapter);
                    }
                });

        NetworkManager.getSupportEventUpdates(mActivity, event_id, new NetworkManager.DeliverResultRunnable<List<Event>>() {
            @Override
            public void deliver(List<Event> events) {
                super.deliver(events);
                //TODO: adding supporter to map
                String x = "";
            }
        });
    }

    private void closeEvent() {
        Prefs.putSupporterMode(mActivity, false);
        if (mEvent != null)
            mEvent.setStatus(Event.STATUS_FINISHED);

        // TODO: network request goes here
        // stop all record services
        mActivity.stopService(new Intent(mActivity, AudioRecordService.class));
        mActivity.stopService(new Intent(mActivity, VideoRecordService.class));

        EventManager.getInstance(mActivity).createNewEvent();

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