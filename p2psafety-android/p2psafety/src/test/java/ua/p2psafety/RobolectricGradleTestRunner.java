package ua.p2psafety;

import org.junit.runners.model.InitializationError;
import org.robolectric.AndroidManifest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.res.Fs;
import org.robolectric.res.FsFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RobolectricGradleTestRunner extends RobolectricTestRunner {

    private static boolean alreadyRegisteredAbs = false;

    public RobolectricGradleTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected AndroidManifest createAppManifest(FsFile manifestFile, FsFile resDir,
                                                FsFile assetsDir) {
        return new MavenAndroidManifest(manifestFile, resDir, assetsDir);
    }

    @Override
    protected AndroidManifest getAppManifest(Config config) {
        String myAppPath = TestMyApplication.class.getProtectionDomain().getCodeSource()
                .getLocation().getPath();
        String manifestFilePath = myAppPath + "../../../../src/main/AndroidManifest.xml";
        String resFilePath = myAppPath + "../../../../res/";
        String assetsFilePath = myAppPath + "../../../../assets/";

        return createAppManifest(Fs.fileFromPath(manifestFilePath), Fs.fileFromPath(resFilePath),
                Fs.fileFromPath(assetsFilePath));
    }

    public static class MavenAndroidManifest extends AndroidManifest {
        public MavenAndroidManifest(FsFile manifestFile, FsFile resDir, FsFile assetsDir) {
            super(manifestFile, resDir, assetsDir);
        }

        public MavenAndroidManifest(FsFile baseDir) {
            super(baseDir);
        }

        @Override
        protected List<FsFile> findLibraries() {
            // Try unpack folder from maven.
            FsFile unpack = getBaseDir().join("build/exploded-bundles/");
            if (unpack.exists()) {
                FsFile[] libs = unpack.listFiles();
                if (libs != null) {
                    ArrayList<FsFile> dirs = new ArrayList<FsFile>();
                    for (FsFile file : libs) {
                        if (file.isDirectory())
                            dirs.add(file);
                    }
                    return dirs;
                }
            }
            return Collections.emptyList();
        }

        @Override
        protected AndroidManifest createLibraryAndroidManifest(FsFile libraryBaseDir) {
            return new MavenAndroidManifest(libraryBaseDir);
        }
    }

}
