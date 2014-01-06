package ua.ip.sosmessage.setphones;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import ua.ip.sosmessage.R;
import ua.ip.sosmessage.data.PhonesDatasourse;

import java.util.List;

/**
 * Created by ihorpysmennyi on 12/7/13.
 */
public class PhonesAdapter extends BaseAdapter {
    private PhonesDatasourse datasourse;
    private Typeface font;
    private List<String> items;
    private Context context;

    public PhonesAdapter(Context context) {
        this.context = context;
        this.datasourse = new PhonesDatasourse(context);
        this.items = datasourse.getAllPones();
        this.font = Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-Light.ttf");
    }

    public void addPhone(String phone) {
        datasourse.addPhone(phone);
        items.add(phone);
        notifyDataSetChanged();

    }

    public void removePhone(String phone) {
        datasourse.removePhone(phone);
        items.remove(phone);
        notifyDataSetChanged();

    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = newView(parent);
        } else {
            v = convertView;
        }
        bindView(v, position);
        return v;
    }

    private View newView(ViewGroup parent) {
        View v;
        LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.phone_list_item, parent, false);
        Log.w("VideoListAdapter", "added new View");
        return v;
    }

    private void bindView(View view, int position) {
        final TextView txt_phone = (TextView) view.findViewById(R.id.txt_phone);

        txt_phone.setText(items.get(position));
        txt_phone.setTypeface(font);
        View ibtn_del = view.findViewById(R.id.ibtn_del);
        ibtn_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removePhone(txt_phone.getText().toString());
            }
        });

    }
}
