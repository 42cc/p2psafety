package ua.ip.sosmessage;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Melon on 10.01.14.
 */
public class StableArrayAdapter extends ArrayAdapter<String> {

    ArrayList<String> mSettingsList = new ArrayList<String>();

    public StableArrayAdapter(Context context, int textViewResourceId,
                              List<String> objects) {
        super(context, textViewResourceId, objects);
        mSettingsList = (ArrayList<String>) objects;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public String getItem(int position) {
        return mSettingsList.get(position);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
