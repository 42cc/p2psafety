package ua.p2psafety;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import ua.p2psafety.media.SetMediaFragment;
import ua.p2psafety.message.MessageFragment;
import ua.p2psafety.password.PasswordFragment;
import ua.p2psafety.roles.SetRolesFragment;
import ua.p2psafety.setemails.SetEmailsFragment;
import ua.p2psafety.setphones.SetPhoneFragment;
import ua.p2psafety.setservers.SetServersFragment;
import ua.p2psafety.sms.MessageResolver;
import ua.p2psafety.util.Utils;

/**
 * Created by Taras Melon on 08.01.14.
 */
public class SettingsFragment extends Fragment {

    private View vParent;
    private Activity mActivity;

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

        final ListView settingsList = (ListView) vParent.findViewById(R.id.settings_list);
        final String[] values = new String[]{
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

        final ArrayList<String> list = new ArrayList<String>();
        Collections.addAll(list, values);
        final StableArrayAdapter adapter = new StableArrayAdapter(this.getActivity(),
                android.R.layout.simple_list_item_1, list);
        settingsList.setAdapter(adapter);

        settingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final Fragment[] mfragment = new Fragment[1];
                FragmentManager mfragmentManager = getFragmentManager();
                final FragmentTransaction fragmentTransaction = mfragmentManager.beginTransaction();

                switch (position) {
                    case 0:
                        mfragment[0] = new SetPhoneFragment();
                        fragmentTransaction.addToBackStack(SetPhoneFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;

                    case 1:
                        mfragment[0] = new MessageFragment();
                        fragmentTransaction.addToBackStack(MessageFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 2:
                        mfragment[0] = new SetEmailsFragment();
                        fragmentTransaction.addToBackStack(SetEmailsFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 3:
                        if (Utils.isFbAuthenticated(mActivity)) {
                            mfragment[0] = new SetServersFragment();
                            fragmentTransaction.addToBackStack(SetServersFragment.TAG);
                            fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        } else {
                            // ask user if he wants to login via FB
                            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                            builder.setTitle("Авторизоваться через Facebook?");
                            builder.setNegativeButton(android.R.string.cancel, null);
                            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Session.StatusCallback mStatusCallback = new Session.StatusCallback() {
                                        @Override
                                        public void call(final Session session, SessionState state, Exception exception) {
                                            if (state.isOpened()) {
                                                mfragment[0] = new SetServersFragment();
                                                fragmentTransaction.addToBackStack(SetServersFragment.TAG);
                                                fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                                            }
                                        }
                                    };

                                    ((SosActivity) mActivity)
                                            .loginToFacebook(mActivity, mStatusCallback);
                                }
                            });
                            builder.create().show();
                        }
                        break;
                    case 4:
                        mfragment[0] = new PasswordFragment();
                        fragmentTransaction.addToBackStack(PasswordFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 5:
                        mfragment[0] = new SetMediaFragment();
                        fragmentTransaction.addToBackStack(SetMediaFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 6:
                        mfragment[0] = new SetRolesFragment();
                        fragmentTransaction.addToBackStack(SetRolesFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                    case 7:
                        logout();
                        break;
                    case 8:
                        mfragment[0] = new SendLogsFragment();
                        fragmentTransaction.addToBackStack(SendLogsFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment[0]).commit();
                        break;
                }
            }

        });

        Utils.checkForLocationServices(mActivity);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        getView().bringToFront();
    }

    public void logout() {
        if (Session.getActiveSession() != null)
            Session.getActiveSession().closeAndClearTokenInformation();
        Session.setActiveSession(null);

        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(mActivity);
        sharedPref.edit().putString("MYSELF_KEY", "").commit();

        Intent i = new Intent(mActivity, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);

        Toast.makeText(mActivity, "Готово", Toast.LENGTH_SHORT)
                .show();

        //mActivity.finish();

        return;
    }

    private class SendReportAsyncTask extends AsyncTask {

        private File file;

        public SendReportAsyncTask(File file)
        {
            this.file = file;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            MessageResolver resolver = new MessageResolver(mActivity);
            resolver.sendEmails("Error", file);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            Toast.makeText(mActivity, R.string.logs_successfully_sent, Toast.LENGTH_SHORT).show();
        }
    }

}
