package ua.p2psafety;

import android.os.AsyncTask;
import android.os.Build;

import java.util.concurrent.Executor;

/**
 * Created by Taras Melon on 13.02.14.
 */
public class AsyncTaskExecutionHelper {
    static class HoneycombExecutionHelper {
        public static <P> void execute(AsyncTask<P, ?, ?> asyncTask, boolean parallel, P... params) {
            SosActivity.LOGS.info("HoneycombExecutionHelper started");
            Executor executor = parallel ? AsyncTask.THREAD_POOL_EXECUTOR : AsyncTask.SERIAL_EXECUTOR;
            asyncTask.executeOnExecutor(executor, params);
            SosActivity.LOGS.info("HoneycombExecutionHelper ended");
        }
    }

    public static <P> void executeParallel(AsyncTask<P, ?, ?> asyncTask, P... params) {
        execute(asyncTask, true, params);
    }

    public static <P> void executeSerial(AsyncTask<P, ?, ?> asyncTask, P... params) {
        execute(asyncTask, false, params);
    }

    private static <P> void execute(AsyncTask<P, ?, ?> asyncTask, boolean parallel, P... params) {
        SosActivity.LOGS.info("AsyncTask executed");
        if (Build.VERSION.SDK_INT >= 11) {
            HoneycombExecutionHelper.execute(asyncTask, parallel, params);
        } else {
            asyncTask.execute(params);
        }
    }
}
