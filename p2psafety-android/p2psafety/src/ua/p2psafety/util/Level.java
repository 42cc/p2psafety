package ua.p2psafety.util;

/**
 * Created by Taras Melon on 17.02.14.
 */
public enum Level {
    FATAL(Level.FATAL_INT),
    ERROR(Level.ERROR_INT),
    WARN(Level.WARN_INT),
    INFO(Level.INFO_INT),
    DEBUG(Level.DEBUG_INT);

    private static final int FATAL_INT = 16;
    private static final int ERROR_INT = 8;
    private static final int WARN_INT = 4;
    private static final int INFO_INT = 2;
    private static final int DEBUG_INT = 1;

    private final int levelValue;

    private Level(final int levelValue) {
        this.levelValue = levelValue;
    }

    public int toInt() {
        return levelValue;
    }

    public String toString() {
        return this.name();
    }
}
