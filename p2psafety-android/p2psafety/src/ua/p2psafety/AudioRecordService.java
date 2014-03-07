package ua.p2psafety;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import ua.p2psafety.Network.NetworkManager;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.Utils;

public class AudioRecordService extends Service {
    private static Boolean mTimerOn = false;
    private static long mDuration;
    private static long mTimeLeft = 0;

    private static AudioRecordTimer mTimer;

    public static Logs LOGS;

    private static MediaRecorder mRecorder;
    File mRecordFile = null;

    @Override
    public void onCreate() {
        super.onCreate();

        LOGS = new Logs(this);
    }

    public void onCreate(Context context) {
        super.onCreate();

        LOGS = new Logs(context);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGS.info("AudioRecordService started");
        if (!mTimerOn) {
            startRecording();
        }
        return START_STICKY;
    }

    private class AudioRecordTimer extends CountDownTimer {
        public AudioRecordTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            stopRecording(false);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mTimeLeft = millisUntilFinished;

            // update notification only once in a minute
            if (mTimeLeft / 1000 % 60 == 0)
                Notifications.notifAudioRecording(getApplicationContext(), mTimeLeft, mDuration);
        }
    }

    public void startRecording() {
        try {
            mDuration = Prefs.getMediaRecordLength(getApplicationContext());

            prepareRecorder();
            mRecorder.start();
            LOGS.info("Start recording audio");

            mTimeLeft = mDuration;
            mTimer = new AudioRecordTimer(mDuration, 1000);
            mTimer.start();
            mTimerOn = true;

            Notifications.notifAudioRecording(getApplicationContext(), mTimeLeft, mDuration);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Can't start audio recording", Toast.LENGTH_LONG)
                 .show();
            LOGS.error("Can't start audio recording", e);
        }
    }

    private void prepareRecorder() throws IOException {
        LOGS.info("Prepare recording audio");
        File mediaDir;
        String state = Environment.getExternalStorageState();
        if(state.equals(Environment.MEDIA_MOUNTED))
        {
            mediaDir = Environment.getExternalStorageDirectory();
            LOGS.info("Use external storage");
        }
        else
        {
            mediaDir = getFilesDir();
            LOGS.info("Use internal storage");
        }
        mRecordFile = File.createTempFile("sound", ".mp4", mediaDir);

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        int duration = (int) mDuration / 60000;
        boolean lowQuality = false;
        if (!Utils.isWiFiConnected(getApplicationContext(), LOGS))
        {
            lowQuality = true;
            LOGS.info("Low quality selected");
        }

        if (duration < 2 && !lowQuality) {
            mRecorder.setAudioEncodingBitRate(256000);
            mRecorder.setAudioSamplingRate(48000);
        } else if (duration < 5 && !lowQuality) {
            mRecorder.setAudioEncodingBitRate(128000);
            mRecorder.setAudioSamplingRate(32000);
        } else {
            mRecorder.setAudioEncodingBitRate(64000);
            mRecorder.setAudioSamplingRate(16000);
        }
        mRecorder.setOutputFile(mRecordFile.getAbsolutePath());
        mRecorder.prepare();
    }

    public void stopRecording(boolean isAlarmStop) {
        LOGS.info("Stop recording audio");
        mRecorder.stop();
        mRecorder.release();

        mTimer.cancel();
        if (isAlarmStop)
            mTimerOn = false;

        if (Utils.isServerAuthenticated(this)) {
            LOGS.info("AudioRecordService. User authenticated at server. Upload record");
            NetworkManager.updateEventWithAttachment(this, mRecordFile, true);
        } else {
            LOGS.info("AudioRecordService. User is NOT authenticated at server. " +  "" +
                      "Send record by Email");
            Utils.sendMailsWithAttachments(this, R.string.audio, mRecordFile);
        }

        Notifications.removeNotification(getApplicationContext(), Notifications.NOTIF_AUDIO_RECORD_CODE);
        Notifications.notifAudioRecordingFinished(getApplicationContext());

        if (!isAlarmStop) {
            LOGS.info("AudioRecordService. SOS is still active. Start a new record.");
            startRecording();
        }
    }

    @Override
    public void onDestroy() {
        LOGS.info("AudioRecordService. Service shutdown");
        if (mTimerOn) {
            LOGS.info("AudioRecordService. Record is on. Stop it");
            stopRecording(true);
        }
        if (LOGS != null)
            LOGS.close();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public static void setDuration(Context context, long val) {
        mDuration = val;
        //Prefs.putAudioDuration(context, mDuration);
    }

    public static long getAudioDuration(Context context) {
        if (mDuration == 0) {
            //mDuration = Prefs.getAudioDuration(context);
        }
        return mDuration;
    }
}