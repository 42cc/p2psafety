package ua.p2psafety;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

public class VideoRecordService extends Service implements SurfaceHolder.Callback {
    public static final String VIDEO_RECORD_START = "ua.p2psafety.VideoRecordService.TimerStart";
    public static final String VIDEO_RECORD_TICK = "ua.p2psafety.VideoRecordService.TimerTick";
    public static final String VIDEO_RECORD_FINISH = "ua.p2psafety.VideoRecordService.TimerFinish";
    public static final String VIDEO_RECORD_CANCEL = "ua.p2psafety.VideoRecordService.TimerCancel";
    //public static final String VIDEO_RECORD_CHANGE = "ua.p2psafety.VideoRecordService.TimerChange";

    private static Boolean mTimerOn = false;
    private static long mDuration = 2*1000*60;
    private static long mTimeLeft = 0;

    private static VideoRecordTimer mTimer = null;

    private static MediaRecorder mRecorder = null;
    private Camera mCamera = null;
    private WindowManager mWindowManager = null;
    private SurfaceView mSurfaceView = null;

    private File mRecordFile = null;

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

            // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
            mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
            mSurfaceView = new SurfaceView(this);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    50, 50,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );
            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            mWindowManager.addView(mSurfaceView, layoutParams);
            mSurfaceView.getHolder().addCallback(this);
            mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public static void registerReceiver(Context context, BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(VideoRecordService.VIDEO_RECORD_START);
        filter.addAction(VideoRecordService.VIDEO_RECORD_TICK);
        filter.addAction(VideoRecordService.VIDEO_RECORD_FINISH);
        filter.addAction(VideoRecordService.VIDEO_RECORD_CANCEL);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("VideoRecord", "surfaceChanged");
        // TODO: move this stuff to after record started successfully
        mTimeLeft = mDuration;
        mTimer = new VideoRecordTimer(mDuration, 1000);
        mTimer.start();
        mTimerOn = true;
        startRecording(holder);

        Intent i = new Intent(VIDEO_RECORD_START);
        sendBroadcast(i);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private class VideoRecordTimer extends CountDownTimer {
        public VideoRecordTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            stopRecording();
            Intent i = new Intent(VIDEO_RECORD_FINISH);
            sendBroadcast(i);
            mTimerOn = false;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mTimeLeft = millisUntilFinished;
            Intent i = new Intent(VIDEO_RECORD_TICK);
            sendBroadcast(i);
        }
    }

    public void startRecording(SurfaceHolder holder) {
        Log.i("VideoRecord", "startrecording 1");
        Log.i("VideoRecord", "startrecording 1.0");
        File sampleDir = Environment.getExternalStorageDirectory();
        Log.i("VideoRecord", "startrecording 1.1");
        try {
            mRecordFile = File.createTempFile("video", ".mp4", sampleDir);
        } catch (Exception e) {
            Log.i("VideoRecord", "SHIT HAPPENED 3 !!!");
            Toast.makeText(getApplicationContext(), "Can't access SD card", Toast.LENGTH_LONG)
                 .show();
            return;
        }
        Log.i("VideoRecord", "startrecording 1.2");
        mRecorder = new MediaRecorder();

        Log.i("VideoRecord", "startrecording 2");
        mCamera = getCameraInstance();

        if (mCamera == null) {
            // TODO: tell user we cannot record video
            Log.i("VideoRecord", "camera is null :(");
            return;
        }
//        try {

            //mCamera.setPreviewDisplay(holder);
//            //mCamera.startPreview();
//        } catch (IOException e) {
//            Log.i("VideoRecord", "SHIT HAPPENED 0 !!!");
//            return;
//        }

        Log.i("VideoRecord", "startrecording 3");

        mCamera.unlock();
        mRecorder.setCamera(mCamera);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mRecorder.setMaxDuration(500000); // 50 seconds
        mRecorder.setMaxFileSize(5000000); // Approximately 5 megabytes
        mRecorder.setOutputFile(mRecordFile.getAbsolutePath());

        Log.i("VideoRecord", "startrecording 4");

        mRecorder.setPreviewDisplay(holder.getSurface());
        try {
            mRecorder.prepare();
        } catch (Exception e) {
            //e.printStackTrace();
            Log.i("VideoRecord", "SHIT HAPPENED!!!");
            return;
        }
        Log.i("VideoRecord", "startrecording 5");
        mRecorder.start();

        Toast.makeText(getApplicationContext(), "Video recording started", Toast.LENGTH_LONG)
             .show();

        Log.i("VideoRecord", "startrecording 6");
    }

    public static Camera getCameraInstance(){
        Camera camera = null;
        try {
            camera = Camera.open();
        }
        catch (Exception e){}
        return camera;
    }

    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        releaseCamera();
        mTimer.cancel();
        mTimerOn = false;
        addRecordingToMediaLibrary();
        Toast.makeText(getApplicationContext(), "Video recording is over", Toast.LENGTH_LONG)
             .show();
    }

    protected void addRecordingToMediaLibrary() {
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        values.put(MediaStore.Video.Media.TITLE, "video" + mRecordFile.getName());
        values.put(MediaStore.Video.Media.DATE_ADDED, (int) (current / 1000));
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, mRecordFile.getAbsolutePath());
        ContentResolver contentResolver = getContentResolver();

        Uri base = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
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
            Intent i = new Intent(VIDEO_RECORD_CANCEL);
            sendBroadcast(i);

            if (mCamera != null) {
                mCamera.lock();
                releaseCamera();
            }

            mWindowManager.removeView(mSurfaceView);
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

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }
}