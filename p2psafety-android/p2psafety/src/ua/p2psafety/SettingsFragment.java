package ua.p2psafety;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.media.SetMediaFragment;
import ua.p2psafety.message.MessageFragment;
import ua.p2psafety.password.PasswordFragment;
import ua.p2psafety.roles.SetRolesFragment;
import ua.p2psafety.setemails.SetEmailsFragment;
import ua.p2psafety.setphones.SetPhoneFragment;
import ua.p2psafety.setservers.SetServersFragment;
import ua.p2psafety.sms.MessageResolver;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.Utils;

/**
 * Created by Taras Melon on 08.01.14.
 */
public class SettingsFragment extends Fragment {
    private View vParent;
    private Activity mActivity;

    ListView mSettingsList;

    private Runnable mDoAfterLogin = null;

    Logs mLogs;

    public SettingsFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.frag_settings, container, false);
        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        vParent = rootView;

        mActivity = getActivity();
        mLogs = new Logs(mActivity);

        mLogs.info("SettingsFragment.onCreateView()");

        mSettingsList = (ListView) vParent.findViewById(R.id.settings_list);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        setupOptions();

        mSettingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                mLogs.info("SettingsFragment. Settings list. Item click");

                final Fragment[] mfragment = new Fragment[1];
                FragmentManager mfragmentManager = getFragmentManager();
                final FragmentTransaction fragmentTransaction = mfragmentManager.beginTransaction();

                switch (position) {
                    case 0:
                        mLogs.info("SettingsFragment. User chose Phones settings");
                        mfragment[0] = new SetPhoneFragment();
                        fragmentTransaction.addToBackStack(SetPhoneFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 1:
                        mLogs.info("SettingsFragment. User chose Message settings");
                        mfragment[0] = new MessageFragment();
                        fragmentTransaction.addToBackStack(MessageFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 2:
                        mLogs.info("SettingsFragment. User chose Emails settings");
                        mfragment[0] = new SetEmailsFragment();
                        fragmentTransaction.addToBackStack(SetEmailsFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 3:
                        mLogs.info("SettingsFragment. User chose Servers settings");
                        if (Prefs.getApiKey(mActivity) != null) {
                            mLogs.info("SettingsFragment. We have ApiKey. Opening Servers screen");
                            openServersScreen();
                        } else {
                            mLogs.info("SettingsFragment. No ApiKey. Asking user to log in");
                            login(new Runnable() {
                                @Override
                                public void run() {
                                    openServersScreen();
                                }
                            });
                        }
                        break;
                    case 4:
                        mLogs.info("SettingsFragment. User chose Password settings");
                        mfragment[0] = new PasswordFragment();
                        fragmentTransaction.addToBackStack(PasswordFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 5:
                        mLogs.info("SettingsFragment. User chose Media settings");
                        mfragment[0] = new SetMediaFragment();
                        fragmentTransaction.addToBackStack(SetMediaFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 6:
                        mLogs.info("SettingsFragment. User chose Roles settings");
                        mfragment[0] = new SetRolesFragment();
                        fragmentTransaction.addToBackStack(SetRolesFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 7:
                        mLogs.info("SettingsFragment. User chose Logout");
                        if (Utils.isServerAuthenticated(mActivity))
                            logout();
                        else
                            login(null);
                        break;
                    case 8:
                        mLogs.info("SettingsFragment. User chose Send Logs");
                        mfragment[0] = new SendLogsFragment();
                        fragmentTransaction.addToBackStack(SendLogsFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                }
            }
        });

        mLogs.info("SettingsFragment. Checking location services");
        Utils.checkForLocationServices(mActivity);

        mLogs.info("SettingsFragment.onResume()");
        getView().bringToFront();
    }

    public void login(Runnable doAfterLogin) {
        mDoAfterLogin = doAfterLogin;

        mLogs.info("SettingsFragment. Building password dialog");
        LayoutInflater li = LayoutInflater.from(mActivity);
        View promptsView = li.inflate(R.layout.login_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
        alertDialogBuilder.setView(promptsView);

        final EditText userLogin = (EditText) promptsView.findViewById(R.id.ld_login_edit);
        final EditText userPassword = (EditText) promptsView.findViewById(R.id.ld_password_edit);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mLogs.info("SettingsFragment. User entered login and password: " +
                                        userLogin.getText().toString() + " " +
                                        userPassword.getText().toString() + "  " +
                                        "Sending request");
                                Utils.setLoading(mActivity, true);
                                NetworkManager.loginAtServer(mActivity,
                                        userLogin.getText().toString(),
                                        userPassword.getText().toString(), postRunnable);
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mLogs.info("SettingsFragment. User canceled login dialog");
                                dialog.cancel();
                            }
                        });
        final AlertDialog alertDialog = alertDialogBuilder.create();

        final Button loginBtn = (Button) promptsView.findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogs.info("SettingsFragment. User tries to login with FB");
                Session.StatusCallback mStatusCallback = new Session.StatusCallback() {
                    @Override
                    public void call(final Session session, SessionState state, Exception exception) {
                        mLogs.info("SettingsFragment. FB Session callback. state: " + state.toString());
                        if (state.isOpened()) {
                            mLogs.info("SettingsFragment. FB session is opened. Login at server");
                            Utils.setLoading(mActivity, true);
                            NetworkManager.loginAtServer(mActivity,
                                    Session.getActiveSession().getAccessToken(),
                                    NetworkManager.FACEBOOK, postRunnable);
                        }
                    }
                };
                alertDialog.dismiss();
                ((SosActivity) mActivity).loginToFacebook(mActivity, mStatusCallback);
            }
        });


        alertDialog.show();
        alertDialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return;
    }

    public void logout() {
        if (Session.getActiveSession() != null)
            Session.getActiveSession().closeAndClearTokenInformation();
        Session.setActiveSession(null);
        Prefs.putApiKey(mActivity, null);

        setupOptions();
    }

    private void setupOptions() {
        String[] options = new String[]{
                getString(R.string.add_phone),
                getString(R.string.edit_message),
                getString(R.string.emails),
                getString(R.string.servers),
                getString(R.string.password),
                getString(R.string.media),
                getString(R.string.roles),
                getString(R.string.logout),
                getString(R.string.send_logs)
        };
        StableArrayAdapter adapter = new StableArrayAdapter(mActivity,
                android.R.layout.simple_list_item_1,
                new ArrayList<String>(Arrays.asList(options)));

        if (!Utils.isServerAuthenticated(mActivity))
            adapter.mSettingsList.set(7, "Вход (Login)");

        mSettingsList.setAdapter(adapter);
    }

    // if login completed successfully, open Servers screen;
    // otherwise build new dialog with retry/cancel buttons
    private NetworkManager.DeliverResultRunnable<Boolean> postRunnable = new NetworkManager.DeliverResultRunnable<Boolean>() {
        @Override
        public void deliver(final Boolean success) {
            mLogs.info("SettingsFragment. Login succeed");
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.setLoading(mActivity, false);
                    setupOptions();
                    mActivity.startService(new Intent(mActivity, XmppService.class));
                    if (mDoAfterLogin != null)
                        mDoAfterLogin.run();
                }
            });
        }

        @Override
        public void onError(final int errorCode) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogs.info("SettingsFragment. Login failed");
                    Utils.setLoading(mActivity, false);
                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

                    switch (errorCode) {
                        case 401:
                            mLogs.info("SettingsFragment. error code: 401 wrong credentials");
                            builder.setTitle("Неверный логин или пароль.");
                            break;
                        default:
                            mLogs.info("SettingsFragment. error code: " + errorCode);
                            builder.setTitle("Не получилось авторизоваться.");
                            break;
                    }

                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mLogs.info("SettingsFragment. User tries to login again");
                            login(mDoAfterLogin);
                        }
                    });
                    builder.create().show();
                }
            });
        }
    };

    private void openServersScreen() {
        mLogs.info("SettingsFragment. Opening Servers screen");
        FragmentManager mfragmentManager = getFragmentManager();
        if (mfragmentManager != null)
        {
            final FragmentTransaction fragmentTransaction = mfragmentManager.beginTransaction();
            Fragment fragment = new SetServersFragment();
            fragmentTransaction.addToBackStack(SetServersFragment.TAG);
            fragmentTransaction.replace(R.id.content_frame, fragment).commit();
        }
    }
}
