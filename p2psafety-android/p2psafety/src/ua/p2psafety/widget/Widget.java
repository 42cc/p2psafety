package ua.p2psafety.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import ua.p2psafety.DelayedSosFragment;
import ua.p2psafety.DelayedSosService;
import ua.p2psafety.EventManager;
import ua.p2psafety.R;
import ua.p2psafety.SosActivity;

public class Widget extends AppWidgetProvider {
    private static final String WIDGET_CLICKED = "ua.p2psafety.widget";

    RemoteViews mRemoteViews = null;
    ComponentName mWatchWidget = null;
    Context mContext = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        mWatchWidget = new ComponentName(context, Widget.class);
        mContext = context;

        if (DelayedSosService.isTimerOn())
            showSosDelay(DelayedSosService.getTimeLeft());
        else
            showSosDelay(DelayedSosService.getSosDelay(mContext));

        mRemoteViews.setOnClickPendingIntent(R.id.btn_run, getPendingSelfIntent(context, WIDGET_CLICKED));
        mRemoteViews.setOnClickPendingIntent(R.id.timer_text, getPendingSelfIntent(context, WIDGET_CLICKED));
        appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);

        if (mRemoteViews == null || mWatchWidget == null || mContext == null) {
            mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
            mWatchWidget = new ComponentName(context, Widget.class);
            mContext = context;
        }

        String action = intent.getAction();

        if (action.equals(WIDGET_CLICKED)) {
            if (DelayedSosService.isTimerOn()) {
                // if delayed sos is on - show timer screen
                Intent i = new Intent(context, SosActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(SosActivity.FRAGMENT_KEY, DelayedSosFragment.class.getName());
                context.startActivity(i);
            }
            else if (EventManager.getInstance(mContext).isSosStarted()) {
                // if normal sos is already on - inform user
                String msg = mContext.getResources().getString(R.string.sos_already_active);
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG)
                     .show();
            } else {
                // if sos is off - start delay timer
                context.startService(new Intent(context, DelayedSosService.class));
            }
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_TICK)) {
            showSosDelay(DelayedSosService.getTimeLeft());
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_FINISH)) {
            showSosDelay(DelayedSosService.getSosDelay(mContext));
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_CANCEL)) {
            showSosDelay(DelayedSosService.getSosDelay(mContext));
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
        }
        else if (action.equals(DelayedSosService.SOS_DELAY_CHANGE)) {
            showSosDelay(DelayedSosService.getSosDelay(mContext));
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(mWatchWidget, mRemoteViews);
        }
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private void showSosDelay(long sosDelay) {
        String timerText = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toSeconds(sosDelay) / 60,
                TimeUnit.MILLISECONDS.toSeconds(sosDelay) % 60);
        mRemoteViews.setTextViewText(R.id.timer_text, timerText );
    }
}