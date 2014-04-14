package ua.p2psafety.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ua.p2psafety.util.Logs;

public class ServersDatasourse {

    //old android api doesn't work with sets of strings
    private static String Pipe_Key_PREF = "Servers_key";
    private static Logs LOGS;
    private Context context;

    public ServersDatasourse(Context context) {
        this.context = context;

        LOGS = new Logs(context);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (LOGS != null)
            LOGS.close();
    }

    private JSONArray getServersArr() {
        String s = getPrefs().getString(Pipe_Key_PREF, "");
        if (s.length() == 0)
            s = new JSONArray().toString();
        JSONArray arr = null;
        try {
            arr = new JSONArray(s);
        } catch (JSONException e) {
            e.printStackTrace();
            LOGS.error("Can't create JSON array", e);
        }
        return arr;
    }

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences("MobileExchange", 0);
    }

    public void addServer(String address) {
        JSONArray arr = getServersArr();
        arr.put(address);
        getPrefs().edit().putString(Pipe_Key_PREF, arr.toString()).commit();

    }

    public void removeServer(String address) {
        JSONArray arr = getServersArr();
        JSONArray arr2 = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            String s = null;
            try {
                s = arr.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
                LOGS.error("Can't get string from JSON array", e);
            }
            if (!address.equals(s))
                arr2.put(s);
        }
        getPrefs().edit().putString(Pipe_Key_PREF, arr2.toString()).commit();

    }

    public List<String> getAllServers() {
        JSONArray arr = getServersArr();
        List<String> servers = new ArrayList<String>();
        for (int i = 0; i < arr.length(); i++)
            try {
                servers.add(arr.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
                LOGS.error("Can't get string from JSON array", e);
            }
        return servers;
    }
}