package org.zzy.lib.bettercamera.utils;

import android.util.Log;

import org.zzy.lib.bettercamera.config.ConfigProvider;


/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public final class Logger {

    private static boolean isDebug;

    static {
        isDebug = ConfigProvider.get().isDebug();
    }

    private Logger() {
        throw new UnsupportedOperationException("U can't initialize me!");
    }

    public static void v(String tag, String msg) {
        if (isDebug) {
            Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (isDebug) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (isDebug) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (isDebug) {
            Log.e(tag, msg);
        }
    }

}
