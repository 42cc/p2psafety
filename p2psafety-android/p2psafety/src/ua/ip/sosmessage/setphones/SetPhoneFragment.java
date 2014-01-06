package ua.ip.sosmessage.setphones;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;
import ua.ip.sosmessage.R;
import ua.ip.sosmessage.SendMessageFragment;
import ua.ip.sosmessage.SosActivity;
import ua.ip.sosmessage.data.PhonesDatasourse;
import ua.ip.sosmessage.message.MessageFragment;


public class SetPhoneFragment extends Fragment {
    View.OnClickListener lsnbbtn = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context context = getActivity();
            if (new PhonesDatasourse(context).getAllPones().size() == 0) {
                Toast.makeText(context, R.string.enter_phones, Toast.LENGTH_LONG).show();
                return;
            }

            Fragment mfragment;
            FragmentManager mfragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = mfragmentManager.beginTransaction();

            switch (v.getId()) {
                case R.id.btn_sos:
                    mfragment = new SendMessageFragment();
                    for (int i = 0; i < mfragmentManager.getBackStackEntryCount(); ++i) {
                        mfragmentManager.popBackStack();
                    }
                    //fragmentTransaction.setCustomAnimations(R.anim.slide_right_in, R.anim.slide_right_out, R.anim.slide_left_in, R.anim.slide_left_out);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                    break;
                case R.id.btn_next:
                    mfragment = new MessageFragment();
                    //fragmentTransaction.setCustomAnimations(R.anim.slide_left_in, R.anim.slide_left_out, R.anim.slide_right_in, R.anim.slide_right_out);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.replace(R.id.content_frame, mfragment).commit();
                    break;
            }
        }
    };
    private View vParent;
    private PhonesAdapter adapter;
    private View.OnClickListener lsnr = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ibtn_addnumber:
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
                    // BoD con't: CONTENT_TYPE instead of CONTENT_ITEM_TYPE
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
        if (!getActivity().getString(R.string.strissmall).equals("small"))
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (rootView == null || getActivity() == null)
                        return;
                    final View pic_protect = rootView.findViewById(R.id.img_protect);
                    final View frame_indent = rootView.findViewById(R.id.frame_indent);
                    int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight() - ((SosActivity)
                            getActivity()).getSupportActionBar().getHeight();
                    if (heightDiff > 150) {
                        Log.d("KeyBoard", "hVisible" + heightDiff);
                        pic_protect.setVisibility(View.GONE);
                        frame_indent.setVisibility(View.VISIBLE);
                    } else if (pic_protect.getVisibility() == View.GONE) {
                        Log.d("KeyBoard", "hinVisible" + heightDiff);
                        pic_protect.setVisibility(View.VISIBLE);
                        frame_indent.setVisibility(View.GONE);
                    }
                }
            });
        else {
            final View pic_protect = rootView.findViewById(R.id.img_protect);
            final View frame_indent = rootView.findViewById(R.id.frame_indent);
            pic_protect.setVisibility(View.GONE);
            frame_indent.setVisibility(View.VISIBLE);
        }
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

        vParent = rootView;
        adapter = new PhonesAdapter(getActivity());
        View ibtnAddPhone = vParent.findViewById(R.id.ibtn_addnumber);
        ibtnAddPhone.setOnClickListener(lsnr);
        View ibtnAddContact = vParent.findViewById(R.id.ibtn_addcontact);
        ibtnAddContact.setOnClickListener(lsnr);

        ListView lsv_phones = (ListView) vParent.findViewById(R.id.lsv_numbers);
        lsv_phones.setAdapter(adapter);

        View btn_test = vParent.findViewById(R.id.btn_sos);
        ((Button) btn_test).setTypeface(font);
        btn_test.setOnClickListener(lsnbbtn);
        View btn_next = vParent.findViewById(R.id.btn_next);
        ((Button) btn_next).setTypeface(font);
        btn_next.setOnClickListener(lsnbbtn);

        ((TextView) rootView.findViewById(R.id.textView)).setTypeface(font);
        ((EditText) rootView.findViewById(R.id.edt_addphone)).setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/RobotoCondensed-Light.ttf"));
        return rootView;
    }


}



