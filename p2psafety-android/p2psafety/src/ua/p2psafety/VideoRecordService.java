package ua.p2psafety;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

import ua.p2psafety.data.Prefs;
import ua.p2psafety.util.Utils;

public class VideoRecordService extends Service implements SurfaceHolder.Callback {
    private final int QUALITY_LOW = 0;
    private final int QUALITY_MEDIUM = 1;
    private final int QUALITY_HIGH = 2;
    private final int QUALITY_UNDETECTED = -1;

    private static Boolean mTimerOn = false;
    private static long mDuration;
    private static long mTimeLeft = 0;

    private static VideoRecordTimer mTimer = null;

    private static MediaRecorder mRecorder = null;
    private Camera mCamera = null;
    private WindowManager mWindowManager = null;
    private SurfaceView mSurfaceView = null;
    private int mQuality = QUALITY_UNDETECTED;

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
            mDuration = Prefs.getMediaRecordLength(getApplicationContext());

            prepareRecorder();
            mRecorder.start();

            mTimeLeft = mDuration;
            mTimer = new VideoRecordTimer(mDuration, 1000);
            mTimer.start();
            mTimerOn = true;

            Notifications.notifVideoRecording(getApplicationContext(), mTimeLeft, mDuration);
        } catch (Exception e) {
            e.printStackTrace();

            mRecorder.reset();
            mRecorder.release();

            tryLowerQuality();
        }
    }

    private void prepareRecorder() throws Exception {
        Log.i("prepareRecorder", "1=============================");
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
        Log.i("prepareRecorder", "2=============================");
        //mCamera.stopPreview();
        try {
            mCamera.unlock();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mRecorder = new MediaRecorder();
        mRecorder.setCamera(mCamera);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        if (mQuality == QUALITY_UNDETECTED)
            detectQuality();

        Log.i("prepareRecorder", "quality: " + mQuality);
        setupQuality();
        Log.i("prepareRecorder", "3=============================");

        mRecorder.setOutputFile(mRecordFile.getAbsolutePath());
        mRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
        mRecorder.prepare();
    }

    public Camera getCameraInstance(){
        if (mCamera != null)
            return mCamera;

        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;
    }

    public void stopRecording() {
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        releaseCamera();
        mWindowManager.removeView(mSurfaceView);

        mTimer.cancel();
        mTimerOn = false;

        Utils.sendMailsWithAttachments(this, R.string.video, mRecordFile);

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

    private void releaseCamera(){
        if (mCamera != null){
            try {
                mCamera.unlock();
            } catch ( Exception e) {}
            //mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setLowQuality() {
        mQuality = QUALITY_LOW;

        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        camcorderProfile.videoFrameWidth = 320;
        camcorderProfile.videoFrameHeight = 240;
        camcorderProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
        camcorderProfile.audioCodec = MediaRecorder.AudioEncoder.AAC;
        camcorderProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        mRecorder.setMaxFileSize(5*1024*1024); // 5 mb

        mRecorder.setProfile(camcorderProfile);
    }

    public void setMediumQuality() {
        mQuality = QUALITY_MEDIUM;

        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        camcorderProfile.videoFrameWidth = 640;
        camcorderProfile.videoFrameHeight = 480;
        camcorderProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
        camcorderProfile.audioCodec = MediaRecorder.AudioEncoder.AAC;
        camcorderProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        mRecorder.setMaxFileSize(10*1024*1024); // 10 mb

        mRecorder.setProfile(camcorderProfile);
    }

    public void setHighQuality() {
        mQuality = QUALITY_HIGH;

        mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));
        mRecorder.setMaxFileSize(20*1024*1024); // 20 mb
    }

    private void detectQuality() {
        int duration = (int) mDuration / 60000; // in minutes
        boolean hasWiFi = false;
        if (Utils.isWiFiConnected(getApplicationContext()))
            hasWiFi = true;

        Log.i("detectQuality", "hasWifi: " + true);
        Log.i("detectQuality", "duration: " + duration);
        // setup record quality
        if (duration < 2 && hasWiFi)
            mQuality = QUALITY_HIGH;
        else if (duration < 5 && hasWiFi)
            mQuality = QUALITY_MEDIUM;
        else
            mQuality = QUALITY_LOW;
    }

    private void setupQuality() {
        if (mQuality == QUALITY_LOW)
            setLowQuality();
        else if (mQuality == QUALITY_MEDIUM)
            setMediumQuality();
        else if (mQuality == QUALITY_HIGH)
            setHighQuality();
    }

    private void tryLowerQuality() {
        if (mQuality == QUALITY_LOW || mQuality == QUALITY_UNDETECTED) {
            releaseCamera();
            Toast.makeText(getApplicationContext(), "Can't start video recording", Toast.LENGTH_LONG)
                 .show();
            return;
        }
        // we have lower quality to try
        if (mQuality == QUALITY_HIGH)
            mQuality = QUALITY_MEDIUM;
        else if (mQuality == QUALITY_MEDIUM)
            mQuality = QUALITY_LOW;
        startRecording();
    }
}