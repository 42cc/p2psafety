package ua.p2psafety;

import android.app.Application;

import com.bugsense.trace.BugSenseHandler;

import ua.p2psafety.data.Prefs;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.Utils;

/**
 * Created by Taras Melon on 14.03.14.
 */
public class MyApplication extends Application {

    private static Logs mLogs;

    @Override
    public void onCreate() {
        super.onCreate();
        BugSenseHandler.initAndStartSession(this, getString(R.string.bugsense_key));

        if (Utils.isServerAuthenticated(this))
        {
            if (Utils.isFbAuthenticated(this))
            {
                String uid = Prefs.getUserIdentifier(this);
                if (uid != null)
                {
                    Utils.putUidToBugSense(uid);
                }
                else
                {
                    Utils.getFbUserInfo(this);
                }
            }
            else
            {
                BugSenseHandler.setUserIdentifier(Prefs.getApiUsername(this));
            }
        }

        mLogs = new Logs(this);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                mLogs.error("UNCAUGHT EXCEPTION!!!", ex);
                System.exit(0);
            }
        });
    }
}
