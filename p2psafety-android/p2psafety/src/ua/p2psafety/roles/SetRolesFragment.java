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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ua.p2psafety.EventManager;
import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
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

        if (!Utils.isServerAuthenticated(mActivity)) {
            Toast.makeText(mActivity, "Please auth at server", Toast.LENGTH_LONG).show();
            return;
        }

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.setLoading(mActivity, true);
                NetworkManager.setRoles(mActivity,
                        EventManager.getInstance(mActivity).getEvent().getUser(),
                        mRoles, new NetworkManager.DeliverResultRunnable<Boolean>() {
                    @Override
                    public void deliver(Boolean success) {
                        if (success)
                            Toast.makeText(mActivity, getString(R.string.save), Toast.LENGTH_LONG).show();
                        Utils.setLoading(mActivity, false);
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
        Utils.setLoading(mActivity, true);
        // get all possible roles
        NetworkManager.getRoles(mActivity, new NetworkManager.DeliverResultRunnable<List<Role>>() {
            @Override
            public void deliver(final List<Role> all_roles) {
                if (!isAdded() || all_roles == null)
                    return;

                mRolesAdapter.clear();
                mRoles.clear();

                for (Role role : all_roles)
                    mRoles.add(role);

                all_roles.clear();

                // get user roles
                NetworkManager.getUserRoles(mActivity,
                        EventManager.getInstance(mActivity).getEvent().getUser(),
                        new NetworkManager.DeliverResultRunnable<List<String>>() {
                            @Override
                            public void deliver(final List<String> user_roles) {
                                if (!isAdded() || user_roles == null)
                                    return;

                                for (String user_role: user_roles)
                                    for (Role role: mRoles)
                                        if (role.id.equals(user_role))
                                            role.checked = true;

                                for (Role role: mRoles)
                                    mRolesAdapter.add(role);

                                Utils.setLoading(mActivity, false);
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