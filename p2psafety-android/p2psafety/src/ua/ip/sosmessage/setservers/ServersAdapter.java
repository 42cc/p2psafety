package ua.ip.sosmessage.setservers;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ua.ip.sosmessage.R;
import ua.ip.sosmessage.data.EmailsDatasourse;
import ua.ip.sosmessage.data.ServersDatasourse;

/**
 * @author Taras Melon
 * @since 2014-01-09
 */
public class ServersAdapter extends BaseAdapter {
    private ServersDatasourse datasourse;
    private Typeface font;
    private List<String> items;
    private Context context;

    public ServersAdapter(Context context) {
        this.context = context;
        this.datasourse = new ServersDatasourse(context);
        this.items = datasourse.getAllServers();
        this.font = Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-Light.ttf");
    }

    public void addServer(String address) {
        datasourse.addServer(address);
        items.add(address);
        notifyDataSetChanged();

    }

    public void removeServer(String address) {
        datasourse.removeServer(address);
        items.remove(address);
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
        return v;
    }

    private void bindView(View view, final int position) {
        final TextView txt_phone = (TextView) view.findViewById(R.id.txt_phone);

        txt_phone.setText(items.get(position));
        txt_phone.setTypeface(font);

        View ibtn_del = view.findViewById(R.id.ibtn_del);
        ibtn_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeServer(txt_phone.getText().toString());
            }
        });

        View arrowUp = view.findViewById(R.id.arrowUpBtn);
        arrowUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position > 0) {
                    String curItem = items.get(position);
                    String prevItem = items.get(position-1);
                    items.set(position, prevItem);
                    items.set(position-1, curItem);
                    saveSortedData();
                    notifyDataSetChanged();
                }
            }
        });

        View arrowDown = view.findViewById(R.id.arrowDownBtn);
        arrowDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position < items.size()-1) {
                    String curItem = items.get(position);
                    String nextItem = items.get(position+1);
                    items.set(position, nextItem);
                    items.set(position+1, curItem);
                    saveSortedData();
                    notifyDataSetChanged();
                }
            }
        });
    }

    private void saveSortedData() {
        for (String item: items)
            datasourse.removeServer(item);
        for (String item: items)
            datasourse.addServer(item);
    }
}
