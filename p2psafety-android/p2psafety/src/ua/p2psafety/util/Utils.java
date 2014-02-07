package ua.p2psafety.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;

import com.facebook.Session;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ua.p2psafety.R;
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
    public static boolean isNetworkConnected(Context context) {
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
            return false;
        }

        return result;
    }

    public static boolean isWiFiConnected(Context context) {
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
            return false;
        }
    }

    public static void startVibration(final Context context)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 2000 milliseconds
                if (v != null)
                    v.vibrate(2000);
            }
        }).start();
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
        try {
            ast.execute();
        } catch (Exception e) {}
    }

//    public static boolean isFbAuthenticated(Context context) {
//        Session currentSession = Session.openActiveSessionFromCache(context);
//
//        if (currentSession == null) {
//            SharedPreferences sharedPref =
//                    PreferenceManager.getDefaultSharedPreferences(context);
//            sharedPref.edit().putString("MYSELF_KEY", "").commit();
//        }
//
//        return currentSession != null && currentSession.getState().isOpened();
//    }

//    public static void setLoading(Activity activity, boolean visible) {
//        if (activity != null)
//            activity.findViewById(R.id.loading_view)
//                .setVisibility(visible ? View.VISIBLE : View.GONE);
//    }

    public static void logKeyHash(Context context) {
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
        catch (PackageManager.NameNotFoundException e) {
            Log.i("KeyHash:", "NameNotFound");
        }
        catch (NoSuchAlgorithmException e) {
            Log.i("KeyHash:", "NoAlgo");
        }
        catch (NullPointerException e) {
            Log.i(TAG, "NullPonterException  " +
                    "SHOULD HAPPEN ONLY UNDER ROBOLECTRIC");
        }
    }
}
