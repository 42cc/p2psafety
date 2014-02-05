package ua.p2psafety.data;

import android.content.Context;
import android.content.SharedPreferences;

import ua.p2psafety.Event;
import ua.p2psafety.R;
import ua.p2psafety.User;

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
    private static final String SOS_STARTED_KEY = "SOS_STARTED";

    private static final String MEDIA_RECORD_TYPE = "MEDIA_RECORD_TYPE";
    private static final String MEDIA_RECORD_LENGTH = "MEDIA_RECORD_LENGTH";

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

    public static void putMediaRecordType(Context context, int val) {
        getPrefs(context).edit().putInt(MEDIA_RECORD_TYPE, val).commit();
    }

    public static void putMediaRecordLength(Context context, long val) {
        getPrefs(context).edit().putLong(MEDIA_RECORD_LENGTH, val).commit();
    }

    public static void putSosStarted(Context context, boolean val) {
        getPrefs(context).edit().putBoolean(SOS_STARTED_KEY, val).commit();
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
        return getPrefs(context).getLong(SOS_DELAY_KEY, 2*1000*60); // default is 2 min
    }

    public static String getPassword(Context context) {
        return getPrefs(context).getString(SOS_PASSWORD_KEY, "");
    }

    public static boolean getUsePassword(Context context) {
        return getPrefs(context).getBoolean(SOS_USE_PASSWORD_KEY, false);
    }

    public static int getMediaRecordType(Context context) {
        return getPrefs(context).getInt(MEDIA_RECORD_TYPE, 0);
    }

    public static long getMediaRecordLength(Context context) {
        return getPrefs(context).getLong(MEDIA_RECORD_LENGTH, 5*1000*60); // default is 5 min
    }

    public static boolean getSosStarted(Context context) {
        return getPrefs(context).getBoolean(SOS_STARTED_KEY, false);
    }


    //============= EVENT SAVING =========================
    private static final String EVENT_ID = "EVENT_ID";
    private static final String EVENT_STATUS = "EVENT_STATUS";
    private static final String EVENT_KEY = "EVENT_KEY";
    private static final String EVENT_URI = "EVENT_URI";

    public static void putEvent(Context context, Event event) {
        getPrefs(context).edit()
                .putString(EVENT_ID, event.getId())
                .putString(EVENT_KEY, event.getKey())
                .putString(EVENT_STATUS, event.getStatus())
                .putString(EVENT_URI, event.getUri())
                .commit();

        putUser(context, event.getUser());
    }

    public static Event getEvent(Context context) {
        // get data
        SharedPreferences prefs = getPrefs(context);
        String id = prefs.getString(EVENT_ID, null);
        String key = prefs.getString(EVENT_KEY, null);
        String status = prefs.getString(EVENT_STATUS, null);
        String uri = prefs.getString(EVENT_URI, null);
        User user = getUser(context);

        // validate data
        if (id == null || key == null || status == null || uri == null || user == null)
            return null;

        // build Event object
        Event event = new Event();
        event.setId(id);
        event.setKey(key);
        event.setStatus(status);
        event.setUri(uri);
        event.setUser(user);

        return event;
    }


    //============= USER SAVING =========================
    private static final String USER_ID = "USER_ID";
    private static final String USER_FULL_NAME = "USER_FULL_NAME";
    private static final String USER_URI = "USER_URI";

    private static void putUser(Context context, User user) {
        getPrefs(context).edit()
                .putString(USER_ID, user.getId())
                .putString(USER_FULL_NAME, user.getFullName())
                .putString(USER_URI, user.getUri())
                .commit();
    }

    private static User getUser(Context context) {
        // get data
        SharedPreferences prefs = getPrefs(context);
        String id = prefs.getString(USER_ID, null);
        String full_name = prefs.getString(USER_FULL_NAME, null);
        String uri = prefs.getString(USER_URI, null);

        // validate data
        if (id == null)
            return null;

        // build User object
        User user = new User();
        user.setId(id);
        user.setFullName(full_name);
        user.setUri(uri);

        return user;
    }
}