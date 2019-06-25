package org.zzy.lib.bettercamera.constant;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public final class PreviewConstant {
    public static final int SURFACE_VIEW    = 0;
    public static final int TEXTURE_VIEW    = 1;

    public static final int NONE                   = 0;
    public static final int WIDTH_FIRST            = 1;
    public static final int HEIGHT_FIRST           = 2;
    public static final int SMALLER_FIRST          = 3;
    public static final int LARGER_FIRST           = 4;

    @IntDef(value = {SURFACE_VIEW, TEXTURE_VIEW})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Type {
    }

    @IntDef({NONE, WIDTH_FIRST, HEIGHT_FIRST, SMALLER_FIRST, LARGER_FIRST})
    public @interface AdjustType {
    }
}
