package ua.ip.sosmessage.data;

import android.content.Context;
import android.content.SharedPreferences;
import ua.ip.sosmessage.R;

/**
 * Created by ihorpysmennyi on 12/7/13.
 */
public class Prefs {
    public static String IS_LOC_KEY = "IS_LOC_KEY";
    public static String MSG = "MSG_KEY";
    public static String IS_FIRST_RUN = "FIRST_RUN_KEY";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("MobileExchange", 0);
    }

    public static void putIsLoc(Context context, boolean val) {
        getPrefs(context).edit().putBoolean(IS_LOC_KEY, val).commit();

    }

    public static void putMessage(Context context, String message) {
        getPrefs(context).edit().putString(MSG, message).commit();

    }

    public static boolean getIsLoc(Context context) {
        return getPrefs(context).getBoolean(IS_LOC_KEY, true);
    }

    public static String getMessage(Context context) {
        return getPrefs(context).getString(MSG, context.getString(R.string.sos_message));
    }

    public static boolean isFirstRun(Context context) {
        boolean b = getPrefs(context).getBoolean(IS_FIRST_RUN, true);
        if(b)
            getPrefs(context).edit().putBoolean(IS_FIRST_RUN, false).commit();
        return b;
    }
}
