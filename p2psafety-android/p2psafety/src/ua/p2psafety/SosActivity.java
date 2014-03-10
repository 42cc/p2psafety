package ua.p2psafety;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import ua.p2psafety.data.Prefs;
import ua.p2psafety.fragments.SendMessageFragment;
import ua.p2psafety.services.LocationService;
import ua.p2psafety.services.PowerButtonService;
import ua.p2psafety.services.XmppService;
import ua.p2psafety.util.EventManager;
import ua.p2psafety.util.GmailOAuth2Sender;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.NetworkManager;
import ua.p2psafety.util.Utils;

/**
 * Created by ihorpysmennyi on 12/14/13.
 */
public class SosActivity extends ActionBarActivity {
    public static final String FRAGMENT_KEY = "fragmentKey";
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private UiLifecycleHelper mUiHelper;
    public static Logs mLogs;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_sosmain);

        mLogs = new Logs(this);
        mLogs.info("\n\n\n==========================\n==============================");
        mLogs.info("SosActiviy. onCreate()");
        mUiHelper = new UiLifecycleHelper(this, null);
        mUiHelper.onCreate(savedInstanceState);

        mLogs.info("SosActiviy. onCreate. Initiating NetworkManager");
        NetworkManager.init(this);
        mLogs.info("SosActiviy. onCreate. Starting PowerButtonService");
        startService(new Intent(this, PowerButtonService.class));
        startService(new Intent(this, LocationService.class));
        if (!Utils.isServiceRunning(this, XmppService.class) &&
            Utils.isServerAuthenticated(this) &&
            !EventManager.getInstance(this).isEventActive())
        {
            startService(new Intent(this, XmppService.class));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mUiHelper.onResume();

        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            showErrorDialog(result);
        }

        Fragment fragment;

        // normal start
        mLogs.info("SosActiviy. onCreate. Normal start. Opening SendMessageFragment");
        fragment = new SendMessageFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (!Utils.isFragmentAdded(fragment, fragmentManager))
        {
            fragmentManager.beginTransaction().addToBackStack(fragment.getClass().getName())
                    .replace(R.id.content_frame, fragment).commit();
        }

        String fragmentClass = getIntent().getStringExtra(FRAGMENT_KEY);
        if (fragmentClass != null) {
            // activity started from outside
            // and requested to show specific fragment
            mLogs.info("SosActiviy. onCreate. Activity requested to open " + fragmentClass);
            fragment = Fragment.instantiate(this, fragmentClass);
            fragment.setArguments(getIntent().getExtras());

            if (!Utils.isFragmentAdded(fragment, fragmentManager))
            {
                fragmentManager.beginTransaction().addToBackStack(fragment.getClass().getName())
                        .replace(R.id.content_frame, fragment).commit();
            }
        }

        if (Utils.getEmail(this) != null && Utils.isNetworkConnected(this, mLogs) && Prefs.getGmailToken(this) == null)
        {
            mLogs.info("SosActiviy. onCreate. Getting new GmailOAuth token");
            GmailOAuth2Sender sender = new GmailOAuth2Sender(this);
            sender.initToken();
        }
        mLogs.info("SosActiviy. onCreate. Checking for location services");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i("onNewIntent", "NEW INTENT!");
        setIntent(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mLogs.info("SosActiviy.onActivityResult()");
        mUiHelper.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogs.info("SosActiviy.onPause");
        mUiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLogs.info("SosActiviy.onDestroy()");
        mLogs.info("\n\n\n==========================\n==============================");
        mUiHelper.onDestroy();
        mLogs.close();
    }

    @Override
    public void onBackPressed() {
        mLogs.info("SosActivity.onBackPressed()");
        Session currentSession = Session.getActiveSession();
        if (currentSession == null || currentSession.getState() != SessionState.OPENING) {
            super.onBackPressed();

            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() == 0) {
                finish();
            }
        } else {
            mLogs.info("SosActivity. onBackPressed. Ignoring");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!EventManager.getInstance(this).isSosStarted())
            stopService(new Intent(this, LocationService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!EventManager.getInstance(this).isSosStarted())
            startService(new Intent(this, LocationService.class));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mLogs.info("SosActivity.onSaveInstanceState()");
        mUiHelper.onSaveInstanceState(outState);
        mLogs.info("SosActivity. onSaveInstanceState. Saving session");
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }

    private void showErrorDialog(int result) {
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(result,
                this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
        if (errorDialog != null)
            errorDialog.show();
    }

    public void loginToFacebook(Activity activity, Session.StatusCallback callback) {
        mLogs.info("SosActivity. loginToFacebook()");
        if (!Utils.isNetworkConnected(activity, mLogs)) {
            mLogs.info("SosActivity. loginToFacebook. No network");
            Utils.errorDialog(activity, Utils.DIALOG_NO_CONNECTION);
            return;
        }
        Session session = Session.getActiveSession();
        if (session == null) {
            mLogs.info("SosActivity. No FB session. Opening a new one");
            Session.openActiveSession(activity, true, callback);
        }
        else if (!session.getState().isOpened() && !session.getState().isClosed()) {
            mLogs.info("SosActivity. loginToFacebook. FB session not opened AND not closed. Opening for read");
            session.openForRead(new Session.OpenRequest(activity)
                    //.setPermissions(Const.FB_PERMISSIONS_READ)
                    .setCallback(callback));
        } else {
            mLogs.info("SosActivity. loginToFacebook. FB session opened or closed. Opening a new one");
            Session.openActiveSession(activity, true, callback);
        }
    }
}