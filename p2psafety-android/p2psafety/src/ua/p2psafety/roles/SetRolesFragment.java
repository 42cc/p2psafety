package ua.p2psafety.roles;


import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
import ua.p2psafety.SosManager;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.util.Utils;

public class SetRolesFragment extends Fragment {
    public static final String TAG = "SetRolesFragment";
    private Button mSaveBtn;
    private Activity mActivity;

    ListView mRolesView;
    List<Role> mRoles = new ArrayList<Role>();
    RolesAdapter mRolesAdapter;

    public SetRolesFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_roles, container, false);

        mActivity = getActivity();
        mRolesView = (ListView) view.findViewById(R.id.roles_list);
        mSaveBtn = (Button) view.findViewById(R.id.btn_save);

        mRolesAdapter = new RolesAdapter(mActivity);
        mRolesView.setAdapter(mRolesAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((SosActivity) mActivity).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Typeface font = Typeface.createFromAsset(mActivity.getAssets(), "fonts/RobotoCondensed-Bold.ttf");
        mSaveBtn.setTypeface(font);
    }

    @Override
    public void onResume() {
        super.onResume();

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkManager.setRoles(mActivity,
                        SosManager.getInstance(mActivity).getEvent().getUser(),
                        mRoles, new NetworkManager.DeliverResultRunnable<Boolean>() {
                    @Override
                    public void deliver(Boolean success) {
                        if (success)
                            Toast.makeText(mActivity, getString(R.string.save), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        if (mRoles.isEmpty()) {
            fetchAndFillAdapter();
        }

        if (mRolesAdapter.isEmpty()) {
            for (Role role_ : mRoles) {
                mRolesAdapter.add(role_);
            }
        }
    }

    private void fetchAndFillAdapter() {
        //Utils.setLoading(mActivity, true);

        // get all possible roles
        NetworkManager.getRoles(mActivity, null, new NetworkManager.DeliverResultRunnable<List<Role>>() {
            @Override
            public void deliver(final List<Role> all_roles) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded() || all_roles == null)
                            return;

                        mRolesAdapter.clear();
                        mRoles.clear();

                        for (Role role : all_roles)
                            mRoles.add(role);

                        all_roles.clear();

                        // get user picked roles
                        NetworkManager.getRoles(mActivity,
                                SosManager.getInstance(mActivity).getEvent().getUser(),
                                new NetworkManager.DeliverResultRunnable<List<Role>>() {
                                    @Override
                                    public void deliver(final List<Role> picked_roles) {
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!isAdded() || picked_roles == null)
                                                    return;

                                                for (Role picked_role: picked_roles)
                                                    for (Role role: mRoles)
                                                        if (role.id == picked_role.id)
                                                            role.checked = true;

                                                for (Role role: mRoles)
                                                    mRolesAdapter.add(role);

                                                picked_roles.clear();
                                                //Utils.setLoading(mActivity, false);
                                            }
                                        });
                                    }
                                });
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}