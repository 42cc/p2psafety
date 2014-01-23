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
import java.io.IOException;

import ua.p2psafety.data.Prefs;

public class VideoRecordService extends Service implements SurfaceHolder.Callback {
    private static Boolean mTimerOn = false;
    private static long mDuration;
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
            // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
            mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
            mSurfaceView = new SurfaceView(this);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );
            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            mWindowManager.addView(mSurfaceView, layoutParams);
            mSurfaceView.getHolder().addCallback(this);
            mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        return START_STICKY;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        startRecording();
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
            mTimerOn = false;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mTimeLeft = millisUntilFinished;

            // update notification only once in a minute
            if (mTimeLeft / 1000 % 60 == 0)
                Notifications.notifVideoRecording(getApplicationContext(), mTimeLeft, mDuration);
        }
    }

    public void startRecording() {
        try {
            prepareRecorder();
            mRecorder.start();

            mDuration = Prefs.getMediaRecordLength(getApplicationContext());
            mTimeLeft = mDuration;
            mTimer = new VideoRecordTimer(mDuration, 1000);
            mTimer.start();
            mTimerOn = true;

            Notifications.notifVideoRecording(getApplicationContext(), mTimeLeft, mDuration);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Can't start video recording", Toast.LENGTH_LONG)
                 .show();
        }
    }

    private void prepareRecorder() throws Exception {
        File mediaDir;
        String state = Environment.getExternalStorageState();
        if(state.equals(Environment.MEDIA_MOUNTED))
            mediaDir = Environment.getExternalStorageDirectory();
        else
            mediaDir = getFilesDir();
        mRecordFile = File.createTempFile("video", ".mp4", mediaDir);

        mCamera = getCameraInstance();
        if (mCamera == null) {
            throw new Exception();
        }
        mCamera.unlock();

        mRecorder = new MediaRecorder();
        mRecorder.setCamera(mCamera);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        //mRecorder.setMaxDuration(500000); // 50 seconds
        //mRecorder.setMaxFileSize(5000000); // Approximately 5 megabytes
        mRecorder.setOutputFile(mRecordFile.getAbsolutePath());

        mRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
        mRecorder.prepare();
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
        mWindowManager.removeView(mSurfaceView);

        mTimer.cancel();
        mTimerOn = false;

        Notifications.removeNotification(getApplicationContext(), Notifications.NOTIF_VIDEO_RECORD_CODE);
        Notifications.notifVideoRecordingFinished(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        if (mTimerOn) {
            stopRecording();
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
    }

    public static long getAudioDuration(Context context) {
        if (mDuration == 0) {
            //mDuration = Prefs.getAudioDuration(context);
        }
        return mDuration;
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }
}