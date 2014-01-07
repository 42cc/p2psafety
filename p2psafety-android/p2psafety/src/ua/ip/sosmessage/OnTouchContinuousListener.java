package ua.ip.sosmessage;
import android.view.MotionEvent;
import android.view.View;

// based on http://stackoverflow.com/a/19047697

public abstract class OnTouchContinuousListener implements View.OnTouchListener {
    private int mInitialRepeatDelay;
    private int mNormalRepeatDelay;
    private View mView;

    public OnTouchContinuousListener() {
        this.mInitialRepeatDelay = 500;
        this.mNormalRepeatDelay = 200;
    }

    private final Runnable repeatRunnable = new Runnable() {
        @Override
        public void run() {
            // as long the button is pressed we continue to repeat
            if (mView.isPressed()) {

                // Fire the onTouchRepeat event
                onTouchRepeat(mView);

                // Schedule the repeat
                mView.postDelayed(repeatRunnable, mNormalRepeatDelay);

                // speed up next repeat by 10%
                mNormalRepeatDelay -= mNormalRepeatDelay * 10 / 100;
            }
            else {
                mInitialRepeatDelay = 500;
                mNormalRepeatDelay = 200;
            }
        }
    };

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mView = v;

            // Fire the first touch straight away
            onTouchRepeat(mView);

            // Start the incrementing with the initial delay
            mView.postDelayed(repeatRunnable, mInitialRepeatDelay);
        }
        return false;
    }

    public abstract void onTouchRepeat(View view);

}
