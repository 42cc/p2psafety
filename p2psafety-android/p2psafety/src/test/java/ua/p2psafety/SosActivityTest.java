package ua.p2psafety;

import android.content.Intent;
import android.support.v4.app.FragmentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Taras Melon on 14.04.14.
 */
@Config(reportSdk = 11, qualifiers = "v11")
@RunWith(RobolectricGradleTestRunner.class)
public class SosActivityTest {

    private SosActivity activity;
    private FragmentManager fragmentManager;

    @Before
    public void setup() {
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);

        Intent intent = new Intent();
        intent.putExtra("testing", true);
        activity = Robolectric.buildActivity(SosActivity.class).withIntent(intent).create()
                .postCreate(null).start().resume().visible().get();

        fragmentManager = activity.getSupportFragmentManager();

        //Utils.startAndWaitForThreadsStop();
    }

    @Test
    public void activityNotNull() {
        assertThat(activity).isNotNull();
        assertThat(fragmentManager).isNotNull();
    }

    @Test
    public void assertion() {
        assertTrue(true);
    }

}
