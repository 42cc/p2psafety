package ua.p2psafety.data;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Ihor
 * Date: 10.10.12
 * Time: 14:51
 */
public class PhonesDatasourse {
    //old android api doesn't work with sets of strings

    private static String Pipe_Key_PREF = "Phones_key";
    private Context context;

    public PhonesDatasourse(Context context) {
        this.context = context;
    }

    private JSONArray getPhonesArr()
    {
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

    public void addPhone(String phone)  {
        JSONArray arr = getPhonesArr();
        arr.put(phone);
        getPrefs().edit().putString(Pipe_Key_PREF, arr.toString()).commit();

    }

    public void removePhone(String phone)  {
        JSONArray arr = getPhonesArr();
        JSONArray arr2 = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            String s = null;
            try {
                s = arr.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!phone.equals(s))
                arr2.put(s);
        }
        getPrefs().edit().putString(Pipe_Key_PREF, arr2.toString()).commit();

    }

    public List<String> getAllPhones() {
        JSONArray arr = getPhonesArr();
        List<String> phones = new ArrayList<String>();
        for (int i = 0; i < arr.length(); i++)
            try {
                phones.add(arr.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        return phones;
    }

}
