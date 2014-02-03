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

    public MessageResolver(Context context) {
        this.context = context;
        PhonesDatasourse phonesDatasourse = new PhonesDatasourse(context);
        EmailsDatasourse emailsDatasourse = new EmailsDatasourse(context);
        message = Prefs.getMessage(context);
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
        if (account!=null && emails.size() > 0)
        {
            String csv = emails.toString().replace("[", "").replace("]", "").replace(", ", ",");
            GmailOAuth2Sender gmailOAuth2Sender = new GmailOAuth2Sender(context);
            gmailOAuth2Sender.sendMail("SOS!!!", message, account, csv);
        }
    }

    // TODO: refactor this code or better whole MessageResolver
    // (split it into SMSSender & EmailSender?)
    public void sendMessages() {
        AsyncTask ast = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                Utils.startVibration(context);
                    if (Prefs.getIsLoc(context)) {
                        // loop until we have location
                        Looper.prepare();
                        MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
                            @Override
                            public void gotLocation(Location location) {
                                if (location != null) {
                                    String lat = location.getLatitude() + "";
                                    if (lat.length() > 10)
                                        lat = lat.substring(0, 9);
                                    String lon = location.getLongitude() + "";
                                    if (lon.length() > 10)
                                        lon = lon.substring(0, 9);

                                    Log.i("locationResult", "message: " + message);

                                    message = new StringBuilder().append(message)
                                            .append("\n")
                                            .append(formatTimeAndDay(location.getTime(), false))
                                            .append(" https://maps.google.com/maps?q=")
                                            .append(lat).append(",").append(lon).toString();

                                    Log.i("locationResult", "message: " + message);

                                    sendMessage(message);
                                    Log.d("Message", "1.0 Message sent" + message);
                                }
                            }
                        };
                        MyLocation myLocation = new MyLocation();
                        myLocation.getLocation(context, locationResult);
                        Looper.loop();
                    }

                    //sendMessage(message);
                    //Log.d("Message", "1.1 Message sent" + message);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Error", "while sending messages ", e);
                }
                return null;
            }
        };
        try {
            ast.execute();
        } catch (Exception e) {}
    }
}
