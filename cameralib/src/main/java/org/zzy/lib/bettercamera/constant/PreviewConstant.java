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

    @IntDef(value = {SURFACE_VIEW, TEXTURE_VIEW})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Type {
    }
}
