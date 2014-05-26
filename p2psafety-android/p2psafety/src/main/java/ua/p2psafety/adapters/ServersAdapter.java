package ua.p2psafety.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;

import java.util.List;

import ua.p2psafety.R;
import ua.p2psafety.SosActivity;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.data.ServersDatasourse;
import ua.p2psafety.services.DelayedSosService;
import ua.p2psafety.services.XmppService;
import ua.p2psafety.util.EventManager;
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
    private Context mContext;

    public ServersAdapter(Context mContext) {
        this.mContext = mContext;
        this.datasourse = new ServersDatasourse(mContext);
        this.items = datasourse.getAllServers();
        this.font = Typeface.createFromAsset(mContext.getAssets(), "fonts/RobotoCondensed-Light.ttf");
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
    public String getItem(int position) {
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
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                if (Utils.isServerAuthenticated(mContext))
                {
                    Toast.makeText(mContext, R.string.please_first_logout, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (items.get(position).equals(selectedServer))
                {
                    deleteServerSettings();
                }
                removeServer(txt_phone.getText().toString());
            }
        });

        // don't allow delete default server
        if (items.get(position).equals(ServersDatasourse.DEFAULT_SERVER))
            ibtn_del.setVisibility(View.INVISIBLE);
        else
            ibtn_del.setVisibility(View.VISIBLE);

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

        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
        if (items.get(position).equals(selectedServer))
        {
            checkBox.setChecked(true);
        }
        else
        {
            checkBox.setChecked(false);
        }
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check if we can switch server now
                EventManager eventManager = EventManager.getInstance(mContext);
                if (eventManager.isSosStarted() || eventManager.isPassiveSosStarted() ||
                    eventManager.isSupportStarted() || DelayedSosService.isTimerOn())
                {
                    checkBox.setChecked(!checkBox.isChecked());
                    // tell user we can't
                    Toast.makeText(mContext, R.string.no_settings_while_sos, Toast.LENGTH_LONG)
                         .show();
                    return;
                }
                // logout if needed
                if (Utils.isServerAuthenticated(mContext)){
                    Utils.logout(mContext);
                }
                // switch server
                if (checkBox.isChecked()) {
                    Utils.setLoading(mContext, true);
                    datasourse.setSelectedServer(items.get(position));
                    notifyDataSetChanged();
                    NetworkManager.getSettings(mContext, new NetworkManager
                            .DeliverResultRunnable<Void>() {

                        @Override
                        public void deliver(Void result) {
                            Utils.setLoading(mContext, false);
                            Session session = new Session.Builder(mContext)
                                    .setApplicationId(Prefs.getFbAppId(mContext))
                                    .build();
                            Session.setActiveSession(session);
                        }

                        @Override
                        public void onError(int errorCode) {
                            Utils.setLoading(mContext, false);
                            datasourse.setSelectedServer(null);
                            notifyDataSetChanged();
                            if (errorCode != 0) { // server responded, but it is not settings
                                mContext.startService(new Intent(mContext, XmppService.class));
                                Toast.makeText(mContext, R.string.no_settings,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else {
                    deleteServerSettings();
                }
            }
        });
    }

    private void deleteServerSettings() {
        SosActivity.mLogs.info("Deleting info about server");
        Prefs.putFbAppId(mContext, null);
        Prefs.putXmppEventsNotifNode(mContext, null);
        Prefs.putXmppPubsubServer(mContext, null);
        Prefs.putXmppServer(mContext, null);
        datasourse.setSelectedServer(null);
        mContext.stopService(new Intent(mContext, XmppService.class));
    }

    private void saveSortedData() {
        for (String item: items)
            datasourse.removeServer(item);
        for (String item: items)
            datasourse.addServer(item);
    }
}
