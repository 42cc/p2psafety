package ua.ip.sosmessage.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Taras Melon
 * @since 2014-01-09
 */
public class EmailsDatasourse {

    //old android api doesn't work with sets of strings
    private static String Pipe_Key_PREF = "Emails_key";
    private Context context;

    public EmailsDatasourse(Context context) {
        this.context = context;
    }

    private JSONArray getEmailsArr() {
        String s = getPrefs().getString(Pipe_Key_PREF, "");
        if (s.length() == 0)
            s = new JSONArray().toString();
        JSONArray arr = null;
        try {
            arr = new JSONArray(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return arr;
    }

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences("MobileExchange", 0);
    }

    public void addEmail(String email) {
        JSONArray arr = getEmailsArr();
        arr.put(email);
        getPrefs().edit().putString(Pipe_Key_PREF, arr.toString()).commit();

    }

    public void removeEmail(String email) {
        JSONArray arr = getEmailsArr();
        JSONArray arr2 = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            String s = null;
            try {
                s = arr.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!email.equals(s))
                arr2.put(s);
        }
        getPrefs().edit().putString(Pipe_Key_PREF, arr2.toString()).commit();

    }

    public List<String> getAllEmails() {
        JSONArray arr = getEmailsArr();
        List<String> emails = new ArrayList<String>();
        for (int i = 0; i < arr.length(); i++)
            try {
                emails.add(arr.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        return emails;
    }
}
