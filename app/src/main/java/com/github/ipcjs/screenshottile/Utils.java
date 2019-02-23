package com.github.ipcjs.screenshottile;

import android.text.TextUtils;
import android.util.Log;

public class Utils {

    /**
     * Log.i()
     *
     * @param args Messages
     */
    public static void p(Object... args) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        p(TextUtils.join(", ", args));
    }

    /**
     * Log.i()
     *
     * @param msg Message
     */
    public static void p(String msg) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        Log.i("Screenshot", msg);
    }

    /**
     * Log.i()
     *
     * @param format Message with format placeholders
     * @param args   Format arguments
     */
    public static void p(String format, Object... args) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        p(String.format(format, args));
    }
}
