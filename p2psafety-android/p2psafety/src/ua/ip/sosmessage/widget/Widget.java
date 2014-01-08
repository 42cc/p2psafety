package ua.ip.sosmessage.widget;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import ua.ip.sosmessage.DelayedSosFragment;
import ua.ip.sosmessage.DelayedSosService;
import ua.ip.sosmessage.R;
import ua.ip.sosmessage.SosActivity;
import ua.ip.sosmessage.sms.MessageResolver;

public class Widget extends AppWidgetProvider {
    private static final String SYNC_CLICKED    = "ua.ip.sosmessage.widget";

    RemoteViews mRemoteViews = null;
    ComponentName mWatchWidget = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        mWatchWidget = new ComponentName(context, Widget.class);

        if (DelayedSosService.mTimerOn != true)
            showSosDelay();

        mRemoteViews.setOnClickPendingIntent(R.id.btn_run, getPendingSelfIntent(context, SYNC_CLICKED));
        appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);

        if (mRemoteViews == null || mWatchWidget == null) {
            mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
            mWatchWidget = new ComponentName(context, Widget.class);
        }

        String action = intent.getAction();

        if (action.equals(SYNC_CLICKED)) {
            if (DelayedSosService.mTimerOn) {
                Intent i = new Intent(context, SosActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(SosActivity.FRAGMENT_KEY, DelayedSosFragment.class.getName());
                context.startActivity(i);
            }
            else
                context.startService(new Intent(context, DelayedSosService.class));

            //MessageResolver resolver=new MessageResolver(context,false);
            //resolver.sendMessages();
         /*   Intent myIntent=new Intent(context, SosActivity.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);      */
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_TICK)) {
            long millisUntilFinished = DelayedSosService.mTimeLeft;

            String timerText = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) / 60,
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60);
            mRemoteViews.setTextViewText(R.id.timer_text, timerText );

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_FINISH)) {
            showSosDelay();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_CANCEL)) {
            showSosDelay();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
        }
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private void showSosDelay() {
        long sosDelay = DelayedSosService.mSosDelay;
        String timerText = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toSeconds(sosDelay) / 60,
                TimeUnit.MILLISECONDS.toSeconds(sosDelay) % 60);
        mRemoteViews.setTextViewText(R.id.timer_text, timerText );
    }
}