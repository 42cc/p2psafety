package ua.p2psafety.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.facebook.Session;

import java.util.List;

import ua.p2psafety.R;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.data.ServersDatasourse;
import ua.p2psafety.util.NetworkManager;
import ua.p2psafety.util.Utils;

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
        v = inflater.inflate(R.layout.server_list_item, parent, false);
        return v;
    }

    private void bindView(View view, final int position) {
        final String selectedServer = datasourse.getSelectedServer();
        final TextView txt_phone = (TextView) view.findViewById(R.id.txt_phone);

        txt_phone.setText(items.get(position));
        txt_phone.setTypeface(font);

        View ibtn_del = view.findViewById(R.id.ibtn_del);
        ibtn_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (items.get(position).equals(selectedServer))
                {
                    deleteServerSettings();
                }
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

        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
        if (items.get(position).equals(selectedServer))
        {
            checkBox.setChecked(true);
        }
        else
        {
            checkBox.setChecked(false);
        }
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                {
                    Utils.setLoading(context, true);
                    datasourse.setSelectedServer(items.get(position));
                    notifyDataSetChanged();
                    NetworkManager.getSettings(context, new NetworkManager
                            .DeliverResultRunnable<Boolean>() {

                        @Override
                        public void deliver(Boolean val) {
                            super.deliver(val);
                            Utils.setLoading(context, false);
                            if (val) {
                                Session session = new Session.Builder(context
                                ).setApplicationId(Prefs
                                        .getFbAppId(context)).build();
                                Session.setActiveSession(session);
                            }
                        }

                        @Override
                        public void onError(int errorCode) {
                            super.onError(errorCode);
                            Utils.setLoading(context, false);
                        }
                    });
                }
                else
                {
                    deleteServerSettings();
                }
            }
        });
    }

    private void deleteServerSettings() {
        Prefs.putFbAppId(context, null);
        Prefs.putXmppEventsNotifNode(context, null);
        Prefs.putXmppPubsubServer(context, null);
        Prefs.putXmppServer(context, null);
        datasourse.setSelectedServer(null);
    }

    private void saveSortedData() {
        for (String item: items)
            datasourse.removeServer(item);
        for (String item: items)
            datasourse.addServer(item);
    }
}
