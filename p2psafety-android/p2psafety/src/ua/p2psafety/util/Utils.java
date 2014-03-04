package ua.p2psafety.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;

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
    public static final int DIALOG_NETWORK_ERROR = 10;
    public static final int DIALOG_NO_CONNECTION = 100;

    static private ProgressDialog mProgressDialog;
    static private AlertDialog mErrorDialog;

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

    public static void playDefaultNotificationSound(Context context) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(context, notification);
        r.play();
    }

    public static void blinkLED(Context context) {
        NotificationManager notifMgr = (NotificationManager)
                context.getSystemService(context.NOTIFICATION_SERVICE);
        Notification notif = new Notification();
        notif.ledARGB = Color.argb(255, 0, 255, 0);
        notif.flags |= Notification.FLAG_SHOW_LIGHTS;
        notif.ledOnMS = 300;
        notif.ledOffMS = 200;
        notifMgr.notify(999, notif);
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

    public static void setLoading(Context context, boolean loading) {
        try {
            if (loading) {
                Activity activity = (Activity) context;
                mProgressDialog = new ProgressDialog(activity);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                mProgressDialog.setContentView(R.layout.loading_progressbar);
            } else {
                mProgressDialog.dismiss();
            }
        } catch (Exception e) {};
    }

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

    public static void errorDialog(final Context context, final int type) {
        try {
            final Activity activity = (Activity) context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mErrorDialog != null && mErrorDialog.isShowing())
                        return;

                    switch (type) {
                        case DIALOG_NETWORK_ERROR:
                            mErrorDialog = new AlertDialog.Builder(activity)
                                    .setMessage(R.string.network_error)
                                    .setNeutralButton(android.R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            dialog.dismiss();
                                        }
                                    }).show();
                            break;
                        case DIALOG_NO_CONNECTION:
                            mErrorDialog = new AlertDialog.Builder(activity)
                                    .setTitle(R.string.connection)
                                    .setMessage(R.string.connection_is_out)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setNeutralButton(R.string.connection_settings,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                    // open wi-fi settings
                                                    activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                                                }
                                            })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            dialog.dismiss();
                                        }
                                    }).show();
                            break;
                    }
                }
            });
        } catch (Exception e) {}
    }
}
