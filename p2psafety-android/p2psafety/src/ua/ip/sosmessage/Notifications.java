package ua.ip.sosmessage;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;

public class Notifications extends BroadcastReceiver{
    public static final int NOTIF_DELAYED_SOS_CODE = 100;
    public static final int NOTIF_SOS_STARTED_CODE = 101;
    public static final int NOTIF_SOS_CANCELED_CODE = 102;
    public static final int NOTIF_AUDIO_RECORD_CODE = 103;
    public static final int NOTIF_AUDIO_RECORD_FINISHED_CODE = 104;

    public static void notifDelayedSOS(Context context, long timeLeft, long timeTotal) {
        Intent notificationIntent = new Intent(context, SosActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(SosActivity.FRAGMENT_KEY, DelayedSosFragment.class.getName());
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Resources res = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(res.getString(R.string.sos_after)
                              .replace("#n#", String.valueOf(timeLeft / 1000 / 60)))
                .setWhen(System.currentTimeMillis() + timeLeft)
                .setAutoCancel(false)
                .setContentTitle(res.getString(R.string.sos))
                .setContentText(res.getString(R.string.sos_after)
                                   .replace("#n#", String.valueOf(timeLeft / 1000 / 60)))
                .setWhen(System.currentTimeMillis() + timeLeft)
                .setOngoing(true)
                .setProgress((int) (timeTotal / 1000), (int) ((timeTotal - timeLeft) / 1000), false)
                .setOnlyAlertOnce(false)
                .setPriority(1000);

        nm.notify(NOTIF_DELAYED_SOS_CODE, builder.build());
    }

    public static void removeNotification(Context context, int notificationId) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(notificationId);
    }

    public static void notifSosStarted(Context context) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Resources res = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(res.getString(R.string.sos_activated))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(res.getString(R.string.sos))
                .setContentText(res.getString(R.string.sos_activated))
                .setPriority(1000);

        nm.notify(NOTIF_SOS_STARTED_CODE, builder.build());
    }

    public static void notifSosCanceled(Context context) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Resources res = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(res.getString(R.string.sos_canceled))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(res.getString(R.string.sos))
                .setContentText(res.getString(R.string.sos_canceled))
                .setPriority(1000);

        nm.notify(NOTIF_SOS_CANCELED_CODE, builder.build());
    }

    public static void notifAudioRecording(Context context, long timeLeft, long timeTotal) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Resources res = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(res.getString(R.string.audio_is_recording)
                        .replace("#n#", String.valueOf(timeLeft / 1000 / 60)))
                .setWhen(System.currentTimeMillis() + timeLeft)
                .setAutoCancel(false)
                .setContentTitle(res.getString(R.string.sos))
                .setContentText(res.getString(R.string.audio_is_recording)
                        .replace("#n#", String.valueOf(timeLeft / 1000 / 60)))
                .setWhen(System.currentTimeMillis() + timeLeft)
                .setOngoing(true)
                .setProgress((int) (timeTotal / 1000), (int) ((timeTotal - timeLeft) / 1000), false)
                .setOnlyAlertOnce(true)
                .setPriority(1000);

        nm.notify(NOTIF_AUDIO_RECORD_CODE, builder.build());
    }

    public static void notifAudioRecordingFinished(Context context) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Resources res = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(res.getString(R.string.audio_record_finished))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(res.getString(R.string.sos))
                .setContentText(res.getString(R.string.audio_record_finished))
                .setPriority(1000);

        nm.notify(NOTIF_AUDIO_RECORD_FINISHED_CODE, builder.build());
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(DelayedSosService.SOS_DELAY_START)) {
            removeNotification(context, NOTIF_SOS_STARTED_CODE);
            removeNotification(context, NOTIF_DELAYED_SOS_CODE);
            notifDelayedSOS(context, DelayedSosService.getSosDelay(context),
                    DelayedSosService.getSosDelay(context));
        }
        if (action.equals(DelayedSosService.SOS_DELAY_TICK)) {
            // update notification only once in a minute
            if (DelayedSosService.getTimeLeft() / 1000 % 60 == 0) {
                notifDelayedSOS(context, DelayedSosService.getTimeLeft(),
                        DelayedSosService.getSosDelay(context));
            }
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_FINISH)) {
            removeNotification(context, NOTIF_DELAYED_SOS_CODE);
            notifSosStarted(context);
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_CANCEL)) {
            removeNotification(context, NOTIF_DELAYED_SOS_CODE);
        }
    }
}