package ua.p2psafety;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

/**
 * Created by Taras Melon on 14.04.14.
 */
@Config(reportSdk = 11, qualifiers = "v11")
@RunWith(RobolectricGradleTestRunner.class)
public class SosActivityTest {

    @Test
    public void assertion()
    {
        assertTrue(true);
    }

}
