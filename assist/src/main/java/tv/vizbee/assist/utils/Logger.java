package tv.vizbee.assist.utils;

import android.util.Log;

/**
 * Generic logger class.
 * 1. When using this module in other modules/APKs use it as
 * <pre> Logger.setDebug(BuildConfig.DEBUG);</pre>
 * This will automatically change log level depending on type of release.
 * 2. At any time, the logLevel can also be changed with
 * <pre> Logger.setLogLevel(level);</pre>
 */
public class Logger {

    private static final String LOG_TAG = Logger.class.getSimpleName();

    public enum TYPE {
        VERBOSE(5),
        DEBUG(4),
        INFO(3),
        WARNING(2),
        ERROR(1),
        NONE(0);

        public int value;

        TYPE(int v) {
            value = v;
        }
    }

    private static TYPE mLogLevel = TYPE.WARNING;

    public static void v(String tag, String message) {
        log(tag, message, TYPE.VERBOSE);
    }

    public static void i(String tag, String message) {
        log(tag, message, TYPE.INFO);
    }

    public static void d(String tag, String message) {
        log(tag, message, TYPE.DEBUG);
    }

    public static void w(String tag, String message) {
        log(tag, message, TYPE.WARNING);
    }

    public static void e(String tag, String message) {
        log(tag, message, TYPE.ERROR);
    }

    public static void e(String tag, String message, Throwable t){
        log(tag, message, TYPE.ERROR, t);
    }

    public static void log(String tag, String msg, TYPE type) {
        log(tag, msg, type, null);
    }

    public static void log(String tag, String msg, TYPE type, Throwable t) {

        if (type.value <= mLogLevel.value) {
            switch (type) {
                case VERBOSE:
                    Log.v(tag, msg);
                    break;
                case INFO:
                    Log.i(tag, msg);
                    break;
                case DEBUG:
                    Log.d(tag, msg);
                    break;
                case WARNING:
                    Log.w(tag, msg);
                    break;
                case ERROR:
                    if (null != t) {
                        Log.e(tag, msg, t);
                    } else {
                        Log.e(tag, msg);
                    }
                    break;
            }
        }
    }

    public static void setLogLevel(TYPE t) {
        Log.v(LOG_TAG, "SetLogLevel =" + t);
        mLogLevel = t;
        return;
    }

    public static TYPE getLogLevel() {
        Log.v(LOG_TAG, "GetLogLevel =" + mLogLevel);
        return mLogLevel;
    }

    public static void setDebug(boolean debug) {

        Log.v(LOG_TAG, "SetDebug");

        if (debug) {
            mLogLevel = TYPE.VERBOSE;
        } else {
            mLogLevel = TYPE.NONE;
        }
    }
}

