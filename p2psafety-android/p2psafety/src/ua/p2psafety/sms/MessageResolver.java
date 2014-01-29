package ua.p2psafety.sms;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import ua.p2psafety.R;
import ua.p2psafety.data.EmailsDatasourse;
import ua.p2psafety.data.PhonesDatasourse;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.util.Utils;

/**
 * Created by ihorpysmennyi on 12/7/13.
 */
public class MessageResolver {
    private Context context;
    private String message;
    private List<String> phones;
    private List<String> emails;

    public MessageResolver(Context context, boolean isTest) {
        this.context = context;
        PhonesDatasourse phonesDatasourse = new PhonesDatasourse(context);
        EmailsDatasourse emailsDatasourse = new EmailsDatasourse(context);
        message = Prefs.getMessage(context);
        if (isTest)
            message = context.getString(R.string.test_message) + " " + message;
        phones = new ArrayList<String>();
        phones = phonesDatasourse.getAllPhones();
        emails = emailsDatasourse.getAllEmails();
    }

    /**
     * @return time and day as "hh:mm, ddd" or "hh:mm:ss, ddd"
     */
    public static String formatTimeAndDay(final long timestamp, final boolean includeSeconds) {
        return (DateFormat.format("kk:mm" + (includeSeconds ? ".ss" : "") + ",E", timestamp).toString());
    }

    private void sendMessage(String message) {
        for (String phone : phones)
            SMSSender.send(phone, message, context);
        String account = Utils.getEmail(context);
        if (account!=null)
        {
            String csv = emails.toString().replace("[", "").replace("]", "").replace(", ", ",");
            GmailOAuth2Sender gmailOAuth2Sender = new GmailOAuth2Sender(context);
            gmailOAuth2Sender.sendMail("SOS!!!", message, account, csv);
        }
    }

    public void sendMessages() {
        AsyncTask ast = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    Log.d("Message1", message);
                    sendMessage(message);

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Error", "while sending SMS ", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                String s = context.getString(R.string.sms_sent);
//                if (Prefs.getIsLoc(context))
//                    s += "\n" + context.getString(R.string.loc_will_send);
                Toast.makeText(context, s, Toast.LENGTH_LONG).show();
            }
        };
        try {
            ast.execute();
        } catch (Exception e) {
        }
        if (Prefs.getIsLoc(context)) {

            try {

                AsyncTask astGeo = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        Looper.prepare();
                        MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
                            @Override
                            public void gotLocation(Location location) {
                                if (location!=null)
                                {
                                    String lat = location.getLatitude() + "";
                                    if (lat.length() > 10)
                                        lat = lat.substring(0, 9);
                                    String lon = location.getLongitude() + "";
                                    if (lon.length() > 10)
                                        lon = lon.substring(0, 9);
                                    message = formatTimeAndDay(location.getTime(), false) + " https://maps.google.com/maps?q="
                                            + lat + "," + lon;
                                    Log.d("Message1", "Message sent" + message);
                                    sendMessage(message);
                                }
                            }
                        };
                        MyLocation myLocation = new MyLocation();
                        myLocation.getLocation(context, locationResult);
                        Looper.loop();

                        return null;
                    }
                };
                if (android.os.Build.VERSION.SDK_INT < 11)
                    astGeo.execute();
                else
                    astGeo.executeOnExecutor(Executors.newCachedThreadPool());
            } catch (Exception e) {
                Toast.makeText(context, R.string.sms_failed, Toast.LENGTH_LONG).show();
            }

        }
    }

}
