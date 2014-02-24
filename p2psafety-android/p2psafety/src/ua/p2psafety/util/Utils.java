package ua.p2psafety.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.facebook.Session;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ua.p2psafety.AsyncTaskExecutionHelper;
import ua.p2psafety.R;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.data.ServersDatasourse;
import ua.p2psafety.sms.MessageResolver;

/**
 * Created by Taras Melon on 10.01.14.
 */
public class Utils {

    public static String getEmail(Context context) {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType("com.google");
        if (accounts != null && accounts.length > 0)
            return accounts[0].name;
        else
            return null;
    }

    public static void showNoAccountDialog(Context context) {
        AlertDialog.Builder noEmailDialog = new AlertDialog.Builder(context);
        noEmailDialog.setMessage(context.getResources().getString(R.string.no_account_message))
                .setNeutralButton("OK", null)
                .show();
    }

    public static boolean isEmailAddress(String text) {
        String pattern = "^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})$";
        return text.matches(pattern);
    }

    // checks if network connection is available and connected
    public static boolean isNetworkConnected(Context context, Logs logs) {
        boolean result = false;

        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (netInfo != null && netInfo.isConnected()) {
                result = true;
            } else {
                netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (netInfo != null && netInfo.isConnected())
                    result = true;
            }
        } catch (Exception e) {
            logs.error("Can't check for network connection", e);
            return false;
        }

        return result;
    }

    public static boolean isWiFiConnected(Context context, Logs logs) {
        boolean result = false;

        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (netInfo != null && netInfo.isConnected()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logs.error("Can't check wifi connection", e);
            return false;
        }
    }

    public static void startVibration(final Context context)
    {
        AsyncTask<Integer, Void, Void> vibration = new AsyncTask<Integer, Void, Void>() {

            @Override
            protected Void doInBackground(Integer... params) {
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null)
                    v.vibrate(params[0]);
                return null;
            }
        };

        // Vibrate for 2000 milliseconds
        AsyncTaskExecutionHelper.executeParallel(vibration, 2000);
    }

    public static void sendMailsWithAttachments(final Context context, final int mediaId, final File file) {
        AsyncTask ast = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                ServersDatasourse serversDatasourse = new ServersDatasourse(context);
                if (serversDatasourse.getAllServers().size() == 0)
                {
                    MessageResolver resolver = new MessageResolver(context);
                    resolver.sendEmails(context.getString(R.string.recorded_media).replace("#media#", context.getString(mediaId)), file);
                }
                return null;
            }
        };
        AsyncTaskExecutionHelper.executeParallel(ast);
    }

    public static void checkForLocationServices(Context context)
    {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            showLocationSourceSettingsDialog(context);
    }

    private static void showLocationSourceSettingsDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.location_services_not_active));
        builder.setMessage(context.getString(R.string.please_enable_location_services));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                // Show location settings when the user acknowledges the alert dialog
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
            }
        });
        Dialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public static boolean isFbAuthenticated(Context context) {
        Session currentSession = Session.openActiveSessionFromCache(context);

        if (currentSession == null) {
            SharedPreferences sharedPref =
                    PreferenceManager.getDefaultSharedPreferences(context);
            sharedPref.edit().putString("MYSELF_KEY", "").commit();
        }

        return currentSession != null && currentSession.getState().isOpened();
    }

    public static boolean isServerAuthenticated(Context context) {
        return (Prefs.getApiKey(context) != null);
    }

//    public static void setLoading(Activity activity, boolean visible) {
//        if (activity != null)
//            activity.findViewById(R.id.loading_view)
//                .setVisibility(visible ? View.VISIBLE : View.GONE);
//    }

    public static void logKeyHash(Context context, Logs logs) {
        final String TAG = "logKeyHash()";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    "ua.p2psafety", PackageManager.GET_SIGNATURES
            );
            for (Signature signature: info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i(TAG, "KeyHash:" +
                        Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        }
        catch (Exception e) {
            logs.error("Can't get key hash", e);
        }
    }

    public static boolean isServiceRunning(Context context, Class service) {
        String service_name = service.getName();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo running_service : manager.getRunningServices(Integer.MAX_VALUE)) {
            String running_service_name = running_service.service.getClassName();
            if (running_service_name.equals(service_name))
                return true;
        }
        return false;
    }
}
