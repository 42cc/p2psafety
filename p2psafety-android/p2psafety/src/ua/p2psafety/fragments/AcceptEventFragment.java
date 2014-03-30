package ua.p2psafety.fragments;

import android.app.Activity;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import ua.p2psafety.json.Event;
import ua.p2psafety.util.EventManager;
import ua.p2psafety.P2PMapView;
import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
import ua.p2psafety.services.XmppService;
import ua.p2psafety.util.NetworkManager;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.Utils;

public class AcceptEventFragment extends Fragment {
    TextView mEventInfo;
    TextView mVictimNameText;
    Button mAcceptBtn, mIgnoreBtn;
    Activity mActivity;

    Location mEventLocation;
    String mEventSupportUrl;
    String mLastComment;
    String mVictimName;

    Boolean mAccepted = false;

    P2PMapView mMapView;
    GoogleMap mMap;

    Logs mLogs;
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final Calendar cal = Calendar.getInstance(Locale.getDefault());

    public AcceptEventFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = getActivity();
        mLogs = new Logs(mActivity);

        ((SosActivity) mActivity).getSupportActionBar().setHomeButtonEnabled(false);
        ((SosActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        View view = inflater.inflate(R.layout.frag_accept_event, container, false);

        mEventInfo = (TextView) view.findViewById(R.id.txt_info);
        mAcceptBtn = (Button) view.findViewById(R.id.btn_accept);
        mIgnoreBtn = (Button) view.findViewById(R.id.btn_ignore);
        mEventInfo = (TextView) view.findViewById(R.id.txt_info);
        mVictimNameText = (TextView) view.findViewById(R.id.txt_victim_name);

        mMapView = (P2PMapView) view.findViewById(R.id.fae_map);
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
        mAccepted = false;

        mAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogs.info("Accept button clicked");
                acceptEvent();
            }
        });

        mIgnoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogs.info("Ignore button clicked");
                ignoreEvent();
                mActivity.onBackPressed();
            }
        });

        Utils.startVibration(mActivity);
        Utils.playDefaultNotificationSound(mActivity);
        Utils.blinkLED(mActivity);

        mEventLocation = XmppService.VICTIM_DATA.getLocation();
        mEventSupportUrl = XmppService.VICTIM_DATA.getSupporterUrl();
        mLastComment = XmppService.VICTIM_DATA.getLastComment();
        mVictimName = XmppService.VICTIM_DATA.getName();

        mEventInfo.setText(mLastComment);
        mVictimNameText.setText(mVictimName);

        System.out.println("onViewCreated. location: " + mEventLocation);
        System.out.println("onViewCreated. url: " + mEventSupportUrl);

        LatLng eventLatLng = new LatLng(mEventLocation.getLatitude(), mEventLocation.getLongitude());
        Log.i("AcceptEventFragment", "loc: \n" + mEventLocation);
        Log.i("AcceptEventFragment", "latLng: \n" + eventLatLng);
        mMap.addMarker(new MarkerOptions()
                .position(eventLatLng)
                .title(mVictimName + ": " + dateFormat.format(cal.getTime())));

        MapsInitializer.initialize(mActivity);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, 15.0f));
    }

    private void acceptEvent() {
        mLogs.info("accepting event");
        mAccepted = true;

        Utils.setLoading(mActivity, true);
        NetworkManager.supportEvent(mActivity, mEventSupportUrl,
                new NetworkManager.DeliverResultRunnable<Boolean>() {
            @Override
            public void deliver(final Boolean success) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.setLoading(mActivity, false);
                        //mActivity.onBackPressed();
                        if (true) { // TODO: find out what is supposed to be here
                            try {
                                EventManager.getInstance(mActivity).getEvent().setType(Event.TYPE_SUPPORT);
                                EventManager.getInstance(mActivity).getEvent().setStatus(Event.STATUS_ACTIVE);
                            } catch (Exception e) {
                                // should never happen
                            }
                            XmppService.processing_event = false;

                            // open Supporter screen

                            Fragment fragment = new SupporterFragment();
                            FragmentManager fm = getFragmentManager();
                            if (!Utils.isFragmentAdded(fragment, fm))
                            {
                                fm.popBackStackImmediate();
                                fm.beginTransaction()
                                    .addToBackStack(fragment.getClass().getName())
                                    .replace(R.id.content_frame, fragment).commit();
                            }
                        }
                    }
                });
            }
        });


    }

    private void ignoreEvent() {
        mLogs.info("ignoring event");
        mAccepted = false;
        XmppService.processing_event = false;
    }

    @Override
    public void onResume() {
        mLogs.info("AcceptEvent screen opened");
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        Log.i("AcceptEventFragment", "onPause");
        mLogs.info("closing AcceptEventScreen...");
        if (!mAccepted)
            ignoreEvent();
        mLogs.info("AcceptEventScreen closed");
        super.onPause();
        mMapView.onPause();
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