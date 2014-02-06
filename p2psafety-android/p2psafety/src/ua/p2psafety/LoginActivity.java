package ua.p2psafety;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.util.Utils;

/**
 * Created by ihorpysmennyi on 12/14/13.
 */
public class LoginActivity extends ActionBarActivity {
    private final String TAG = "LoginActivity";

    private UiLifecycleHelper mUiHelper;
    private Button mLoginBtn;

    private Session.StatusCallback mStatusCallback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, SessionState state, Exception exception) {
            if (state.isOpened()) {
                startSosActivity();
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_login);
        setSupportActionBar();

        mLoginBtn = (Button) findViewById(R.id.loginBtn);
        mUiHelper = new UiLifecycleHelper(this, null);
        mUiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mUiHelper.onResume();

        NetworkManager.init(this);

        if (Utils.isFbAuthenticated(this)) {
            startSosActivity();
        }

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginToFacebook(LoginActivity.this, mStatusCallback);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mUiHelper.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        mUiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUiHelper.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Session currentSession = Session.getActiveSession();
        if (currentSession == null || currentSession.getState() != SessionState.OPENING)
            super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUiHelper.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }

    private void setSupportActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        ImageView icon;
        if (android.os.Build.VERSION.SDK_INT < 11)
            icon = (ImageView) findViewById(R.id.home);
        else
            icon = (ImageView) findViewById(android.R.id.home);
        FrameLayout.LayoutParams iconLp = (FrameLayout.LayoutParams) icon.getLayoutParams();
        iconLp.topMargin = iconLp.bottomMargin = 0;
        icon.setLayoutParams(iconLp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
//        switch (menuItem.getItemId()) {
//            case android.R.id.home:
//                if (new PhonesDatasourse(LoginActivity.this).getAllPhones().size() == 0) {
//                    Toast.makeText(LoginActivity.this, R.string.enter_phones, Toast.LENGTH_LONG).show();
//                    break;
//                }
//                Fragment fragment = new SendMessageFragment();
//                FragmentManager fragmentManager = getSupportFragmentManager();
//                for (int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
//                    fragmentManager.popBackStack();
//                }
//                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//                //fragmentTransaction.setCustomAnimations(R.anim.slide_right_in, R.anim.slide_right_out, R.anim.slide_left_in, R.anim.slide_left_out);
//                fragmentTransaction.replace(R.id.content_frame, fragment).commit();
//                break;

        //}
        return (super.onOptionsItemSelected(menuItem));
    }

    private void loginToFacebook(Activity activity, Session.StatusCallback callback) {
        if (!Utils.isNetworkConnected(activity)) {
            //errorDialog(activity, DIALOG_NO_CONNECTION);
            return;
        }
        Session session = Session.getActiveSession();
        if (session == null)
        {
            Session.openActiveSession(activity, true, callback);
        }
        else if (!session.getState().isOpened() && !session.getState().isClosed()) {
            session.openForRead(new Session.OpenRequest(activity)
                    //.setPermissions(Const.FB_PERMISSIONS_READ)
                    .setCallback(callback));
        } else {
            Session.openActiveSession(activity, true, callback);
        }
    }

    private void startSosActivity() {
        Intent i = new Intent(this, SosActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}