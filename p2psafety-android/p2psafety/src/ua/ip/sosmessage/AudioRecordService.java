package ua.ip.sosmessage;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import ua.ip.sosmessage.data.Prefs;

public class AudioRecordService extends Service {
    public static final String AUDIO_RECORD_START = "ua.ip.sosmessage.AudioRecordService.TimerStart";
    public static final String AUDIO_RECORD_TICK = "ua.ip.sosmessage.AudioRecordService.TimerTick";
    public static final String AUDIO_RECORD_FINISH = "ua.ip.sosmessage.AudioRecordService.TimerFinish";
    public static final String AUDIO_RECORD_CANCEL = "ua.ip.sosmessage.AudioRecordService.TimerCancel";
    //public static final String AUDIO_RECORD_CHANGE = "ua.ip.sosmessage.AudioRecordService.TimerChange";

    private static Boolean mTimerOn = false;
    private static long mDuration = 2*1000*60;
    private static long mTimeLeft = 0;

    private static AudioRecordTimer mTimer;

    private static MediaRecorder mRecorder;
    File mRecordFile = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void onCreate(Context context) {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mTimerOn) {
            // let our notification manager know when things happen
            registerReceiver(getApplicationContext(), new Notifications());

            mTimeLeft = mDuration;
            mTimer = new AudioRecordTimer(mDuration, 1000);
            mTimer.start();
            mTimerOn = true;
            startRecording();

            Intent i = new Intent(AUDIO_RECORD_START);
            sendBroadcast(i);

            Log.i("AudioRecord", "onStartCommand");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public static void registerReceiver(Context context, BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioRecordService.AUDIO_RECORD_START);
        filter.addAction(AudioRecordService.AUDIO_RECORD_TICK);
        filter.addAction(AudioRecordService.AUDIO_RECORD_FINISH);
        filter.addAction(AudioRecordService.AUDIO_RECORD_CANCEL);
        context.registerReceiver(receiver, filter);
    }

    private class AudioRecordTimer extends CountDownTimer {
        public AudioRecordTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            stopRecording();
            Intent i = new Intent(AUDIO_RECORD_FINISH);
            sendBroadcast(i);
            mTimerOn = false;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mTimeLeft = millisUntilFinished;
            Intent i = new Intent(AUDIO_RECORD_TICK);
            sendBroadcast(i);
        }
    }

    public void startRecording() {
        Log.i("AudioRecord", "startrecording 1");
        File sampleDir = Environment.getExternalStorageDirectory();
        try {
            mRecordFile = File.createTempFile("sound", ".mp4", sampleDir);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Can't access SD card", Toast.LENGTH_LONG)
                 .show();
            return;
        }
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setAudioEncodingBitRate(128);
        mRecorder.setAudioEncodingBitRate(44100);
        mRecorder.setOutputFile(mRecordFile.getAbsolutePath());
        try {
            mRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mRecorder.start();

        Toast.makeText(getApplicationContext(), "Audio recording started", Toast.LENGTH_LONG)
             .show();

        Log.i("AudioRecord", "startrecording 2");
    }

    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        addRecordingToMediaLibrary();
        Toast.makeText(getApplicationContext(), "Audio recording is over", Toast.LENGTH_LONG)
             .show();
    }

    protected void addRecordingToMediaLibrary() {
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        values.put(MediaStore.Audio.Media.TITLE, "audio" + mRecordFile.getName());
        values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
        values.put(MediaStore.Audio.Media.DATA, mRecordFile.getAbsolutePath());
        ContentResolver contentResolver = getContentResolver();

        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri newUri = contentResolver.insert(base, values);

        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
        Toast.makeText(this, "Added File " + newUri, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        if (mTimerOn) {
            stopRecording();
            mTimer.cancel();
            mTimerOn = false;
            Intent i = new Intent(AUDIO_RECORD_CANCEL);
            sendBroadcast(i);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public static void setDuration(Context context, long val) {
        mDuration = val;
        //Prefs.putAudioDuration(context, mDuration);

        // send broadcast that audio time changed
        //Intent i = new Intent(AUDIO_RECORD_CHANGE);
        //context.sendBroadcast(i);

    }

    public static long getAudioDuration(Context context) {
        if (mDuration == 0) {
            //mDuration = Prefs.getAudioDuration(context);
        }
        return mDuration;
    }

    public static boolean isTimerOn() {
        return mTimerOn;
    }

    public static long getTimeLeft() {
        return mTimeLeft;
    }
}