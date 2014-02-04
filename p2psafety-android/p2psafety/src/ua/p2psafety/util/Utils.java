package ua.p2psafety.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Vibrator;

import java.io.File;

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

}
