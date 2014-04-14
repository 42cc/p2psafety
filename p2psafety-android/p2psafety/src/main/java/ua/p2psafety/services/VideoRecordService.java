package ua.p2psafety.services;

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
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

import ua.p2psafety.Notifications;
import ua.p2psafety.R;
import ua.p2psafety.util.NetworkManager;
import ua.p2psafety.data.Prefs;
import ua.p2psafety.util.Logs;
import ua.p2psafety.util.Utils;

public class VideoRecordService extends Service implements SurfaceHolder.Callback {
    private final int QUALITY_LOW = 0;
    private final int QUALITY_MEDIUM = 1;
    private final int QUALITY_HIGH = 2;
    private final int QUALITY_UNDETECTED = -1;

    private static Boolean mTimerOn = false;
    private static long mDuration;
    private static long mTimeLeft = 0;
    public static Logs LOGS;

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

        LOGS = new Logs(this);
    }

    public void onCreate(Context context) {
        super.onCreate();

        LOGS = new Logs(context);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGS.info("VideoRecordService.onStartCommand()");
        if (!mTimerOn) {
            // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
            LOGS.info("VideoRecordService. Creating new surface for video");
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
        } {
            LOGS.info("VideoRecordService. Record is already on");
        }
        return START_STICKY;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        LOGS.info("VideoRecordService.surfaceChanged() Starting record");
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
            LOGS.info("VideoRecordService. Record duration elapsed. Stoping record.");
            stopRecording(false);
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
        LOGS.info("VideoRecordService. Starting video record");
        try {
            mDuration = Prefs.getMediaRecordLength(getApplicationContext());
            LOGS.info("VideoRecordService. Duration: " + mDuration);

            LOGS.info("VideoRecordService. Preparing recorder");
            prepareRecorder();
            LOGS.info("VideoRecordService. Starting recorder");
            mRecorder.start();

            mTimeLeft = mDuration;
            mTimer = new VideoRecordTimer(mDuration, 1000);
            mTimer.start();
            mTimerOn = true;

            LOGS.info("VideoRecordService. Sending Notification");
            Notifications.notifVideoRecording(getApplicationContext(), mTimeLeft, mDuration);
        } catch (Exception e) {
            e.printStackTrace();

            LOGS.error("Can't start recording video", e);

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

            LOGS.error("Can't unlock camera", e);
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

            LOGS.error("Can't open camera");
        }
        return camera;
    }

    public void stopRecording(boolean isAlarmStop) {
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        releaseCamera();
        if (isAlarmStop)
            mWindowManager.removeView(mSurfaceView);

        mTimer.cancel();
        if (isAlarmStop)
            mTimerOn = false;

        if (Utils.isServerAuthenticated(this)) {
            LOGS.info("VideoRecordService. User authenticated at server. Upload record");
            NetworkManager.updateEventWithAttachment(this, mRecordFile, false);
        } else {
            LOGS.info("VideoRecordService. User is NOT authenticated at server. " +  "" +
                    "Send record by Email");
            Utils.sendMailsWithAttachments(this, R.string.video, mRecordFile);
        }

        LOGS.info("VideoRecordService. Changing Notifications");
        Notifications.removeNotification(getApplicationContext(), Notifications.NOTIF_VIDEO_RECORD_CODE);
        Notifications.notifVideoRecordingFinished(getApplicationContext());

        if (!isAlarmStop) {
            LOGS.info("VideoRecordService. SOS is still active. Start a new record.");
            startRecording();
        }
    }

    @Override
    public void onDestroy() {
        LOGS.info("VideoRecordService. Service shutdown");
        if (mTimerOn) {
            LOGS.info("VideoRecordService. Record is on. Stop it");
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

    private void releaseCamera(){
        if (mCamera != null){
            try {
                mCamera.unlock();
            } catch ( Exception e) {
                LOGS.error("Can't unlock camera", e);
            }
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
        if (Utils.isWiFiConnected(getApplicationContext(), LOGS))
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
            String msg = getString(R.string.media_record_error)
                    .replace("#media#", getString(R.string.video));
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
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