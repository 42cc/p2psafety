package ua.p2psafety.data;

import android.content.Context;
import android.content.SharedPreferences;

import ua.p2psafety.R;

/**
 * Created by ihorpysmennyi on 12/7/13.
 */
public class Prefs {
    private static final String IS_LOC_KEY = "IS_LOC_KEY";
    private static final String MSG = "MSG_KEY";
    private static final String IS_FIRST_RUN = "FIRST_RUN_KEY";

    private static final String SOS_DELAY_KEY = "SOS_DELAY";
    private static final String SOS_PASSWORD_KEY = "SOS_PASSWORD";
    private static final String SOS_USE_PASSWORD_KEY = "SOS_USE_PASSWORD";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("MobileExchange", 0);
    }

    public static void putIsLoc(Context context, boolean val) {
        getPrefs(context).edit().putBoolean(IS_LOC_KEY, val).commit();
    }

    public static void putMessage(Context context, String message) {
        getPrefs(context).edit().putString(MSG, message).commit();
    }

    public static void putSosDelay(Context context, long val) {
        getPrefs(context).edit().putLong(SOS_DELAY_KEY, val).commit();
    }

    public static void putPassword(Context context, String val) {
        getPrefs(context).edit().putString(SOS_PASSWORD_KEY, val).commit();
    }

    public static void putUsePassword(Context context, boolean val) {
        getPrefs(context).edit().putBoolean(SOS_USE_PASSWORD_KEY, val).commit();
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

    public static long getSosDelay(Context context) {
        return getPrefs(context).getLong(SOS_DELAY_KEY, 2*60*1000); // default is 2 min
    }

    public static String getPassword(Context context) {
        return getPrefs(context).getString(SOS_PASSWORD_KEY, "");
    }

    public static boolean getUsePassword(Context context) {
        return getPrefs(context).getBoolean(SOS_USE_PASSWORD_KEY, false);
    }
}