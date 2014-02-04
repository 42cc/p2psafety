package ua.p2psafety.Network;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.facebook.Session;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ua.p2psafety.Event;
import ua.p2psafety.SosManager;
import ua.p2psafety.data.Prefs;

public class NetworkManager {
    private static final String SERVER_URL = "http://p2psafety.staging.42cc.co";

    private static final int CODE_SUCCESS = 201;

    private static int DIALOG_NETWORK_ERROR = 10;
    private static int DIALOG_NO_CONNECTION = 100;

    private static HttpClient httpClient;
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private static ObjectMapper mapper = new ObjectMapper();

    public static void init(Context c) {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 0);
        HttpConnectionParams.setSoTimeout(httpParams, 0);
        httpClient = new DefaultHttpClient(httpParams);
    }

    public static void createEvent(final Activity context,
                                   final DeliverResultRunnable<Event> postRunnable) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String TAG = "createEvent";

//                if (!Utils.isNetworkConnected(context)) {
//                    errorDialog(context, DIALOG_NO_CONNECTION);
//                    return;
//                }

                String access_token = Session.getActiveSession().getAccessToken();

                try {
                    HttpPost httpPost = new HttpPost(new StringBuilder().append(SERVER_URL)
                            .append("/api/v1/events/").toString());

                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");

                    JSONObject json = new JSONObject();
                    json.put("provider", "facebook");
                    json.put("access_token", access_token);
                    StringEntity se = new StringEntity(json.toString());
                    httpPost.setEntity(se);

                    Log.i(TAG, "request: " + httpPost.getRequestLine().toString());
                    Log.i(TAG, "request entity: " + EntityUtils.toString(httpPost.getEntity()));

                    HttpResponse response = null;
                    try {
                        response = httpClient.execute(httpPost);
                    } catch (Exception e) {
                        errorDialog(context, DIALOG_NETWORK_ERROR);
                        return;
                    }

                    int responseCode = response.getStatusLine().getStatusCode();
                    String responseContent = EntityUtils.toString(response.getEntity());
                    Log.i(TAG, "responseCode: " + responseCode);
                    Log.i(TAG, "responseContent: " + responseContent);

                    if (responseCode == CODE_SUCCESS) {
                        Map<String, Object> data = mapper.readValue(responseContent, Map.class);
                        Event event = JsonHelper.jsonToEvent(data);
                        data.clear();

                        postRunnable.setResult(event);
                    } else {
                        postRunnable.setResult(null);
                    }

                    context.runOnUiThread(postRunnable);
                } catch (Exception e) {
                    errorDialog(context, DIALOG_NETWORK_ERROR);
                }
            }
        });
    }

    public static void updateEvent(final Context context,
                                   final Map data,
                                   final DeliverResultRunnable<Boolean> postRunnable) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String TAG = "updateEvent";

//                if (!Utils.isNetworkConnected(context)) {
//                    errorDialog(context, DIALOG_NO_CONNECTION);
//                    return;
//                }

                Event event = SosManager.getInstance(context).getEvent();

                try {
                    HttpPost httpPost = new HttpPost(new StringBuilder().append(SERVER_URL)
                            .append("/api/v1/eventupdates/").toString());

                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");

                    JSONObject json = new JSONObject();
                    json.put("key", event.getKey());
                    json.put("text", Prefs.getMessage(context));
                    StringEntity se = new StringEntity(json.toString(), "UTF-8");
                    httpPost.setEntity(se);

                    Log.i(TAG, "request: " + httpPost.getRequestLine().toString());
                    Log.i(TAG, "request entity: " + EntityUtils.toString(httpPost.getEntity()));

                    HttpResponse response = null;
                    try {
                        response = httpClient.execute(httpPost);
                    } catch (Exception e) {
                        errorDialog(context, DIALOG_NETWORK_ERROR);
                        return;
                    }

                    int responseCode = response.getStatusLine().getStatusCode();
                    String responseContent = EntityUtils.toString(response.getEntity());
                    Log.i(TAG, "responseCode: " + responseCode);
                    Log.i(TAG, "responseContent: " + responseContent);

                    if (responseCode == CODE_SUCCESS) {
                        postRunnable.setResult(true);
                    } else {
                        postRunnable.setResult(false);
                    }

                    postRunnable.run();
                } catch (Exception e) {
                    errorDialog(context, DIALOG_NETWORK_ERROR);
                }
            }
        });
    }

    private static void errorDialog(final Context context, final int type) {
        if (context == null)
            return;

        Activity activity = null;
        try {
            activity = (Activity) context;
        } catch (Exception e) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                try {
//                    final ETAApplication app = (ETAApplication) ((Activity) context).getApplication();
//                    if (app.dialogShown)
//                        return;
//
//                    if (type == DIALOG_NETWORK_ERROR) {
//                        app.dialogShown = true;
//
//                        new AlertDialog.Builder(context)
//                                .setMessage(R.string.network_error)
//                                .setNeutralButton(android.R.string.ok,
//                                        new DialogInterface.OnClickListener() {
//                                            @Override
//                                            public void onClick(DialogInterface dialog, int which) {
//                                                dialog.dismiss();
//                                                app.dialogShown = false;
//                                            }
//                                        })
//                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
//                                    @Override
//                                    public void onCancel(DialogInterface dialog) {
//                                        dialog.dismiss();
//                                        app.dialogShown = false;
//                                    }
//                                }).show();
//                    }
//
//                    if (type == DIALOG_NO_CONNECTION) {
//                        app.dialogShown = true;
//
//                        new AlertDialog.Builder(context)
//                                .setTitle(R.string.connection)
//                                .setMessage(R.string.connection_is_out)
//                                .setIcon(android.R.drawable.ic_dialog_alert)
//                                .setNeutralButton(R.string.connection_settings,
//                                        new DialogInterface.OnClickListener() {
//                                            @Override
//                                            public void onClick(DialogInterface dialog, int which) {
//                                                dialog.dismiss();
//                                                app.dialogShown = false;
//
//                                                // open wi-fi settings
//                                                ((Activity) context)
//                                                        .startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
//                                            }
//                                        })
//                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
//                                    @Override
//                                    public void onCancel(DialogInterface dialog) {
//                                        dialog.dismiss();
//                                        app.dialogShown = false;
//                                    }
//                                }).show();
//                    }
//                } catch (Exception e) {
//                    // if activity is already destroyed, we don't need to show any dialogs
//                    // let them go :)
//                }
            }
        });
    }

    public static abstract class DeliverResultRunnable<Result> implements Runnable {

        private Result result;
        private boolean success = true;
        private int errorCode = -1;

        public void setResult(Result result) {
            this.result = result;
        }

        @Override
        public final void run() {
            if (success) {
                deliver(result);
            } else {
                onError(errorCode);
            }
        }

        public void setUnsuccessful(int errorCode) {
            this.success = false;
            this.errorCode = errorCode;
        }

        public abstract void deliver(Result result);

        public void onError(int errorCode) { }

    }

}
