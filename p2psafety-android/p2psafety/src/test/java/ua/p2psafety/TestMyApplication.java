package ua.p2psafety;

/**
 * Created by Taras on 14.04.14.
 */
public class TestMyApplication extends MyApplication {

    @Override
    public void onCreate() {
        mIsTesting = true;
        super.onCreate();
    }
}
