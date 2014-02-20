package ua.p2psafety;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import ua.p2psafety.data.PhonesDatasourse;
import ua.p2psafety.util.Logs;

public class PhoneCallService extends Service {
    public static Logs LOGS;

    private BroadcastReceiver mPhoneCallReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        LOGS = new Logs(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // execute call
        startPhoneCall();

        // set lowest sound volume
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.NEW_OUTGOING_CALL");

        mPhoneCallReceiver = new OutgoingBroadcastReceiver();
        registerReceiver(mPhoneCallReceiver, filter);

        return START_STICKY;
    }

    private void startPhoneCall() {
        PhonesDatasourse phonesDatasourse = new PhonesDatasourse(this);
        List<String> phones = phonesDatasourse.getAllPhones();
        if (phones.size() > 0) {
            String phone_num = phones.get(0);
            String phoneCallUri = "tel:" + phone_num;
            Intent phoneCallIntent = new Intent(Intent.ACTION_CALL);
            phoneCallIntent.setData(Uri.parse(phoneCallUri));
            phoneCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(phoneCallIntent);
            } catch (Exception e) {
                LOGS.error("Can't start phone call intent", e);
            }
        }
    }

    private static class OutgoingBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Log.i("OutgoingBroadcastReceiver", "onReceive: " + intent.getAction());
            if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                AsyncTask ast = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        Log.i("AsyncTask", "doInBackground");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            LOGS.error("Thread.sleep error", e);
                        }
                        // hide call screen
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(startMain);

                        // start our app
                        startMain = new Intent(context, SosActivity.class);
                        //startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //startMain.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context.startActivity(startMain);

                        return null;
                    }
                };

                AsyncTaskExecutionHelper.executeParallel(ast);
            }
        }
    }

    private void stopPhoneCall() {
        try {
            //String serviceManagerName = "android.os.IServiceManager";
            String serviceManagerName = "android.os.ServiceManager";
            String serviceManagerNativeName = "android.os.ServiceManagerNative";
            String telephonyName = "com.android.internal.telephony.ITelephony";

            Class telephonyClass;
            Class telephonyStubClass;
            Class serviceManagerClass;
            Class serviceManagerStubClass;
            Class serviceManagerNativeClass;
            Class serviceManagerNativeStubClass;

            Method telephonyCall;
            Method telephonyEndCall;
            Method telephonyAnswerCall;
            Method getDefault;

            Method[] temps;
            Constructor[] serviceManagerConstructor;

            // Method getService;
            Object telephonyObject;
            Object serviceManagerObject;

            telephonyClass = Class.forName(telephonyName);
            telephonyStubClass = telephonyClass.getClasses()[0];
            serviceManagerClass = Class.forName(serviceManagerName);
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName);

            Method getService = // getDefaults[29];
                    serviceManagerClass.getMethod("getService", String.class);

            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod(
                    "asInterface", IBinder.class);

            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");

            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);

            telephonyObject = serviceMethod.invoke(null, retbinder);
            //telephonyCall = telephonyClass.getMethod("call", String.class);
            telephonyEndCall = telephonyClass.getMethod("endCall");
            //telephonyAnswerCall = telephonyClass.getMethod("answerRingingCall");

            telephonyEndCall.invoke(telephonyObject);

        } catch (Exception e) {
            e.printStackTrace();
            LOGS.error("Stop phone call error", e);
        }
    }

    @Override
    public void onDestroy() {
        stopPhoneCall();
        unregisterReceiver(mPhoneCallReceiver);

        if (LOGS != null)
            LOGS.close();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
