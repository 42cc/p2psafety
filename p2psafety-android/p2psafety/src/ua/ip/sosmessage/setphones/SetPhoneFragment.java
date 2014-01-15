package ua.ip.sosmessage.setphones;

import android.content.Intent;
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
import android.widget.*;
import ua.ip.sosmessage.R;
import ua.ip.sosmessage.SosActivity;


public class SetPhoneFragment extends Fragment {
    public static final String TAG = "SetPhoneGragment";
    private View vParent;
    private PhonesAdapter adapter;

    private View.OnClickListener lsnr = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ibtn_addphone:
                    if (adapter != null) {
                        EditText edt_addphone = (EditText) vParent.findViewById(R.id.edt_addphone);
                        String phone = edt_addphone.getText().toString();
                        if (phone.length() > 0)
                            adapter.addPhone(phone);
                        edt_addphone.setText("");
                    }
                    break;
                case R.id.ibtn_addcontact:
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                    startActivityForResult(intent, 1001);
                    break;
            }
        }
    };

    public SetPhoneFragment() {
        super();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            Uri uri = data.getData();

            if (uri != null) {
                Cursor c = null;
                try {
                    c = getActivity().getContentResolver().query(uri, new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.TYPE},
                            null, null, null);

                    if (c != null && c.moveToFirst()) {
                        String number = c.getString(0);
                        int type = c.getInt(1);
                        EditText edt_addphone = (EditText) vParent.findViewById(R.id.edt_addphone);
                        edt_addphone.setText(number);
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.setup_numbers, container, false);
        ((SosActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((SosActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final View frame_indent = rootView.findViewById(R.id.frame_indent);
        frame_indent.setVisibility(View.VISIBLE);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        vParent = rootView;
        adapter = new PhonesAdapter(getActivity());
        View ibtnAddPhone = vParent.findViewById(R.id.ibtn_addphone);
        ibtnAddPhone.setOnClickListener(lsnr);
        View ibtnAddContact = vParent.findViewById(R.id.ibtn_addcontact);
        ibtnAddContact.setOnClickListener(lsnr);

        ListView lsv_phones = (ListView) vParent.findViewById(R.id.lsv_numbers);
        lsv_phones.setAdapter(adapter);

        ((TextView) rootView.findViewById(R.id.textView)).setTypeface(font);
        ((EditText) rootView.findViewById(R.id.edt_addphone)).setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/RobotoCondensed-Light.ttf"));
        return rootView;
    }


}



