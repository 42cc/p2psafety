package ua.ip.sosmessage.setservers;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import ua.ip.sosmessage.R;
import ua.ip.sosmessage.SosActivity;
import ua.ip.sosmessage.setemails.EmailsAdapter;
import ua.ip.sosmessage.util.Utils;

public class SetServersFragment extends Fragment {
    public static final String TAG = "SetServersFragment";
    private View vParent;
    private ServersAdapter mAdapter;

    private View.OnClickListener lsnr = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ibtn_addserver:
                    if (mAdapter != null) {
                        EditText edt_addserver = (EditText) vParent.findViewById(R.id.edt_addserver);
                        String address = edt_addserver.getText().toString();
                        if (address.length() > 0)
                            mAdapter.addServer(address);
                        edt_addserver.setText("");
                    }
                    break;
            }
        }
    };

    public SetServersFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.setup_servers, container, false);
        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final View pic_protect = rootView.findViewById(R.id.img_protect);
        final View frame_indent = rootView.findViewById(R.id.frame_indent);
        pic_protect.setVisibility(View.GONE);
        frame_indent.setVisibility(View.VISIBLE);

        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        vParent = rootView;
        mAdapter = new ServersAdapter(getActivity());
        View ibtnAddServer = vParent.findViewById(R.id.ibtn_addserver);
        ibtnAddServer.setOnClickListener(lsnr);

        ListView lsv_servers = (ListView) vParent.findViewById(R.id.lsv_servers);
        lsv_servers.setAdapter(mAdapter);

        ((TextView) rootView.findViewById(R.id.textView)).setTypeface(font);
        ((EditText) rootView.findViewById(R.id.edt_addserver)).setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/RobotoCondensed-Light.ttf"));
        return rootView;
    }


}



