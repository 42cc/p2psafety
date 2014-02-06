package ua.p2psafety;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.facebook.Session;

import java.util.ArrayList;
import java.util.Collections;

import ua.p2psafety.media.SetMediaFragment;
import ua.p2psafety.message.MessageFragment;
import ua.p2psafety.password.PasswordFragment;
import ua.p2psafety.setemails.SetEmailsFragment;
import ua.p2psafety.setphones.SetPhoneFragment;
import ua.p2psafety.setservers.SetServersFragment;

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
                getResources().getString(R.string.add_phone),
                getResources().getString(R.string.edit_message),
                getResources().getString(R.string.emails),
                getResources().getString(R.string.servers),
                getResources().getString(R.string.password),
                getResources().getString(R.string.media),
                getResources().getString(R.string.logout)
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
                Fragment mfragment;
                FragmentManager mfragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = mfragmentManager.beginTransaction();

                switch (position) {
                    case 0:
                        mfragment = new SetPhoneFragment();
                        fragmentTransaction.addToBackStack(SetPhoneFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                        break;

                    case 1:
                        mfragment = new MessageFragment();
                        fragmentTransaction.addToBackStack(MessageFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                        break;
                    case 2:
                        mfragment = new SetEmailsFragment();
                        fragmentTransaction.addToBackStack(SetEmailsFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                        break;
                    case 3:
                        mfragment = new SetServersFragment();
                        fragmentTransaction.addToBackStack(SetServersFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                        break;
                    case 4:
                        mfragment = new PasswordFragment();
                        fragmentTransaction.addToBackStack(PasswordFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                        break;
                    case 5:
                        mfragment = new SetMediaFragment();
                        fragmentTransaction.addToBackStack(SetMediaFragment.TAG);
                        fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                        break;
                    case 6:
                        logout();
                        break;
                }
            }

        });
        return rootView;
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

        mActivity.finish();

        return;
    }

}
