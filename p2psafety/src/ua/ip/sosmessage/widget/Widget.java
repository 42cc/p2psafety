package ua.ip.sosmessage.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import ua.ip.sosmessage.R;
import ua.ip.sosmessage.sms.MessageResolver;

public class Widget extends AppWidgetProvider {

    private static final String SYNC_CLICKED    = "ua.ip.sosmessage.widget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews;
        ComponentName watchWidget;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        watchWidget = new ComponentName(context, Widget.class);

        remoteViews.setOnClickPendingIntent(R.id.btn_run, getPendingSelfIntent(context, SYNC_CLICKED));
        appWidgetManager.updateAppWidget(watchWidget, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);

        if (SYNC_CLICKED.equals(intent.getAction())) {
            MessageResolver resolver=new MessageResolver(context,false);
            resolver.sendMessages();
         /*   Intent myIntent=new Intent(context, SosActivity.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);      */

        }
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}